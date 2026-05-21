<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { ChatDotRound, Plus, Promotion, MagicStick } from "@element-plus/icons-vue";
import {
  getAiChatMessagesApi,
  getAiChatSessionsApi,
  getAiChatStreamUrl
} from "@/api/ai/chat";
import type {
  AiChatAnswerDTO,
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
  "把复杂概念用适合新人理解的方式解释一下"
];

const question = ref("");
const loading = ref(false);
const loadingHistory = ref(false);
const loadingSessions = ref(false);
const messages = ref<ChatMessage[]>([]);
const sessions = ref<AiChatSessionDTO[]>([]);
const currentSessionId = ref<number>();
const pageRootRef = ref<HTMLElement>();
const messagesContainer = ref<HTMLElement>();
const textareaRef = ref<HTMLTextAreaElement>();
const pageHeight = ref("calc(100vh - 96px)");
let currentAbortController: AbortController | null = null;

const currentSessionIdStr = computed(() =>
  currentSessionId.value != null ? String(currentSessionId.value) : null
);

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const formatInlineMarkdown = (text: string) => {
  return text
    .replace(/`([^`\n]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*\n]+)\*/g, "<em>$1</em>")
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
};

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
  let inBlockquote = false;

  const closeList = () => {
    if (inList && listType) {
      html.push(`</${listType}>`);
      inList = false;
      listType = null;
    }
  };

  const closeQuote = () => {
    if (inBlockquote) {
      html.push("</blockquote>");
      inBlockquote = false;
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
      if (!inBlockquote) {
        html.push("<blockquote>");
        inBlockquote = true;
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
  if (messagesContainer.value) {
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: smooth ? "smooth" : "auto"
    });
  }
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

const loadSessionList = async () => {
  loadingSessions.value = true;
  try {
    const res = await getAiChatSessionsApi({ pageNum: 1, pageSize: 50 });
    if (res.code === 0 && res.data) {
      sessions.value = res.data.rows ?? [];
    }
  } catch {
    // ignore
  } finally {
    loadingSessions.value = false;
  }
};

const normalizeHistoryMessage = (dto: AiChatMessageDTO): ChatMessage => ({
  id: String(dto.messageId),
  role: dto.messageRole === 1 ? "user" : "assistant",
  content: dto.messageContent,
  sources: dto.sources ?? undefined,
  isError: dto.messageRole === 2 && !dto.messageContent,
  expanded: false,
  streaming: false
});

const loadHistory = async (sessionId: number) => {
  loadingHistory.value = true;
  try {
    const res = await getAiChatMessagesApi(sessionId);
    if (res.code === 0 && res.data) {
      messages.value = res.data.map(normalizeHistoryMessage);
      currentSessionId.value = sessionId;
      sessionStorage.setItem(SESSION_STORAGE_KEY, String(sessionId));
    } else {
      ElMessage.error(res.msg || "加载历史消息失败");
    }
  } catch {
    ElMessage.error("加载历史消息失败，可继续发送新消息");
  } finally {
    loadingHistory.value = false;
    scrollToBottom();
  }
};

const selectSession = (sessionId: number) => {
  if (loading.value || currentSessionId.value === sessionId) return;
  loadHistory(sessionId);
};

const newSession = () => {
  currentSessionId.value = undefined;
  sessionStorage.removeItem(SESSION_STORAGE_KEY);
  messages.value = [];
};

const onSessionListReady = () => {
  const saved = sessionStorage.getItem(SESSION_STORAGE_KEY);
  if (!saved) return;
  const sessionId = Number(saved);
  if (Number.isNaN(sessionId)) {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    return;
  }
  if (sessions.value.some(s => s.sessionId === sessionId)) {
    loadHistory(sessionId);
  } else {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
  }
};

const createStreamingAssistantMessage = () => {
  const aiMsg: ChatMessage = {
    id: `assistant-${Date.now()}`,
    role: "assistant",
    content: "",
    sources: [],
    isError: false,
    expanded: false,
    streaming: true
  };
  messages.value.push(aiMsg);
  return aiMsg;
};

const parseStreamEvent = (payload: string): StreamEvent | null => {
  try {
    return JSON.parse(payload) as StreamEvent;
  } catch {
    return null;
  }
};

const handleStreamEvent = async (
  event: StreamEvent,
  aiMsg: ChatMessage,
  isNewSession: boolean
) => {
  switch (event.type) {
    case "token":
      aiMsg.content += String(event.data ?? "");
      await scrollToBottom();
      break;
    case "sources":
      aiMsg.sources = (event.data as SourceInfo[]) ?? [];
      break;
    case "conv_id":
      if (typeof event.data === "number") {
        currentSessionId.value = event.data;
        sessionStorage.setItem(SESSION_STORAGE_KEY, String(event.data));
        if (isNewSession) {
          await loadSessionList();
        }
      }
      break;
    case "python_conv_id":
      break;
    case "start":
      break;
    case "done": {
      const data = event.data as { answer?: string; sources?: SourceInfo[] } | null;
      if (data?.answer) {
        aiMsg.content = data.answer;
      }
      if (data?.sources) {
        aiMsg.sources = data.sources;
      }
      aiMsg.streaming = false;
      await scrollToBottom();
      break;
    }
    case "error":
      aiMsg.isError = true;
      aiMsg.streaming = false;
      aiMsg.content = String(event.data ?? "AI 服务暂时不可用");
      await scrollToBottom();
      throw new Error(aiMsg.content);
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

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    const chunks = buffer.split("\n\n");
    buffer = chunks.pop() || "";

    for (const chunk of chunks) {
      const dataLine = chunk
        .split("\n")
        .map(line => line.trim())
        .find(line => line.startsWith("data: "));
      if (!dataLine) continue;
      const event = parseStreamEvent(dataLine.slice(6));
      if (!event) continue;
      await handleStreamEvent(event, aiMsg, isNewSession);
    }
  }
};

const send = async () => {
  const text = question.value.trim();
  if (!text || loading.value) return;

  const isNewSession = currentSessionId.value == null;
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
    if (currentSessionId.value != null) {
      await loadHistory(currentSessionId.value);
    }
  } catch (error) {
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
  onSessionListReady();
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
          新建会话
        </el-button>
      </div>

      <div class="sidebar-overview">
        <div class="overview-card">
          <span class="overview-label">会话总数</span>
          <strong>{{ sessions.length }}</strong>
        </div>
        <div class="overview-card">
          <span class="overview-label">回答模式</span>
          <strong>流式增强</strong>
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
          <span class="session-item__icon">
            <el-icon><ChatDotRound /></el-icon>
          </span>
          <span class="session-title">{{ session.sessionTitle }}</span>
        </button>
      </div>
    </aside>

    <section class="chat-main">
      <header class="chat-header">
        <div>
          <p class="chat-header__kicker">Knowledge-grounded assistant</p>
          <h1>AI 问答工作台</h1>
          <p class="chat-header__hint">支持 Markdown 渲染与流式输出，回答会更像真正的聊天产品。</p>
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
          <template v-if="msg.role === 'assistant'">
            <div class="assistant-avatar">AI</div>
          </template>

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
              placeholder="输入你的问题，让 AI 基于知识库逐字生成更可靠的回答。"
              :disabled="loading"
              rows="1"
              @input="resizeTextarea"
              @keydown.enter="handleEnter"
            />
            <el-button type="primary" class="send-btn" :icon="Promotion" :loading="loading" :disabled="!question.trim()" @click="send">
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
  align-items: center;
  padding: 32px 18px;
  text-align: center;
  color: var(--text-sub);
}

.session-empty__badge {
  width: 44px;
  height: 44px;
  margin-bottom: 12px;
  border-radius: 14px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 13px;
  font-weight: 700;
  line-height: 44px;
}

.session-empty p,
.session-empty span {
  margin: 0;
}

.session-empty p {
  color: var(--text-main);
  font-weight: 600;
}

.session-empty span {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
}

.session-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 14px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: transparent;
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
  transition: 0.2s ease;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.56);
  border-color: var(--line-soft);
}

