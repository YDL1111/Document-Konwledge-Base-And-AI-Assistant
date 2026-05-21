<template>
  <div class="chat-shell">
    <aside class="chat-sidebar">
      <div class="chat-sidebar__top">
        <div class="chat-sidebar__title-row">
          <button class="icon-button icon-button--muted" @click="$router.push(`/kb/${id}`)">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <p class="chat-sidebar__eyebrow">Workspace</p>
            <h2 class="chat-sidebar__title">对话记录</h2>
          </div>
        </div>
        <button class="glass-button glass-button--small" @click="newConversation">
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          新对话
        </button>
      </div>

      <div class="chat-sidebar__stats">
        <div class="chat-metric">
          <span class="chat-metric__label">会话数</span>
          <span class="chat-metric__value">{{ convs.length }}</span>
        </div>
        <div class="chat-metric">
          <span class="chat-metric__label">知识库</span>
          <span class="chat-metric__value chat-metric__value--truncate">{{ kbName || '加载中' }}</span>
        </div>
      </div>

      <div class="chat-sidebar__list">
        <button
          v-for="conv in convs"
          :key="conv.id"
          class="conv-card"
          :class="{ 'conv-card--active': activeConvId === conv.id }"
          @click="loadConversation(conv.id)"
        >
          <div class="conv-card__icon">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          </div>
          <div class="conv-card__body">
            <p class="conv-card__title">{{ conv.title }}</p>
            <p class="conv-card__meta">点击继续这段对话</p>
          </div>
          <button class="conv-card__delete" title="删除对话" @click.stop="deleteConv(conv.id)">
            <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </button>

        <div v-if="!convs.length" class="chat-sidebar__empty">
          <div class="chat-sidebar__empty-mark">01</div>
          <p>还没有历史对话</p>
          <span>发出第一个问题后，这里会自动生成会话记录。</span>
        </div>
      </div>
    </aside>

    <section class="chat-stage">
      <header class="chat-stage__header">
        <div>
          <p class="chat-stage__eyebrow">Knowledge-grounded answers</p>
          <h1 class="chat-stage__title">AI 问答工作台</h1>
        </div>
        <div class="chat-stage__badge">
          <span class="chat-stage__badge-dot"></span>
          基于 {{ kbName || '当前知识库' }}
        </div>
      </header>

      <div ref="msgContainer" class="chat-stage__messages">
        <div v-if="!messages.length" class="chat-welcome">
          <div class="chat-welcome__glow"></div>
          <div class="chat-welcome__panel">
            <div class="chat-welcome__icon">
              <svg class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <p class="chat-welcome__label">RAG Assistant</p>
            <h3 class="chat-welcome__title">让回答看起来像产品，不像接口返回</h3>
            <p class="chat-welcome__desc">
              这里会结合知识库内容回答你的问题，并尽量附上引用来源。你可以直接提问文档内容、概念解释、方案对比或实现细节。
            </p>
            <div class="chat-welcome__suggestions">
              <button class="suggestion-chip" @click="fillPrompt('帮我总结这个知识库里最重要的几个主题')">总结重点主题</button>
              <button class="suggestion-chip" @click="fillPrompt('这个知识库适合面试前怎么快速复习')">面试前快速复习</button>
              <button class="suggestion-chip" @click="fillPrompt('给我一份基于文档内容的问答清单')">生成问答清单</button>
            </div>
          </div>
        </div>

        <template v-for="msg in messages" :key="msg.id || msg._tmpId">
          <div v-if="msg.role === 'user'" class="message-row message-row--user">
            <div class="message-bubble message-bubble--user">
              <div class="message-bubble__meta">你</div>
              <div class="message-bubble__text">{{ msg.content }}</div>
            </div>
          </div>

          <div v-else class="message-row message-row--assistant">
            <div class="assistant-avatar">
              <span>AI</span>
            </div>
            <div class="assistant-stack">
              <div class="message-bubble message-bubble--assistant">
                <div class="message-bubble__meta">知识库助手</div>
                <div v-if="msg._streaming" class="prose-rag">
                  <span>{{ msg.content }}</span>
                  <span class="cursor-blink ml-1 text-[var(--accent-strong)]">|</span>
                </div>
                <div v-else class="prose-rag" v-html="renderMd(msg.content)"></div>
              </div>

              <div v-if="msg.sources && msg.sources.length" class="sources-panel">
                <button class="sources-panel__toggle" @click="msg._showSrc = !msg._showSrc">
                  <span>{{ msg._showSrc ? '收起引用' : '查看引用' }}</span>
                  <span class="sources-panel__count">{{ msg.sources.length }}</span>
                </button>
                <div v-if="msg._showSrc" class="sources-panel__list">
                  <div v-for="src in msg.sources" :key="src.index" class="source-card">
                    <div class="source-card__index">{{ src.index }}</div>
                    <div class="source-card__content">
                      <p class="source-card__title">{{ sourceTitle(src) }}</p>
                      <p class="source-card__text">{{ src.content }}</p>
                    </div>
                    <div class="source-card__score">{{ formatScore(src.score) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div v-if="thinking && !streamingMsg" class="message-row message-row--assistant">
          <div class="assistant-avatar">
            <span>AI</span>
          </div>
          <div class="message-bubble message-bubble--assistant message-bubble--thinking">
            <div class="message-bubble__meta">知识库助手</div>
            <div class="thinking-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <p class="message-bubble__hint">正在检索文档、组织答案...</p>
          </div>
        </div>
      </div>

      <footer class="chat-composer">
        <div class="chat-composer__frame">
          <div class="chat-composer__toolbar">
            <span class="chat-composer__tag">Context-aware</span>
            <span class="chat-composer__tip">Enter 发送，Shift + Enter 换行</span>
          </div>
          <div class="chat-composer__body">
            <textarea
              ref="inputRef"
              v-model="input"
              rows="1"
              placeholder="输入你的问题，让回答更清楚、更像一个认真设计过的产品。"
              class="chat-textarea"
              :disabled="thinking"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.enter.shift.exact="() => {}"
              @input="autoResize"
            ></textarea>
            <button class="send-button" :disabled="!input.trim() || thinking" @click="sendMessage">
              <span>{{ thinking ? '生成中' : '发送' }}</span>
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 12h13m0 0-4-4m4 4-4 4" />
              </svg>
            </button>
          </div>
        </div>
        <p class="chat-composer__footnote">答案基于知识库召回结果生成，仅供参考，请结合原文与业务场景判断。</p>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { chatApi, kbApi } from '@/api'
import { useAppStore } from '@/stores/app'
import { marked } from 'marked'

const route = useRoute()
const appStore = useAppStore()
const id = parseInt(route.params.id)

const messages = ref([])
const convs = ref([])
const activeConvId = ref(null)
const input = ref('')
const thinking = ref(false)
const streamingMsg = ref(null)
const msgContainer = ref(null)
const inputRef = ref(null)
const kbName = ref('')

marked.setOptions({ breaks: true, gfm: true })
const renderMd = (text) => marked.parse(text || '')

function autoResize(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 180) + 'px'
}

async function scrollToBottom(smooth = true) {
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTo({
      top: msgContainer.value.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto',
    })
  }
}

