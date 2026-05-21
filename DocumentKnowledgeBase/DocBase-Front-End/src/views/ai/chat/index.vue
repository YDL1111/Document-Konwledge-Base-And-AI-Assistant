<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { ChatDotRound, Delete, MagicStick, Plus, Promotion } from "@element-plus/icons-vue";
import {
  deleteAiChatSessionApi,
  getAiChatMessagesApi,
  getAiChatSessionsApi,
  getAiChatStreamUrl
} from "@/api/ai/chat";
import type {
  AiChatMessageDTO,
  AiChatQueryRequest,
  AiChatSessionDTO,
  SourceInfo
} from "@/api/ai/types";
import { formatToken, getToken } from "@/utils/auth";

defineOptions({
  name: "AiChat"
});

const SESSION_STORAGE_KEY = "ai_chat_session_id";

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: SourceInfo[];
  isError?: boolean;
  expanded?: boolean;
  streaming?: boolean;
}

interface StreamEvent {
  type: string;
  data: unknown;
}

const quickPrompts = [
  "帮我总结当前知识库最重要的三部分内容",
  "基于文档内容给我一份面试问答清单",
  "把复杂概念用适合新人的方式解释一遍"
];

const question = ref("");
const loading = ref(false);
const loadingHistory = ref(false);
const loadingSessions = ref(false);
const deletingSessionId = ref<number>();
const sessions = ref<AiChatSessionDTO[]>([]);
const messages = ref<ChatMessage[]>([]);
const currentSessionId = ref<number>();
const pageRootRef = ref<HTMLElement>();
const messagesContainer = ref<HTMLElement>();
const textareaRef = ref<HTMLTextAreaElement>();
const pageHeight = ref("calc(100vh - 96px)");

let currentAbortController: AbortController | null = null;
let streamSessionListSynced = false;

const currentSessionIdStr = computed(() =>
  currentSessionId.value != null ? String(currentSessionId.value) : ""
);

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const formatInlineMarkdown = (text: string) =>
  text
    .replace(/`([^`\n]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*\n]+)\*/g, "<em>$1</em>")
    .replace(
      /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>'
    );

const renderMarkdown = (content: string) => {
  if (!content) return "";

  const normalized = content.replace(/\r\n/g, "\n");
  const codeBlocks: string[] = [];
  let working = normalized.replace(/```([\w-]*)\n([\s\S]*?)```/g, (_, lang = "", code = "") => {
    const index = codeBlocks.length;
    const language = lang ? `<div class="md-code__lang">${escapeHtml(lang)}</div>` : "";
    codeBlocks.push(
      `<pre class="md-code">${language}<code>${escapeHtml(code.trimEnd())}</code></pre>`
    );
    return `@@CODE_BLOCK_${index}@@`;
  });

  const lines = working.split("\n");
  const html: string[] = [];
  let inList = false;
  let listType: "ul" | "ol" | null = null;
  let inQuote = false;

  const closeList = () => {
    if (inList && listType) {
      html.push(`</${listType}>`);
      inList = false;
      listType = null;
    }
  };

  const closeQuote = () => {
    if (inQuote) {
      html.push("</blockquote>");
      inQuote = false;
    }
  };

  lines.forEach(rawLine => {
    const line = rawLine.trimEnd();
    const trimmed = line.trim();

    if (!trimmed) {
      closeList();
      closeQuote();
      return;
    }

    if (trimmed.startsWith("@@CODE_BLOCK_")) {
      closeList();
      closeQuote();
      html.push(trimmed);
      return;
    }

    if (trimmed.startsWith(">")) {
      closeList();
      if (!inQuote) {
        html.push("<blockquote>");
        inQuote = true;
      }
      html.push(`<p>${formatInlineMarkdown(escapeHtml(trimmed.replace(/^>\s?/, "")))}</p>`);
      return;
    }

    closeQuote();

    const orderedMatch = trimmed.match(/^\d+\.\s+(.*)$/);
    const unorderedMatch = trimmed.match(/^[-*]\s+(.*)$/);

    if (orderedMatch) {
      if (!inList || listType !== "ol") {
        closeList();
        html.push("<ol>");
        inList = true;
        listType = "ol";
      }
      html.push(`<li>${formatInlineMarkdown(escapeHtml(orderedMatch[1]))}</li>`);
      return;
    }

    if (unorderedMatch) {
      if (!inList || listType !== "ul") {
        closeList();
        html.push("<ul>");
        inList = true;
        listType = "ul";
      }
      html.push(`<li>${formatInlineMarkdown(escapeHtml(unorderedMatch[1]))}</li>`);
      return;
    }

    closeList();

    if (trimmed.startsWith("### ")) {
      html.push(`<h3>${formatInlineMarkdown(escapeHtml(trimmed.slice(4)))}</h3>`);
      return;
    }
    if (trimmed.startsWith("## ")) {
      html.push(`<h2>${formatInlineMarkdown(escapeHtml(trimmed.slice(3)))}</h2>`);
      return;
    }
    if (trimmed.startsWith("# ")) {
      html.push(`<h1>${formatInlineMarkdown(escapeHtml(trimmed.slice(2)))}</h1>`);
      return;
    }

    html.push(`<p>${formatInlineMarkdown(escapeHtml(trimmed))}</p>`);
  });

  closeList();
  closeQuote();

  let result = html.join("");
  codeBlocks.forEach((block, index) => {
    result = result.replace(`@@CODE_BLOCK_${index}@@`, block);
  });
  return result;
};

