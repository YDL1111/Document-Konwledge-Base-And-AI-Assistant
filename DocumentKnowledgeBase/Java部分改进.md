# Java部分改进方案

## 1. 改造目标

本项目不是从零搭建新的后台，而是基于开源 `AgileBoot` 做二次改造，目标是：

1. 删除与“企业文档知识库”无关的冗余能力，降低系统复杂度。
2. 保留 AgileBoot 已有的通用后台核心能力，作为业务底座复用。
3. 新增“企业文档与知识管理平台”所需的领域模型、接口和存储能力。
4. 为后续 Python 智能问答系统提供稳定的文档、权限、任务和审计边界。
5. 从一开始升级到 `Java 17`，并同步升级到 `Spring Boot 3.x`，避免后续二次返工。

一句话概括：

> Java 负责“文档资产、权限、流程、审计、任务”；Python 负责“解析、切块、向量化、检索、生成”。

---

## 2. 现有 AgileBoot 能力分析

### 2.1 适合保留的核心能力

结合当前仓库结构，以下能力建议保留，并作为知识库平台底座继续使用：

| 模块/能力 | 现状 | 建议 |
|------|------|------|
| 登录认证 | JWT + Spring Security | 保留 |
| 用户管理 | `sys_user` | 保留 |
| 角色管理 | `sys_role` | 保留 |
| 部门管理 | `sys_dept` | 保留，后续作为文档权限的重要维度 |
| 菜单管理 | `sys_menu` | 保留，供后台菜单和权限点使用 |
| 参数配置 | `sys_config` | 保留，后续存 AI 服务地址、MinIO 配置等 |
| 操作日志 | `@AccessLog` + `sys_operation_log` | 保留，直接用于文档和问答审计 |
| 登录日志 | `sys_login_info` | 保留 |
| 统一返回体 | `ResponseDTO` | 保留 |
| 统一异常 | `ApiException` 等 | 保留 |
| MyBatis-Plus 基础设施 | 已集成 | 保留 |

### 2.2 需要弱化或删除的能力

AgileBoot 是通用管理后台，里面有些能力对你的项目不是核心，需要裁剪。

| 模块/能力 | 处理建议 | 原因 |
|------|------|------|
| `agileboot-api` | 删除整个模块 | 当前目标是后台管理与知识库，不做 APP 端接口 |
| 代码生成相关依赖/功能 | 删除 | 对业务无直接价值，增加维护负担 |
| 监控模块 | 先保留后弱化 | 可用于开发期排查，但不作为优先建设内容 |
| 公告管理 `notice` | 可删除或延后 | 与文档知识库主链路无关 |
| 岗位管理 `post` | 可删除或延后 | 不是一期核心能力 |
| 个人中心部分非必要功能 | 精简 | 保留基础个人信息即可 |
| 本地文件上传为主的实现 | 改造成 MinIO 优先 | 后续文档解析和服务编排更适合对象存储 |

### 2.3 当前项目的真实定位

改造后的 Java 项目应定位为：

> 企业文档知识平台后台服务

它不是一个“大而全的 OA 后台”，也不是一个“直接做 LLM 推理的 AI 服务”，而是一个中台型业务后端，职责包括：

1. 管理文档和目录结构。
2. 管理文档版本、状态、权限和生命周期。
3. 统一承接前端请求。
4. 把入库任务和问答请求转发给 Python。
5. 记录审计日志和会话记录。

---

## 3. 总体技术路线

### 3.1 职责边界

#### Java 侧职责

1. 用户、角色、部门、菜单、权限体系。
2. 文档分类、文档元数据、版本管理。
3. 文件上传到 MinIO。
4. 文档预览地址生成。
5. 入库任务创建、状态跟踪、失败重试。
6. 问答入口、会话管理、审计日志。
7. 向 Python 传递权限上下文。

#### Python 侧职责

1. 从 MinIO 获取文档文件。
2. PDF/Word/Excel/Markdown 解析。
3. 文本切块与 metadata 组装。
4. Embedding、向量写入、关键词索引。
5. 检索、重排、生成答案。
6. 返回答案和引用来源。

### 3.2 服务调用关系

```text
前端
  -> Java 后台
      -> MinIO
      -> MySQL
      -> Python RAG 服务
           -> MinIO
           -> 向量库
           -> 检索索引
           -> 大模型
```

### 3.3 为什么要这样拆

这样拆分的好处：