function fillPrompt(text) {
  input.value = text
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.focus()
      inputRef.value.style.height = 'auto'
      inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 180) + 'px'
    }
  })
}

function sourceTitle(src) {
  return src.page ? `${src.filename} · 第 ${src.page} 页` : src.filename
}

function formatScore(score) {
  return `${((score || 0) * 100).toFixed(0)}%`
}

function newConversation() {
  activeConvId.value = null
  messages.value = []
}

async function loadConvList() {
  try {
    const res = await chatApi.listConvs({ kb_id: id, page_size: 50 })
    convs.value = res.data.items
  } catch {}
}

async function loadConversation(convId) {
  activeConvId.value = convId
  try {
    const res = await chatApi.getMessages(convId)
    messages.value = res.data.map((m) => ({ ...m, _showSrc: false }))
    await scrollToBottom(false)
  } catch {}
}

async function deleteConv(convId) {
  try {
    await chatApi.deleteConv(convId)
    if (activeConvId.value === convId) {
      activeConvId.value = null
      messages.value = []
    }
    await loadConvList()
  } catch (e) {
    appStore.showToast(e.message, 'error')
  }
}

async function sendMessage() {
  const q = input.value.trim()
  if (!q || thinking.value) return

  input.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = '56px'
  }
  thinking.value = true

  const tmpId = Date.now()
  messages.value.push({ _tmpId: tmpId, role: 'user', content: q })
  await scrollToBottom()

  const aiMsg = reactive({
    _tmpId: tmpId + 1,
    role: 'assistant',
    content: '',
    sources: [],
    _streaming: true,
    _showSrc: false,
  })
  messages.value.push(aiMsg)
  streamingMsg.value = aiMsg

  try {
    const res = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ kb_id: id, conv_id: activeConvId.value, question: q }),
    })

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data: ')) continue
        try {
          const data = JSON.parse(line.slice(6))
          if (data.type === 'token') {
            aiMsg.content += data.data
            await scrollToBottom()
          } else if (data.type === 'sources') {
            aiMsg.sources = data.data
          } else if (data.type === 'conv_id') {
            activeConvId.value = data.data
            await loadConvList()
          } else if (data.type === 'done') {
            aiMsg._streaming = false
            streamingMsg.value = null
          } else if (data.type === 'error') {
            aiMsg.content = `请求失败：${data.data}`
            aiMsg._streaming = false
          }
        } catch {}
      }
    }
  } catch (e) {
    aiMsg.content = `请求失败：${e.message}`
    aiMsg._streaming = false
    streamingMsg.value = null
  } finally {
    thinking.value = false
    streamingMsg.value = null
    await scrollToBottom()
  }
}

onMounted(async () => {
  try {
    const res = await kbApi.get(id)
    kbName.value = res.data.name
  } catch {}
  await loadConvList()
})
</script>