const scrollToBottom = async (smooth = false) => {
  await nextTick();
  if (!messagesContainer.value) return;
  messagesContainer.value.scrollTo({
    top: messagesContainer.value.scrollHeight,
    behavior: smooth ? "smooth" : "auto"
  });
};

const syncViewportLayout = () => {
  nextTick(() => {
    const root = pageRootRef.value;
    if (!root) return;
    const rect = root.getBoundingClientRect();
    const availableHeight = window.innerHeight - rect.top - 24;
    if (availableHeight > 360) {
      pageHeight.value = `${availableHeight}px`;
    }
  });
};

const resizeTextarea = () => {
  nextTick(() => {
    const textarea = textareaRef.value;
    if (!textarea) return;
    textarea.style.height = "auto";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  });
};

const fillPrompt = (text: string) => {
  question.value = text;
  resizeTextarea();
  nextTick(() => textareaRef.value?.focus());
};

const formatSourceTitle = (source: SourceInfo) =>
  source.page ? `${source.filename} · 第 ${source.page} 页` : source.filename;

const formatScore = (score: number) => `${((score || 0) * 100).toFixed(0)}%`;

const normalizeHistoryMessage = (dto: AiChatMessageDTO): ChatMessage => ({
  id: String(dto.messageId),
  role: dto.messageRole === 1 ? "user" : "assistant",
  content: dto.messageContent || "",
  sources: dto.sources ?? undefined,
  isError: dto.messageRole === 2 && !dto.messageContent,
  expanded: false,
  streaming: false
});

const abortStreaming = () => {
  if (loading.value) {
    currentAbortController?.abort();
  }
};

const loadSessionList = async () => {
  loadingSessions.value = true;
  try {
    const res = await getAiChatSessionsApi({ pageNum: 1, pageSize: 50 });
    if (res.code === 0 && res.data) {
      sessions.value = res.data.rows ?? [];
    } else {
      ElMessage.error(res.msg || "加载会话列表失败");
    }
  } catch {
    ElMessage.error("加载会话列表失败");
  } finally {
    loadingSessions.value = false;
  }
};

const resolveNewSessionId = (
  previousSessionIds: number[],
  questionText: string
) => {
  const previousIdSet = new Set(previousSessionIds);
  const created = sessions.value.find(item => !previousIdSet.has(item.sessionId));
  if (created) return created.sessionId;

  const fallbackTitle = questionText.length > 20 ? `${questionText.slice(0, 20)}...` : questionText;
  return sessions.value.find(item => item.sessionTitle === fallbackTitle)?.sessionId;
};

