# AI 问答流式输出问题排查与修复总结

## 问题现象

AI 问答界面发起提问后，网络层能观察到 SSE 事件逐条到达浏览器，但前端界面**没有逐字流式效果**，回答在全部生成完毕后一次性"蹦"出来。

---

## 涉及的调用链路

```
Python RAG (FastAPI)
  → chat_stream() 逐 token 产出 SSE 事件
  → StreamingResponse 异步流式发送

Java 后端 (Spring Boot)
  → PythonAiClient.streamMessage() 通过 HttpURLConnection 读取 Python SSE
  → handlePythonStreamLine() 解析事件
  → sendEvent() 通过 SseEmitter 转发到浏览器

前端 (Vue 3)
  → fetch() + ReadableStream.getReader() 读取响应流
  → extractSseBlocks() 按 \n\n 分块解析 SSE
  → handleStreamEvent() 处理 token 事件
  → 更新 DOM 显示文本
```

---

## 排查过程与修复（共 5 轮）

### 第 1 轮：前端 Markdown 渲染性能问题

**怀疑点**：`v-html="renderMarkdown(msg.content)"` 在流式过程中每次 token 都对全文重新解析 Markdown，文本越长越慢。

**修复**：流式过程中改用 `{{ msg.content }}` 纯文本直显，流结束后再切回 Markdown 渲染。

**结果**：问题未解决。Markdown 解析虽降低了性能，但不是根因。

**修改文件**：`DocBase-Front-End/src/views/ai/chat/index.vue`

---

### 第 2 轮：浏览器 paint 时机问题

**怀疑点**：`await nextTick()` 只保证 Vue 虚拟 DOM 更新，不保证浏览器实际绘制（paint）。多个 token 在同一帧内处理完毕，浏览器只绘制最终结果。

**修复**：在 `scrollToBottom()` 中加入 `await requestAnimationFrame()`，让浏览器在每批 token 后有 paint 机会。

**结果**：问题未解决。rAF 在快速 token 流中不可靠——多个 rAF 回调在同一帧内统一触发。

**修改文件**：`DocBase-Front-End/src/views/ai/chat/index.vue`

---

### 第 3 轮：Java 端 Tomcat 输出缓冲问题

**怀疑点**：Tomcat 默认 8KB 输出缓冲区，SSE 事件体积小，多个事件被 Tomcat 攒到一起才发送到客户端。

**修复**：
- `response.setBufferSize(0)` — 尝试禁用缓冲
- `response.flushBuffer()` — 每次事件发送后显式 flush

**结果**：问题未解决。`setBufferSize(0)` 在 Servlet 容器中行为未定义（有的忽略，有的退回默认值）。

**修改文件**：
- `AiChatApplicationService.java`
- `AiChatController.java`

---

### 第 4 轮：前端 Vue 响应式 + 事件循环问题

**怀疑点**：
1. Vue 响应式系统对 `ref<ChatMessage[]>` 内对象的 `content` 属性变更可能批处理
2. `requestAnimationFrame` 不可靠
3. token handler 内同步处理完所有 token，浏览器无 paint 窗口

**修复**：
1. 使用 `:ref` 回调 + `document.getElementById` 双重获取流式消息 DOM 元素
2. 直接用 `element.textContent = ...` 写 DOM，绕过 Vue 虚拟 DOM
3. 在 token handler 末尾加入 `await new Promise(resolve => setTimeout(resolve, 0))`，让出控制权到 macrotask 队列

**结果**：有所改善但仍不完美。前端侧已做到逐 token 更新 DOM 并 yield 给浏览器。

**修改文件**：`DocBase-Front-End/src/views/ai/chat/index.vue`

---

### 第 5 轮：Java SSE 输出写法不规范（最终根因）✅

**怀疑点**：
1. **手工拼 SSE 格式**：`emitter.send(("data: " + json + "\n\n").getBytes(...))`——SseEmitter 本身负责 SSE 格式化，手工塞 `data:` 前缀会被容器/代理误判为普通响应块
2. **`response.setBufferSize(0)` 不可靠**：Servlet 容器对 buffer size 0 的行为不确定
3. **`SseEmitter` 超时**：默认构造无超时参数，可能受框架默认值影响

**修复**：
```java
// 修复前（错误写法）：
response.setBufferSize(0);
SseEmitter emitter = new SseEmitter();
emitter.send(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));

// 修复后（正确写法）：
SseEmitter emitter = new SseEmitter(-1L);  // -1 = 永不超时
emitter.send(SseEmitter.event().data(json));
response.flushBuffer();
```

**为什么这是根因**：
- `SseEmitter.event().data(json)` 是 Spring 的标准 SSE API，内部正确处理 SSE 分帧（`data:{json}\n\n`）
- 手工拼 `data: ` 前缀使得 SseEmitter 的底层 `ResponseBodyEmitter` 无法识别这是 SSE 事件，将其当作普通 body 字节写入
- 普通 body 写入会走 Servlet 容器的常规缓冲路径，多个小 write 被合并为一个大块
- `SseEmitter(-1L)` 确保流式连接不会因为超时被中断

**修改文件**：`AiChatApplicationService.java`

---

## 最终修复清单

| 层 | 文件 | 修改内容 | 作用 |
|---|---|---|---|
| Java | `AiChatApplicationService.java` | `sendEvent()` 改用 `SseEmitter.event().data(json)` | 标准 SSE 格式，容器正确分帧 |
| Java | `AiChatApplicationService.java` | `SseEmitter(-1L)` 替代 `new SseEmitter()` + `setBufferSize(0)` | 永不超时，不依赖不可靠的 buffer=0 |
| Java | `AiChatApplicationService.java` | 保留 `response.flushBuffer()` 作安全兜底 | 确保立即发送 |
| 前端 | `ai/chat/index.vue` | 流式消息改用 DOM `textContent` 直写 | 绕过 Vue 虚拟 DOM 批处理 |
| 前端 | `ai/chat/index.vue` | token handler 末尾 `await setTimeout(0)` | 让出控制权，浏览器可执行 paint |
| 前端 | `ai/chat/index.vue` | 流式结束切回 Markdown 渲染 | 性能优化 |

---

## 关键教训

1. **永远使用框架标准 API**：Spring 的 `SseEmitter` 提供了完整的 SSE 支持（`event().data()`、`event().name()`、`event().id()`），不应该手工拼 SSE 字节格式。

2. **`Servlet.setBufferSize(0)` 不可依赖**：不同 Servlet 容器对此参数的处理不一致，有的直接忽略，有的退回默认值。需要关闭缓冲应使用框架提供的机制。

3. **流式输出的每一层都要验证**：
   - Python 产出 SSE：通过日志验证逐 token 产出
   - Java 转发：通过浏览器 Network 面板 SSE 消息时间戳验证逐条到达
   - 前端渲染：通过 `textContent` 直写 + 事件循环 yield 确保即时显示

4. **前端流式渲染的正确模式**：
   - 不依赖 Vue/React 的虚拟 DOM 进行高频更新
   - 直接操作 DOM `textContent`
   - 通过 `setTimeout(0)` 或 `requestAnimationFrame` 让浏览器有机会 paint
