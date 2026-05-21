# DocumentKnowledgeBase 与 AI-Assistant 对接落位方案

## 目标

本文基于当前代码库现状，明确以下两件事：

1. `DocumentKnowledgeBase` 里 AI 页面应该放在哪
2. `DocumentKnowledgeBase` 里 Java 后端代理 Python 接口应该放在哪

当前原则：

- Java 继续作为主业务平台
- Python 继续作为独立 AI 服务
- 前端用户只访问 Java 项目页面
- Java 后端负责鉴权、权限上下文组装、请求转发、审计落库

## 现状结论

### 前端现有落点

当前前端已经存在 AI 页面占位目录：

- [src/views/ai/chat/index.vue](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/src/views/ai/chat/index.vue)
- [src/views/ai/audit/index.vue](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/src/views/ai/audit/index.vue)

当前知识库相关 API 已经分模块组织：

- [src/api/knowledge/document.ts](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/src/api/knowledge/document.ts)
- [src/api/knowledge/ingest.ts](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/src/api/knowledge/ingest.ts)

这说明前端不需要重新找目录，`AI 问答页` 应继续放在：

- `DocBase-Front-End/src/views/ai/chat/`

AI 相关前端 API 建议放在：

- `DocBase-Front-End/src/api/ai/`

建议新增：

- `src/api/ai/chat.ts`
- `src/api/ai/types.ts`

### 后端现有落点

当前 Java 后端已经存在 AI 模块骨架：

- [AiChatController.java](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-admin/src/main/java/com/docbase/admin/controller/ai/AiChatController.java)
- [AiChatApplicationService.java](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-domain/src/main/java/com/docbase/domain/ai/chat/AiChatApplicationService.java)

当前实现主要还是“会话列表”能力，还没有真正代理 Python 问答服务。

因此后端不需要新起一套模块，建议继续沿用现有分层：

- Controller 放在 `docbase-admin/controller/ai`
- 应用服务放在 `docbase-domain/ai/chat`
- Python HTTP 客户端与基础设施配置放在 `docbase-infrastructure`

## 推荐落位

### 一、前端页面放置位置

AI 问答正式页面继续落在：

- `DocBase-Front-End/src/views/ai/chat/index.vue`

原因：

- 现有菜单语义已经是 `ai/chat`
- 与 `views/knowledge/*` 分开，符合“AI 能力页”和“文档管理页”分层
- 后续如果加“AI 审计页”，可以继续复用 `views/ai/audit`

不建议把 Python 前端页面整体搬到：

- `views/knowledge/*`

原因：

- 知识库、文档、入库任务本来就是 Java 业务主场
- AI 问答页应该是“消费知识库”的页面，而不是重新做一套 Python 版文档后台

### 二、前端 API 放置位置

建议新增目录：

- `DocBase-Front-End/src/api/ai/`

建议文件：

- `src/api/ai/chat.ts`
- `src/api/ai/types.ts`

建议在 `chat.ts` 中收口以下接口：

- `POST /ai/chat/query`
- `GET /ai/chat/sessions`
- `POST /ai/chat/sessions`
- `GET /ai/chat/sessions/{sessionId}/messages`

如果第一阶段只做最小闭环，前端最先只需要一个接口：

- `POST /ai/chat/query`

### 三、Java Controller 放置位置

继续放在现有目录：

- `DocBase-Back-End/docbase-admin/src/main/java/com/docbase/admin/controller/ai/`

建议在现有 `AiChatController` 基础上扩展，而不是新增平行控制器。

推荐新增接口：

1. `POST /ai/chat/query`
用途：
- 前端提交问题
- Java 组装用户、部门、可见范围
- 转发给 Python 问答接口

2. `POST /ai/chat/sessions`
用途：
- 创建会话

3. `GET /ai/chat/sessions/{sessionId}/messages`
用途：
- 查询会话消息历史

4. `POST /ai/chat/messages`
用途：
- 记录一轮问答消息

第一阶段如果只追求跑通，可以先只新增：

- `POST /ai/chat/query`

### 四、Java 应用服务放置位置

继续放在现有目录：

- `DocBase-Back-End/docbase-domain/src/main/java/com/docbase/domain/ai/chat/`

建议扩展 [AiChatApplicationService.java](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-domain/src/main/java/com/docbase/domain/ai/chat/AiChatApplicationService.java)

建议新增职责：

1. 会话查询
2. 组装 Python 问答请求
3. 调用 Python 问答服务
4. 结果落库

### 五、Python HTTP 客户端放置位置

建议新增到基础设施层：

- `DocBase-Back-End/docbase-infrastructure/src/main/java/com/docbase/infrastructure/ai/`