const loadHistory = async (sessionId: number) => {
  loadingHistory.value = true;
  try {
    const res = await getAiChatMessagesApi(sessionId);
    if (res.code === 0 && res.data) {
      applyHistoryMessages(sessionId, res.data);
    } else {
      throw new Error(res.msg || "加载历史消息失败");
    }
  } catch {
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = undefined;
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      messages.value = [];
    }
    ElMessage.error("加载历史消息失败，可继续发送新消息");
  } finally {
    loadingHistory.value = false;
    await scrollToBottom();
  }
};

const syncHistoryAfterStream = async (sessionId: number, expectedAnswer: string) => {
  const normalizedExpected = expectedAnswer.trim();

  for (let attempt = 0; attempt < 6; attempt += 1) {
    try {
      const res = await getAiChatMessagesApi(sessionId);
      if (res.code === 0 && res.data) {
        const lastAssistant = [...res.data]
          .reverse()
          .find(item => item.messageRole === 2);
        const answer = lastAssistant?.messageContent?.trim() ?? "";

        if (!normalizedExpected) {
          if (answer) {
            applyHistoryMessages(sessionId, res.data);
            return;
          }
        } else if (answer === normalizedExpected || answer.length >= normalizedExpected.length) {
          applyHistoryMessages(sessionId, res.data);
          return;
        }
      }
    } catch {
      break;
    }

    await new Promise(resolve => window.setTimeout(resolve, 250));
  }

  try {
    await loadHistory(sessionId);
  } catch {
    // Keep the in-memory stream result if history sync still fails.
  }
};

const onSessionListReady = async () => {
  const saved = sessionStorage.getItem(SESSION_STORAGE_KEY);
  if (!saved) return;

  const sessionId = Number(saved);
  if (Number.isNaN(sessionId)) {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    return;
  }

  if (sessions.value.some(item => item.sessionId === sessionId)) {
    await loadHistory(sessionId);
  } else {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
  }
};

const selectSession = async (sessionId: number) => {
  if (currentSessionId.value === sessionId) return;
  abortStreaming();
  await loadHistory(sessionId);
};

const newSession = () => {
  abortStreaming();
  currentSessionId.value = undefined;
  sessionStorage.removeItem(SESSION_STORAGE_KEY);
  messages.value = [];
};

const deleteSession = async (session: AiChatSessionDTO) => {
  deletingSessionId.value = session.sessionId;
  try {
    const res = await deleteAiChatSessionApi(session.sessionId);
    if (res.code !== 0) {
      throw new Error(res.msg || "删除会话失败");
    }

    if (currentSessionId.value === session.sessionId) {
      abortStreaming();
      currentSessionId.value = undefined;
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      messages.value = [];
    }

    sessions.value = sessions.value.filter(item => item.sessionId !== session.sessionId);
    ElMessage.success("会话已删除");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "删除会话失败");
  } finally {
    deletingSessionId.value = undefined;
  }
};

const createStreamingAssistantMessage = () => {
  const aiMsg: ChatMessage = {
    id: `assistant-${Date.now()}`,
    role: "assistant",
    content: "",
    sources: [],
    expanded: false,
    streaming: true
  };
  messages.value.push(aiMsg);
  return messages.value[messages.value.length - 1]!;
};

const parseStreamEvent = (payload: string): StreamEvent | null => {
  try {
    return JSON.parse(payload) as StreamEvent;
  } catch {
    return null;
  }
};

const applyHistoryMessages = (sessionId: number, history: AiChatMessageDTO[]) => {
  messages.value = history.map(normalizeHistoryMessage);
  currentSessionId.value = sessionId;
  sessionStorage.setItem(SESSION_STORAGE_KEY, String(sessionId));
};

const extractSseBlocks = (buffer: string) => {
  const normalized = buffer.replace(/\r\n/g, "\n");
  const blocks = normalized.split("\n\n");
  return {
    completeBlocks: blocks.slice(0, -1),
    remainder: blocks[blocks.length - 1] ?? ""
  };
};

const parseSseBlock = (block: string): StreamEvent | null => {
  const dataLines = block
    .split("\n")
    .map(line => line.trim())
    .filter(line => line.startsWith("data:"))
    .map(line => line.slice(5).trimStart());

  if (!dataLines.length) return null;

  return parseStreamEvent(dataLines.join("\n"));
};