1. Java 专注企业业务规则，稳定性更高。
2. Python 专注 AI 能力，后续替换模型或向量库更灵活。
3. 权限和审计不落到 Python，系统边界更清晰。
4. 后面即使 AI 服务重构，Java 业务底座也不需要大改。

---

## 4. Java 17 与 Spring Boot 3 升级策略

### 4.1 升级结论

本项目建议从一开始就升级到：

- `Java 17`
- `Spring Boot 3.2.x`

不要先用 2.7 再升级。因为你现在就是在做“结构性改造”，如果这一步不一起做，后面新增的知识库代码还要跟着再迁一次，返工成本更高。

### 4.2 升级的主要改动点

1. `java.version` 从 `1.8` 升到 `17`
2. `spring.boot.version` 升到 `3.2.x`
3. `javax.*` 全面迁移到 `jakarta.*`
4. 升级依赖版本：
   - MyBatis-Plus
   - Druid
   - springdoc
   - 可能涉及 pagehelper 替代或升级
5. 检查 Spring Security 配置写法
6. 检查过滤器、校验注解、Servlet 相关代码

### 4.3 对当前仓库的影响

重点受影响位置主要包括：

1. `SecurityConfig`
2. `JwtAuthenticationTokenFilter`
3. Controller 中的校验注解
4. 文件上传下载相关 Servlet API
5. Swagger/OpenAPI 依赖

### 4.4 升级建议顺序

建议把升级放到整个项目改造的最前面，作为 `Phase 0` 的第一件事：

1. 先做编译链升级。
2. 修复 Jakarta 兼容问题。
3. 跑通现有基础功能。
4. 再开始做知识库业务改造。

这样后续新增代码就全部建立在新版本之上，不会出现“双轨维护”。

---

## 5. Java 侧建议保留/删除/新增的模块

## 5.1 建议保留

### 后台基础模块

- 用户管理
- 角色管理
- 部门管理
- 菜单管理
- 参数配置
- 登录认证
- 操作日志
- 登录日志

### 基础设施模块

- Redis 缓存
- MyBatis-Plus
- 异常处理
- 线程池/异步任务基础能力

## 5.2 建议删除或下线

### 优先删除

1. `agileboot-api`
2. 代码生成相关功能和依赖

### 可在一期不做

1. 公告管理
2. 岗位管理
3. 非核心监控页面
4. 与当前业务目标无关的系统展示页

### 处理原则

不是“为了删而删”，而是：

1. 不参与知识库主链路的功能尽量不继续扩展。
2. 优先减少维护面和菜单复杂度。
3. 管理后台只留下真正会被使用的系统能力。

## 5.3 建议新增

### 新领域模块

在 `agileboot-domain` 中新增：

1. `knowledge/category`
2. `knowledge/document`
3. `knowledge/permission`
4. `knowledge/ingest`
5. `ai/chat`
6. `ai/audit`

### 新控制器

在 `agileboot-admin` 中新增：

1. `KnowledgeCategoryController`
2. `KnowledgeDocumentController`
3. `KnowledgeIngestController`
4. `KnowledgePreviewController`
5. `AiChatController`
6. `AiSessionController`

### 新基础设施

在 `agileboot-infrastructure` 中新增：

1. `minio/MinioStorageService`
2. `client/python/PythonAiClient`
3. `client/python/dto/*`

---

## 6. 知识库业务模型设计

### 6.1 核心业务对象

Java 侧建议至少设计以下几个核心对象：

1. 文档分类
2. 文档主实体
3. 文档版本
4. 文档权限
5. 入库任务
6. 问答会话
7. 问答消息

### 6.2 建议数据表

建议核心表调整为下面这组，而不是只停留在“文件表”思维：

1. `knowledge_category`
2. `knowledge_document`
3. `knowledge_document_version`
4. `knowledge_ingest_task`
5. `knowledge_document_acl` 或二期再补
6. `ai_chat_session`
7. `ai_chat_message`
8. `ai_audit_log`

如果你想先控制一期复杂度，可以先不上 `knowledge_document_acl`，但需要预留扩展位。

### 6.3 文档主表建议字段

`knowledge_document` 建议至少包含：

- `document_id`
- `title`
- `category_id`
- `dept_id`
- `visibility`
- `status`
- `current_version`
- `current_file_name`
- `current_file_url`
- `file_type`
- `file_size`
- `summary`
- `tags`
- `creator_id`
- `create_time`
- `updater_id`
- `update_time`
- `deleted`

### 6.4 文档状态建议

建议不要只用“是否入库”描述文档状态，而是做成明确状态机：