.session-item--active {
  background: linear-gradient(180deg, rgba(255, 248, 242, 0.95), rgba(255, 244, 235, 0.9));
  border-color: rgba(191, 90, 54, 0.16);
  box-shadow: 0 12px 24px rgba(191, 90, 54, 0.08);
}

.session-item__icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--accent-strong);
  flex-shrink: 0;
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid var(--line-soft);
  border-radius: 34px;
  background: var(--bg-panel-strong);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 34px 18px;
  border-bottom: 1px solid var(--line-soft);
}

.chat-header h1 {
  font-size: 40px;
  line-height: 1.02;
}

.chat-header__hint {
  max-width: 700px;
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.chat-header__status {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 10px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--text-sub);
  font-size: 13px;
  white-space: nowrap;
}

.chat-header__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #41b66d;
  box-shadow: 0 0 0 6px rgba(65, 182, 109, 0.12);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 28px 34px;
}

.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  text-align: center;
}

.empty-title {
  margin: 0;
  color: var(--text-sub);
  font-size: 18px;
}

.chat-welcome {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
}

.chat-welcome__halo {
  position: absolute;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(222, 152, 110, 0.18), transparent 68%);
  filter: blur(8px);
}

.chat-welcome__panel {
  position: relative;
  z-index: 1;
  max-width: 760px;
  padding: 34px;
  border: 1px solid rgba(191, 90, 54, 0.12);
  border-radius: 30px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 248, 241, 0.84));
  text-align: left;
  box-shadow: 0 24px 80px rgba(86, 53, 26, 0.08);
}