const handleStreamEvent = async (event: StreamEvent, aiMsg: ChatMessage, isNewSession: boolean) => {
  switch (event.type) {
    case "start":
      break;
    case "conv_id":
      if (typeof event.data === "number") {
        currentSessionId.value = event.data;
        sessionStorage.setItem(SESSION_STORAGE_KEY, String(event.data));
        if (isNewSession && !streamSessionListSynced) {
          streamSessionListSynced = true;
          await loadSessionList();
        }
      }
      break;
    case "python_conv_id":
      break;
    case "token":
      aiMsg.content += String(event.data ?? "");
      await scrollToBottom();
      break;
    case "sources":
      aiMsg.sources = (event.data as SourceInfo[]) ?? [];
      break;
    case "done": {
      const data = event.data as { answer?: string; sources?: SourceInfo[] } | string | null;
      if (typeof data === "string") {
        aiMsg.content = data;
      } else {
        if (data?.answer != null) {
          aiMsg.content = data.answer;
        }
        if (data?.sources) {
          aiMsg.sources = data.sources;
        }
      }
      aiMsg.streaming = false;
      await scrollToBottom();
      break;
    }
    case "error": {
      const message = String(event.data ?? "AI 服务暂时不可用，请稍后重试");
      aiMsg.isError = true;
      aiMsg.streaming = false;
      aiMsg.content = message;
      await scrollToBottom();
      throw new Error(message);
    }
    default:
      break;
  }
};

const streamAnswer = async (payload: AiChatQueryRequest, aiMsg: ChatMessage, isNewSession: boolean) => {
  const token = getToken();
  currentAbortController = new AbortController();

  const response = await fetch(`${import.meta.env.VITE_APP_BASE_API}${getAiChatStreamUrl()}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      Authorization: formatToken(token.token)
    },
    body: JSON.stringify(payload),
    signal: currentAbortController.signal
  });

  if (!response.ok || !response.body) {
    throw new Error(`请求失败: HTTP ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  const processBlock = async (block: string) => {
    const event = parseSseBlock(block);
    if (!event) return;
    await handleStreamEvent(event, aiMsg, isNewSession);
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const { completeBlocks, remainder } = extractSseBlocks(buffer);
    buffer = remainder;

    for (const block of completeBlocks) {
      await processBlock(block);
    }
  }

  buffer += decoder.decode();
  const { completeBlocks, remainder } = extractSseBlocks(`${buffer}\n\n`);
  for (const block of completeBlocks) {
    await processBlock(block);
  }
  if (remainder.trim()) {
    await processBlock(remainder);
  }
};

const send = async () => {
  const text = question.value.trim();
  if (!text || loading.value) return;

  streamSessionListSynced = false;
  const isNewSession = currentSessionId.value == null;
  const previousSessionIds = isNewSession ? sessions.value.map(item => item.sessionId) : [];
  loading.value = true;

  messages.value.push({
    id: `user-${Date.now()}`,
    role: "user",
    content: text,
    expanded: false
  });
  question.value = "";
  resizeTextarea();
  await scrollToBottom();

  const aiMsg = createStreamingAssistantMessage();

  try {
    await streamAnswer(
      {
        sessionId: currentSessionId.value,
        question: text
      },
      aiMsg,
      isNewSession
    );
    if (isNewSession && currentSessionId.value == null) {
      await loadSessionList();
      const resolvedSessionId = resolveNewSessionId(previousSessionIds, text);
      if (resolvedSessionId != null) {
        currentSessionId.value = resolvedSessionId;
        sessionStorage.setItem(SESSION_STORAGE_KEY, String(resolvedSessionId));
      }
    }
    if (currentSessionId.value != null) {
      await syncHistoryAfterStream(currentSessionId.value, aiMsg.content);
    }

  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      return;
    }

    aiMsg.isError = true;
    aiMsg.streaming = false;
    if (!aiMsg.content) {
      aiMsg.content = error instanceof Error ? error.message : "AI 服务暂时不可用，请稍后重试";
    }
    ElMessage.error(aiMsg.content);
  } finally {
    loading.value = false;
    currentAbortController = null;
    await scrollToBottom();
  }
};