- `draft`
- `uploaded`
- `ingesting`
- `active`
- `failed`
- `archived`

这样后续前端展示、任务重试、版本回滚会清晰很多。

### 6.5 权限模型建议

一期推荐权限模型：

- `private`
- `dept`
- `public`

二期扩展：

- `custom`

一旦加入 `custom`，再引入授权表：

- `knowledge_document_acl`

字段可以是：

- `document_id`
- `subject_type`：`user` / `role` / `dept`
- `subject_id`

---

## 7. Java 侧接口规划

### 7.1 文档管理接口

建议由 Java 提供统一入口，前端不直接访问 Python。

#### 文档分类

- `POST /knowledge/categories`
- `GET /knowledge/categories/tree`
- `GET /knowledge/categories/{id}`
- `PUT /knowledge/categories/{id}`
- `DELETE /knowledge/categories/{id}`

#### 文档管理

- `POST /knowledge/documents/upload`
- `GET /knowledge/documents`
- `GET /knowledge/documents/{id}`
- `PUT /knowledge/documents/{id}`
- `DELETE /knowledge/documents/{id}`
- `GET /knowledge/documents/{id}/versions`
- `POST /knowledge/documents/{id}/versions`
- `POST /knowledge/documents/{id}/restore`
- `GET /knowledge/documents/{id}/preview`
- `GET /knowledge/documents/{id}/download`

#### 入库任务

- `POST /knowledge/documents/{id}/ingest`
- `GET /knowledge/ingest/tasks/{taskId}`
- `POST /knowledge/ingest/tasks/{taskId}/retry`

### 7.2 AI 问答接口

- `POST /ai/chat/message`
- `GET /ai/chat/history`
- `POST /ai/chat/session`
- `GET /ai/chat/sessions`
- `DELETE /ai/chat/sessions/{id}`

### 7.3 Java 调 Python 的内部接口

建议先只定义 4 个：

1. `POST /api/v1/documents/ingest`
2. `GET /api/v1/documents/ingest/{task_id}`
3. `POST /api/v1/documents/query`
4. `DELETE /api/v1/documents/{document_id}/version/{version}`

---

## 8. Java 与 Python 的协作设计

### 8.1 入库链路

```text
用户上传文档
  -> Java 保存文档元数据
  -> Java 上传文件到 MinIO
  -> Java 创建 ingest_task
  -> Java 调用 Python /ingest
  -> Python 解析并写入向量库
  -> Python 返回任务状态
  -> Java 更新任务状态和文档状态
```

### 8.2 问答链路

```text
用户提问
  -> Java 鉴权
  -> Java 获取用户部门/角色/可见范围
  -> Java 组装 query 请求
  -> Python 检索和生成
  -> Python 返回 answer + sources
  -> Java 保存会话与审计
  -> Java 返回前端
```

### 8.3 传给 Python 的权限上下文

Java 不应该把“是否允许访问”这个责任完全丢给 Python，但应把过滤条件传过去。

建议 query 请求中至少包含：

```json
{
  "question": "员工请假制度是什么",
  "user_id": "1001",
  "dept_id": 20,
  "role_ids": [1, 3],
  "allowed_scopes": ["public", "dept"],
  "allowed_dept_ids": [20],
  "top_k": 5
}
```

这样 Python 只负责执行过滤，不负责理解组织权限规则。

---

## 9. Java 侧重点改造项

### 9.1 文件存储改造

当前 `FileController` 以本地存储为主，不适合作为知识库正式方案。

建议改造为：

1. 后台普通附件仍可兼容本地上传。
2. 知识库文档统一走 MinIO。
3. 后续预览、解析、下载全部基于对象存储地址。

### 9.2 文档上传改造

上传不应只做“传文件”，而应包含：

1. 分类
2. 文档标题
3. 可见范围
4. 部门归属
5. 标签
6. 版本说明

### 9.3 权限改造

你当前已有部门和角色体系，这正是优势。

建议：

1. 文档列表查询接入部门和可见范围过滤。
2. 文档详情访问接入权限校验。
3. AI 问答时把权限上下文传给 Python。
4. 管理员具备更高可见范围。

### 9.4 审计改造

要把“文档”和“问答”都纳入审计。

建议审计记录这些行为：

1. 上传文档
2. 更新文档
3. 删除文档
4. 触发入库
5. 重试入库
6. 发起问答
7. 下载文档
8. 访问预览

### 9.5 版本管理改造

企业文档最容易被忽略的是版本。

建议：

