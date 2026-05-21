# AI 问答消息持久化设计说明

## 数据模型

表：`ai_chat_message`（建表脚本：`sql/init-ai-chat-message.sql`）

一轮问答写入两条记录：

| 写入时机 | role | error_flag | 内容 |
|----------|------|------------|------|
| Python 调用前 | 1 (user) | 0 | 用户问题 |
| Python 返回后 | 2 (assistant) | 0 | AI 回答 + sources_json |
| Python 调用失败 | 2 (assistant) | 1 | 空内容 + error_message |

## 一致性方案

当前采用**弱一致性**设计：

- user message 和 assistant message 是两次独立的 `INSERT`
- 不使用数据库事务包裹整个问答流程
- 不保证两条记录同时存在或同时不存在

可能的数据形态：

| 场景 | user 消息 | assistant 消息 |
|------|-----------|----------------|
| 正常完成 | 存在 | 存在 (error_flag=0) |
| Python 调用失败 | 存在 | 存在 (error_flag=1) |
| Python 调用成功但 DB 写入 assistant 失败 | 存在 | **不存在** |
| Python 调用前 JVM 崩溃 | **不存在** | 不存在 |

## 设计权衡

**强一致性不做**的原因：Python 调用是外部 HTTP 请求，耗时 1-30 秒不等。如果在同一个数据库事务中持有连接等待 Python 返回，会导致连接池耗尽。

**弱一致性可接受**的原因：
1. 用户消息永远先落库（try-catch 之前），最坏情况是缺 assistant 消息
2. 前端通过 `GET /sessions/{sessionId}/messages` 查询历史时，缺少 assistant 消息只是显示不完整，不会报错
3. 后续可加定时任务补齐缺失的 assistant 消息（通过 error_flag 标记或消息对检查）

## 后续增强方向（非本期）

- 引入 `message_pair_id` 关联一对问答
- 定时任务检测并修复不完整的消息对
- 异步写入助手消息（MQ/事件驱动）