const toggleSources = (msg: ChatMessage) => {
  msg.expanded = !msg.expanded;
};

const handleEnter = (event: KeyboardEvent) => {
  if (event.shiftKey) return;
  event.preventDefault();
  send();
};

onMounted(async () => {
  await loadSessionList();
  await onSessionListReady();
  resizeTextarea();
  syncViewportLayout();
  window.addEventListener("resize", syncViewportLayout);
  setTimeout(syncViewportLayout, 0);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", syncViewportLayout);
  currentAbortController?.abort();
});
</script>

<template>
  <div ref="pageRootRef" class="ai-chat-page" :style="{ height: pageHeight }">
    <aside class="session-sidebar">
      <div class="sidebar-top">
        <div class="sidebar-heading">
          <span class="sidebar-kicker">Workspace</span>
          <h2>AI 对话</h2>
        </div>
        <el-button type="primary" class="new-session-btn" :icon="Plus" :disabled="loading" @click="newSession">
          新会话
        </el-button>
      </div>

      <div class="sidebar-overview">
        <div class="overview-card">
          <span class="overview-label">会话总数</span>
          <strong>{{ sessions.length }}</strong>
        </div>
        <div class="overview-card">
          <span class="overview-label">回答模式</span>
          <strong>流式输出</strong>
        </div>
      </div>

      <div class="session-list" v-loading="loadingSessions">
        <div v-if="sessions.length === 0 && !loadingSessions" class="session-empty">
          <div class="session-empty__badge">01</div>
          <p>还没有历史会话</p>
          <span>发送第一个问题后，这里会自动生成对话记录。</span>
        </div>

        <button
          v-for="session in sessions"
          :key="String(session.sessionId)"
          class="session-item"
          :class="{ 'session-item--active': currentSessionIdStr === String(session.sessionId) }"
          @click="selectSession(session.sessionId)"
        >
          <span class="session-item__main">
            <span class="session-item__icon">
              <el-icon><ChatDotRound /></el-icon>
            </span>
            <span class="session-title">{{ session.sessionTitle }}</span>
          </span>

          <el-popconfirm
            width="220"
            title="确认删除这个历史会话吗？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="deleteSession(session)"
          >
            <template #reference>
              <el-button
                class="session-delete-btn"
                text
                circle
                :icon="Delete"
                :loading="deletingSessionId === session.sessionId"
                @click.stop
              />
            </template>
          </el-popconfirm>
        </button>
      </div>
    </aside>

    <section class="chat-main">
      <header class="chat-header">
        <div>
          <p class="chat-header__kicker">Knowledge-grounded assistant</p>
          <h1>AI 问答工作台</h1>
          <p class="chat-header__hint">
            支持流式输出、Markdown 渲染与来源回显，回答会像真实聊天一样逐步出现。
          </p>
        </div>
        <div class="chat-header__status">
          <span class="chat-header__dot"></span>
          上下文已连接
        </div>
      </header>

      <div ref="messagesContainer" class="chat-messages">
        <div v-if="loadingHistory" class="chat-empty">
          <p class="empty-title">正在加载历史消息...</p>
        </div>

        <div v-else-if="messages.length === 0" class="chat-welcome">
          <div class="chat-welcome__halo"></div>
          <div class="chat-welcome__panel">
            <div class="chat-welcome__icon">
              <el-icon><MagicStick /></el-icon>
            </div>
            <p class="chat-welcome__label">RAG Assistant</p>
            <h3>让回答更像成熟的聊天产品</h3>
            <p>支持逐字输出、Markdown 段落、列表、代码块和引用来源展示，适合文档问答与知识检索场景。</p>
            <div class="prompt-grid">
              <button v-for="item in quickPrompts" :key="item" class="prompt-chip" @click="fillPrompt(item)">
                {{ item }}
              </button>
            </div>
          </div>
        </div>

        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="msg.role === 'user' ? 'message-row--user' : 'message-row--ai'"
        >
          <div v-if="msg.role === 'assistant'" class="assistant-avatar">AI</div>

          <div class="message-stack">
            <div
              class="message-bubble"
              :class="{
                'message-bubble--user': msg.role === 'user',
                'message-bubble--error': msg.isError
              }"
            >
              <div class="message-meta">
                {{ msg.role === "user" ? "你" : "知识库助手" }}
              </div>

              <div v-if="msg.isError" class="message-content">
                <span class="error-icon">!</span>
                {{ msg.content || "消息获取失败" }}
              </div>

              <div
                v-else-if="msg.role === 'assistant'"
                class="message-markdown"
                v-html="renderMarkdown(msg.content)"
              ></div>

              <div v-else class="message-content">
                {{ msg.content }}
              </div>

              <div v-if="msg.streaming" class="stream-caret"></div>
            </div>

            <template v-if="msg.role === 'assistant' && !msg.isError && msg.sources && msg.sources.length">
              <div class="source-panel">
                <button class="source-toggle" @click="toggleSources(msg)">
                  <span>{{ msg.expanded ? "收起引用来源" : "查看引用来源" }}</span>
                  <em>{{ msg.sources.length }}</em>
                </button>

                <div v-if="msg.expanded" class="sources-list">
                  <div v-for="(src, idx) in msg.sources" :key="idx" class="source-card">
                    <div class="source-card__index">{{ idx + 1 }}</div>
                    <div class="source-card__body">
                      <div class="source-card__header">
                        <span class="source-filename">{{ formatSourceTitle(src) }}</span>
                        <span class="source-score">{{ formatScore(src.score) }}</span>
                      </div>
                      <p class="source-content">{{ src.content }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <footer class="chat-composer">
        <div class="chat-composer__frame">
          <div class="chat-composer__toolbar">
            <span class="composer-tag">Stream response</span>
            <span class="composer-tip">Enter 发送，Shift + Enter 换行</span>
          </div>

          <div class="chat-composer__body">
            <textarea
              ref="textareaRef"
              v-model="question"
              class="chat-textarea"
              placeholder="输入你的问题，让 AI 基于知识库逐步生成回答。"
              :disabled="loading"
              rows="1"
              @input="resizeTextarea"
              @keydown.enter="handleEnter"
            />
            <el-button
              type="primary"
              class="send-btn"
              :icon="Promotion"
              :loading="loading"
              :disabled="!question.trim()"
              @click="send"
            >
              {{ loading ? "生成中" : "发送" }}
            </el-button>
          </div>
        </div>
        <p class="composer-footnote">回答基于知识库内容生成，仅供参考，重要信息请结合原始文档核验。</p>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.ai-chat-page {
  --bg-panel: rgba(255, 252, 247, 0.78);
  --bg-panel-strong: rgba(255, 250, 243, 0.96);
  --line-soft: rgba(81, 58, 42, 0.12);
  --text-main: #2d2219;
  --text-sub: #7c6959;
  --accent: #bf5a36;
  --accent-strong: #8f3f24;
  --accent-soft: rgba(191, 90, 54, 0.12);
  --shadow-soft: 0 24px 80px rgba(73, 45, 24, 0.08);
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  padding: 20px;
  margin: -24px;
  box-sizing: border-box;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(230, 181, 128, 0.18), transparent 28%),
    radial-gradient(circle at right 10%, rgba(197, 99, 59, 0.1), transparent 26%),
    linear-gradient(180deg, #fbf7f0 0%, #f1ebe3 100%);
}

.session-sidebar,
.chat-main {
  min-height: 0;
  height: 100%;
}

.session-sidebar {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--line-soft);
  border-radius: 30px;
  background: var(--bg-panel);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(20px);
  overflow: hidden;
}