.chat-welcome__icon {
  width: 62px;
  height: 62px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(191, 90, 54, 0.16), rgba(143, 63, 36, 0.1));
  color: var(--accent-strong);
  font-size: 28px;
}

.chat-welcome h3 {
  font-size: 34px;
  line-height: 1.1;
}

.chat-welcome p {
  margin: 12px 0 0;
  color: var(--text-sub);
  font-size: 15px;
  line-height: 1.8;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 26px;
}

.prompt-chip {
  min-height: 84px;
  padding: 16px;
  border: 1px solid rgba(191, 90, 54, 0.14);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.6;
  text-align: left;
  cursor: pointer;
  transition: 0.2s ease;
}

.prompt-chip:hover {
  transform: translateY(-2px);
  border-color: rgba(191, 90, 54, 0.28);
  box-shadow: 0 16px 30px rgba(191, 90, 54, 0.08);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 22px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--user .message-stack {
  align-items: flex-end;
}

.assistant-avatar {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #c56b43, #8f3f24);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  flex-shrink: 0;
  box-shadow: 0 12px 26px rgba(197, 107, 67, 0.2);
}

.message-stack {
  max-width: min(78%, 860px);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-bubble {
  position: relative;
  padding: 16px 18px;
  border: 1px solid var(--line-soft);
  border-radius: 22px 22px 22px 10px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 14px 34px rgba(60, 38, 22, 0.06);
}

.message-bubble--user {
  border-color: transparent;
  border-radius: 22px 22px 10px 22px;
  background: linear-gradient(135deg, #c86a45, #a14a2a);
  color: #fff;
}

.message-bubble--error {
  border-color: rgba(196, 68, 54, 0.2);
  background: #fff2ef;
  color: #8d3026;
}

.message-meta {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(124, 105, 89, 0.88);
}

.message-bubble--user .message-meta {
  color: rgba(255, 255, 255, 0.78);
}

.message-content,
.message-markdown {
  color: inherit;
  font-size: 14px;
  line-height: 1.85;
  word-break: break-word;
}

.message-content {
  white-space: pre-wrap;
}

.message-markdown :deep(p),
.message-markdown :deep(ul),
.message-markdown :deep(ol),
.message-markdown :deep(blockquote),
.message-markdown :deep(pre) {
  margin: 0 0 12px;
}

.message-markdown :deep(p:last-child),
.message-markdown :deep(ul:last-child),
.message-markdown :deep(ol:last-child),
.message-markdown :deep(blockquote:last-child),
.message-markdown :deep(pre:last-child) {
  margin-bottom: 0;
}

.message-markdown :deep(h1),
.message-markdown :deep(h2),
.message-markdown :deep(h3) {
  margin: 0 0 12px;
  color: var(--text-main);
  font-weight: 700;
  line-height: 1.35;
}

.message-markdown :deep(h1) {
  font-size: 20px;
}

.message-markdown :deep(h2) {
  font-size: 18px;
}

.message-markdown :deep(h3) {
  font-size: 16px;
}

.message-markdown :deep(ul),
.message-markdown :deep(ol) {
  padding-left: 20px;
}

.message-markdown :deep(li) {
  margin-bottom: 6px;
}

.message-markdown :deep(blockquote) {
  padding: 10px 14px;
  border-left: 3px solid rgba(191, 90, 54, 0.35);
  border-radius: 0 14px 14px 0;
  background: rgba(191, 90, 54, 0.06);
  color: var(--text-sub);
}

.message-markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(81, 58, 42, 0.08);
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.message-markdown :deep(.md-code) {
  overflow-x: auto;
  padding: 14px 16px;
  border-radius: 16px;
  background: #241b16;
  color: #f8ede1;
}

.message-markdown :deep(.md-code__lang) {
  margin-bottom: 10px;
  color: rgba(248, 237, 225, 0.72);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.message-markdown :deep(.md-code code) {
  padding: 0;
  background: transparent;
  color: inherit;
  font-size: 13px;
  line-height: 1.7;
}

.message-markdown :deep(a) {
  color: var(--accent-strong);
  text-decoration: underline;
}

.error-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin-right: 6px;
  border-radius: 50%;
  background: #d65144;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.stream-caret {
  width: 10px;
  height: 18px;
  display: inline-block;
  margin-top: 8px;
  border-right: 2px solid var(--accent-strong);
  animation: blink-caret 1s step-end infinite;
}

.source-panel {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(81, 58, 42, 0.08);
  overflow: hidden;
}

.source-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: none;
  background: transparent;
  color: var(--text-sub);
  font-size: 13px;
  cursor: pointer;
}

.source-toggle em {
  min-width: 24px;
  height: 24px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-style: normal;
  font-weight: 600;
  line-height: 24px;
  text-align: center;
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