建议文件：

- `PythonAiClient.java`
- `PythonAiProperties.java`
- `PythonAiClientConfig.java`
- `dto/PythonChatQueryRequest.java`
- `dto/PythonChatQueryResponse.java`

原因：

- Python 服务调用属于外部依赖集成
- 不应该放在 controller
- 也不建议直接塞进 domain application service

### 六、配置放置位置

Java 侧新增 Python 服务配置，建议放在：

- `docbase-admin/src/main/resources/application-dev.yml`

建议配置项：

```yaml
docbase:
  ai:
    python:
      base-url: http://localhost:8000
      api-key: your-shared-secret
      connect-timeout-ms: 3000
      read-timeout-ms: 30000
```

## 最小可行对接方案

### 第一阶段只接一个能力

先只接：

- Java 前端 `AI 问答页`
- Java 后端 `POST /ai/chat/query`
- Java 调 Python 问答接口

不要第一阶段就做：

- 文档上传即入库异步通知重构
- MinIO -> Python 拉取闭环
- 审计页完整实现
- 会话流式响应

原因：

- 当前 Python 已经最小闭环跑通
- 最快见效的是“Java 页面能问答”

### 第一阶段前端页面建议内容

基于 [src/views/ai/chat/index.vue](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/src/views/ai/chat/index.vue) 扩展：

1. 左侧区域
- 会话列表
- 新建会话按钮

2. 中间区域
- 对话消息流
- 用户消息 / AI 消息气泡

3. 底部输入区
- 问题输入框
- 发送按钮

4. 右侧或答案卡片下方
- 来源文档列表
- 文档标题
- 页码
- 片段摘要

第一版可以不做流式输出，先做普通问答即可。

### 第一阶段 Java 接口建议

建议 Java 对前端暴露：

```http
POST /ai/chat/query
```

前端请求体建议：

```json
{
  "sessionId": 1,
  "question": "会议室开放时间是什么时候？",
  "knowledgeScope": {
    "categoryId": 10,
    "documentIds": [101, 102]
  }
}
```

Java 转发给 Python 时，不必立即重构 Python 为最终 `/api/v1/documents/query` 风格。
第一阶段建议做一个“Java 适配层”，把当前 Python 现有问答接口包装起来。

## 与当前代码结构的对应关系

### 前端改造建议

保留并扩展：

- [src/views/ai/chat/index.vue](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/src/views/ai/chat/index.vue)

新增：

- `src/api/ai/chat.ts`
- `src/api/ai/types.ts`
- 如果需要消息卡片组件，可放到 `src/views/ai/chat/components/`

### 后端改造建议

扩展：

- [AiChatController.java](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-admin/src/main/java/com/docbase/admin/controller/ai/AiChatController.java)
- [AiChatApplicationService.java](/D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-domain/src/main/java/com/docbase/domain/ai/chat/AiChatApplicationService.java)

新增：

- `docbase-infrastructure/.../ai/PythonAiClient.java`
- `docbase-infrastructure/.../ai/PythonAiProperties.java`
- `docbase-domain/.../ai/chat/dto/AiChatQueryRequest.java`
- `docbase-domain/.../ai/chat/dto/AiChatAnswerDTO.java`

## 不建议现在做的事

1. 不建议把 Python 的 `rag/frontend` 整套复制到 Java 项目
2. 不建议现在就重写 Python 全部接口成最终规划版
3. 不建议第一阶段前端直连 Python

## 推荐实施顺序

### Step 1
在 Java 后端补齐 `POST /ai/chat/query`

### Step 2
在 Java infrastructure 层新增 Python 客户端

### Step 3
在前端 `src/views/ai/chat/index.vue` 上做真实问答页面

### Step 4
前端接 Java 的 `/ai/chat/query`

### Step 5
跑通：

- 登录 Java 系统
- 进入 AI 问答页
- 提问
- Java 转发 Python
- 返回答案与来源

### Step 6
第二阶段再处理文档上传后自动触发 Python 入库

## 最终建议

如果只看当前代码库现状，最合适的放置方式是：

- AI 页面放在：
  `DocBase-Front-End/src/views/ai/chat/`

- AI 前端 API 放在：
  `DocBase-Front-End/src/api/ai/`

- AI Java Controller 放在：
  `docbase-admin/.../controller/ai/`

- AI Java 应用服务放在：
  `docbase-domain/.../ai/chat/`

- Python HTTP 客户端放在：
  `docbase-infrastructure/.../ai/`

这是和你现有仓库结构最一致、改动成本最低、也最符合后续演进方向的方案。