.sidebar-top {
  padding: 24px 22px 18px;
  border-bottom: 1px solid var(--line-soft);
}

.sidebar-kicker,
.chat-header__kicker,
.chat-welcome__label {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-sub);
}

.sidebar-heading h2,
.chat-header h1,
.chat-welcome h3 {
  margin: 0;
  color: var(--text-main);
  font-family: "Georgia", "Times New Roman", serif;
  font-weight: 600;
}

.sidebar-heading h2 {
  font-size: 28px;
}

.new-session-btn {
  width: 100%;
  height: 44px;
  margin-top: 18px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  box-shadow: 0 14px 30px rgba(191, 90, 54, 0.24);
}

.sidebar-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 16px 18px;
}

.overview-card {
  padding: 14px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.5);
}

.overview-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-sub);
}

.overview-card strong {
  color: var(--text-main);
  font-size: 14px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 12px 16px;
  min-height: 0;
}

.session-empty {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 24px 18px;
  text-align: center;
  color: var(--text-sub);
}

.session-empty__badge {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 14px;
  font-weight: 700;
}

.session-empty p {
  margin: 0;
  color: var(--text-main);
  font-weight: 600;
}

.session-empty span {
  font-size: 12px;
  line-height: 1.7;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  margin-bottom: 8px;
  padding: 12px 12px 12px 10px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
  transition: 0.2s ease;
  text-align: left;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.62);
  border-color: rgba(191, 90, 54, 0.1);
}