1. 主表仅存当前生效版本信息。
2. 历史版本放 `knowledge_document_version`。
3. 新版本上传后触发重新入库。
4. 删除旧版本时同步通知 Python 清理向量数据。

---

## 10. 建议的实施阶段

## Phase 0：基础改造

目标：先把底盘换好。

1. 创建你自己的 GitHub 仓库。
2. 在新仓库中保存当前 AgileBoot 改造起点。
3. 升级到 Java 17 + Spring Boot 3。
4. 删除 `agileboot-api`。
5. 清理代码生成相关依赖。
6. 跑通当前登录、用户、角色、部门功能。

## Phase 1：文档知识库底座

目标：先做成可用的文档管理平台。

1. 接入 MinIO。
2. 新建知识库数据表。
3. 完成分类管理。
4. 完成文档上传、列表、详情、删除。
5. 完成版本管理。
6. 完成文档权限控制。
7. 完成预览和下载。

## Phase 2：入库任务联通 Python

目标：让文档“可入库”。

1. 新增 PythonAiClient。
2. 创建入库任务表和状态流转。
3. 上传后触发入库。
4. 支持查询任务状态。
5. 支持失败重试。

## Phase 3：智能问答接入

目标：让文档“可问答”。

1. 新增问答会话与消息表。
2. 新增 `/ai/chat/message`。
3. Java 负责鉴权并转发 query。
4. 保存历史和来源引用。
5. 记录 AI 审计日志。

## Phase 4：增强与优化

1. 文档 ACL
2. 热门问题缓存
3. 问答质量评估
4. 多模型切换
5. 高级检索与 rerank
6. 运营统计和知识命中分析

---

## 11. 对 Java部分改进的最终建议

### 11.1 这次改造的正确思路

不是在 AgileBoot 上“继续堆功能”，而是：

1. 先裁剪成一个干净的后台底座。
2. 再围绕“文档资产管理”建设知识库。
3. 最后接上 Python 智能问答。

### 11.2 最重要的三个改造原则

1. Java 只做企业业务底座，不做复杂文档解析和模型推理。
2. 权限必须牢牢掌握在 Java 手里。
3. 从第一天开始就用 Java 17 和 Spring Boot 3，不走过渡方案。

---

## 12. GitHub 版本控制建议

### 12.1 改动前是否要先传到 GitHub

建议是：**要，而且应该尽快做。**

原因很直接：

1. 你接下来会做结构级改造，删模块、升版本、改依赖，风险很高。
2. 有自己的远程仓库后，可以随时回滚到某个阶段。
3. 后续不管是你自己继续做，还是让我帮你分阶段改，都更清晰。
4. 原仓库是你拉下来的开源项目，你自己的改造轨迹应该有独立版本线。

### 12.2 最稳的做法

建议不是直接在原作者仓库上继续推，而是：

1. 在 GitHub 新建你自己的仓库。
2. 把当前代码作为你的改造起点推上去。
3. 后续按阶段提交。

建议提交节奏：

1. `chore: init forked agileboot workspace`
2. `refactor: upgrade to java17 and springboot3`
3. `refactor: remove unused agileboot modules`
4. `feat: add knowledge base domain model`
5. `feat: integrate minio storage`
6. `feat: add python rag client`

### 12.3 改造前的 Git 操作建议

如果你现在还没推自己的仓库，建议按这个顺序做：

1. 先检查当前本地改动。
2. 把当前项目完整提交为一个起点版本。
3. 推到你自己的 GitHub 仓库。
4. 从这个起点开始做 Java 17 升级和结构裁剪。

### 12.4 当前仓库的一个提醒

我刚看了当前工作区，已经有未提交改动：

- `agileboot-admin/src/main/resources/application-dev.yml`
- `sql/agileboot-20230814.sql`

所以你在推 GitHub 前，建议先做一次判断：

1. 这些改动是不是你要保留的。
2. 如果要保留，就一起提交。
3. 如果只是临时实验，先整理干净再作为初始版本推上去。

---

## 13. 下一步建议

最推荐的下一步顺序：

1. 先建立你自己的 GitHub 仓库并提交当前起点。
2. 再做 Java 17 + Spring Boot 3 升级。
3. 然后裁剪 AgileBoot 冗余模块。
4. 最后开始知识库领域建模和接口建设。

如果继续往下做，下一份文档最值得补的是：

1. `Java 升级与裁剪执行清单`
2. `知识库 SQL 终版设计`
3. `Java-Python 接口协议终版`

这三份一旦定下来，项目就可以正式进入编码阶段。