.session-item--active {
  background: rgba(191, 90, 54, 0.08);
  border-color: rgba(191, 90, 54, 0.18);
  box-shadow: inset 3px 0 0 var(--accent);
}

.session-item__main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.session-item__icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--accent-strong);
  flex-shrink: 0;
}

.session-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 500;
}

.session-delete-btn {
  color: #a96a58;
  flex-shrink: 0;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid var(--line-soft);
  border-radius: 30px;
  background: var(--bg-panel-strong);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(20px);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 30px 20px;
  border-bottom: 1px solid var(--line-soft);
}

.chat-header h1 {
  font-size: 38px;
  line-height: 1.05;
}

.chat-header__hint {
  margin: 12px 0 0;
  max-width: 700px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.8;
}

.chat-header__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text-main);
  font-size: 12px;
  white-space: nowrap;
}

.chat-header__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #28b463;
  box-shadow: 0 0 0 4px rgba(40, 180, 99, 0.14);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px 30px;
  min-height: 0;
}

.chat-empty,
.chat-welcome {
  min-height: 100%;
  display: grid;
  place-items: center;
}

.empty-title {
  margin: 0;
  color: var(--text-sub);
}

.chat-welcome {
  position: relative;
}

.chat-welcome__halo {
  position: absolute;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(191, 90, 54, 0.12), transparent 68%);
  filter: blur(4px);
}

.chat-welcome__panel {
  position: relative;
  width: min(680px, 100%);
  padding: 34px 32px;
  border: 1px solid rgba(191, 90, 54, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 18px 60px rgba(74, 45, 24, 0.08);
  text-align: center;
}

.chat-welcome__icon {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(191, 90, 54, 0.12), rgba(143, 63, 36, 0.2));
  color: var(--accent-strong);
  font-size: 28px;
}

.chat-welcome__panel p {
  margin: 10px auto 0;
  max-width: 560px;
  color: var(--text-sub);
  line-height: 1.8;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.prompt-chip {
  padding: 14px 14px;
  border: 1px solid rgba(191, 90, 54, 0.14);
  border-radius: 16px;
  background: rgba(255, 251, 247, 0.9);
  color: var(--text-main);
  line-height: 1.6;
  cursor: pointer;
  transition: 0.2s ease;
}

.prompt-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(191, 90, 54, 0.28);
  box-shadow: 0 12px 22px rgba(75, 46, 25, 0.08);
}

.message-row {
  display: flex;
  gap: 14px;
  margin-bottom: 22px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-stack {
  max-width: min(860px, 88%);
}

.assistant-avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.message-bubble {
  position: relative;
  padding: 14px 16px 16px;
  border: 1px solid rgba(81, 58, 42, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 30px rgba(75, 46, 25, 0.04);
}

.message-bubble--user {
  background: linear-gradient(135deg, #d66a42, #a94928);
  border-color: transparent;
  color: #fff;
}

.message-bubble--error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}

.message-meta {
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.74;
}

.message-content {
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-markdown {
  color: inherit;
  font-size: 14px;
  line-height: 1.85;
  word-break: break-word;
}

.message-markdown :deep(p),
.message-markdown :deep(ul),
.message-markdown :deep(ol),
.message-markdown :deep(blockquote),
.message-markdown :deep(pre) {
  margin: 0 0 10px;
}

.message-markdown :deep(p:last-child),
.message-markdown :deep(ul:last-child),
.message-markdown :deep(ol:last-child),
.message-markdown :deep(blockquote:last-child),
.message-markdown :deep(pre:last-child) {
  margin-bottom: 0;
}

.message-markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(52, 36, 24, 0.08);
  font-size: 13px;
}

.message-markdown :deep(a) {
  color: inherit;
  text-decoration: underline;
}

.message-markdown :deep(blockquote) {
  padding-left: 12px;
  border-left: 3px solid rgba(191, 90, 54, 0.3);
  color: var(--text-sub);
}

.message-markdown :deep(.md-code) {
  overflow-x: auto;
  padding: 12px;
  border-radius: 14px;
  background: #1f1a17;
  color: #f8ede5;
}

.message-markdown :deep(.md-code code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.message-markdown :deep(.md-code__lang) {
  margin-bottom: 8px;
  color: #d5b5a1;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.error-icon {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  margin-right: 8px;
  border-radius: 50%;
  background: #dc2626;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  vertical-align: middle;
}

.stream-caret {
  width: 10px;
  height: 18px;
  margin-top: 8px;
  border-radius: 999px;
  background: currentColor;
  animation: blink-caret 1s steps(2, jump-none) infinite;
  opacity: 0.6;
}

.source-panel {
  margin-top: 10px;
  border: 1px solid rgba(81, 58, 42, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.source-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 14px;
  border: none;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
}

.source-toggle em {
  font-style: normal;
  font-weight: 700;
  color: var(--accent-strong);
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 12px 12px;
}

.source-card {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(81, 58, 42, 0.08);
  border-radius: 16px;
  background: rgba(255, 251, 247, 0.92);
}

.source-card__index {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: rgba(191, 90, 54, 0.12);
  color: var(--accent-strong);
  font-size: 13px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
  flex-shrink: 0;
}

.source-card__body {
  min-width: 0;
  flex: 1;
}

.source-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.source-filename {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
}

.source-score {
  color: var(--text-sub);
  font-size: 12px;
  white-space: nowrap;
}

.source-content {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

.chat-composer {
  padding: 18px 26px 22px;
  border-top: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.6), rgba(255, 249, 242, 0.96));
}

.chat-composer__frame {
  padding: 12px;
  border: 1px solid rgba(191, 90, 54, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 40px rgba(75, 46, 25, 0.05);
}

.chat-composer__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 6px 10px;
}

.composer-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 600;
}

.composer-tip,
.composer-footnote {
  color: var(--text-sub);
  font-size: 12px;
}

.chat-composer__body {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.chat-textarea {
  flex: 1;
  min-height: 56px;
  max-height: 180px;
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: #fffdf9;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.7;
  resize: none;
  outline: none;
  transition: 0.2s ease;
}

.chat-textarea:focus {
  border-color: rgba(191, 90, 54, 0.22);
  box-shadow: inset 0 0 0 1px rgba(191, 90, 54, 0.08);
}

.send-btn {
  height: 56px;
  padding: 0 22px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  box-shadow: 0 18px 34px rgba(191, 90, 54, 0.24);
}

.composer-footnote {
  margin: 10px 4px 0;
}

@keyframes blink-caret {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@media (max-width: 1200px) {
  .ai-chat-page {
    grid-template-columns: 260px minmax(0, 1fr);
    padding: 16px;
    gap: 16px;
  }

  .chat-header,
  .chat-messages {
    padding-left: 24px;
    padding-right: 24px;
  }
}

@media (max-width: 960px) {
  .ai-chat-page {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(220px, 32%) minmax(0, 1fr);
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }

  .message-stack {
    max-width: 100%;
  }

  .chat-header {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .ai-chat-page {
    gap: 14px;
    padding: 12px;
  }

  .sidebar-top,
  .chat-header,
  .chat-messages,
  .chat-composer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .chat-header h1 {
    font-size: 30px;
  }

  .chat-welcome__panel {
    padding: 24px 20px;
  }

  .chat-composer__body {
    flex-direction: column;
    align-items: stretch;
  }

  .send-btn {
    width: 100%;
  }
}
</style>
