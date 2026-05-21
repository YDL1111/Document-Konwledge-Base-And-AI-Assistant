# 知识库管理 -> 导入任务 -> Python RAG 落地设计稿

## 1. 目标

将 `DocumentKnowledgeBase` 的“知识库管理”能力与 `AI-Assistant` 的 Python RAG 检索能力正式打通，形成一条稳定、可追踪、可重试的同步链路：

`知识库管理（Java） -> 导入任务（Java） -> Python RAG（FastAPI） -> AI 问答`

本设计的核心目标：

- 以 Java 侧知识库管理为主数据源
- 以 Python 侧知识库为检索副本
- 用“导入任务”承担异步同步、状态跟踪、失败重试
- 让 AI 问答只依赖已成功导入到 Python 的内容

---

## 2. 当前现状

### 2.1 Java 侧已有能力

- 知识库分类管理
- 知识文档管理
- 文档审核/审计
- AI 问答会话与消息持久化
- `categoryId -> kbId` 的配置映射能力

### 2.2 Python 侧已有能力

- `knowledge_bases` 知识库表
- `documents` 文档表
- 文件上传/解析/向量化
- `/api/chat/send`
- `/api/chat/stream`
- `/api/document/upload`
- `/api/kb/*`

### 2.3 当前缺口

当前 Java 与 Python 之间只有“问答调用链”，没有正式的“文档同步链”。

表现为：

- Java 文档新增后，不会自动进入 Python RAG
- Python 中有哪些文档，Java 不可追踪
- 文档审核状态与 AI 可检索状态未打通
- 文档更新/删除后，Python 侧无法自动增量同步

---

## 3. 总体架构

### 3.1 主从定位

- Java 知识库管理：主库
- Python RAG 知识库：从库

职责划分：

- Java 负责业务主数据、审核、权限、任务编排
- Python 负责文件解析、切分、向量化、检索、问答

### 3.2 同步链路

1. 用户在 Java 侧上传或维护文档
2. 文档进入“审核通过/可发布”状态
3. Java 创建一条导入任务
4. 导入任务根据映射规则解析目标 `python kb_id`
5. Java 调用 Python 文档导入接口
6. Python 完成上传、解析、向量化
7. Java 回写同步状态、Python 文档 ID、错误信息
8. AI 问答只检索已同步成功的数据

---

## 4. 为什么要利用“导入任务”模块

“导入任务”非常适合做这条桥接链路，原因如下：

- 文档导入和向量化是耗时操作，不适合同步阻塞前端请求
- 需要失败重试能力
- 需要任务状态可视化
- 需要批量补数、批量重建、历史回放
- 需要为后续“文档删除同步”“文档重导入”预留统一入口

结论：

“导入任务”不应只是一个附属页面，而应升级为“Java 与 Python 知识库同步中台”。

---

## 5. 推荐落地方案

## 5.1 核心原则

- 不让 AI 问答直接读取 Java 文档表
- 不让前端直接调用 Python 上传文档
- 所有同步行为统一走 Java 导入任务
- AI 问答只依赖 Python 已完成向量化的数据

## 5.2 业务规则

### 文档生命周期规则

- 草稿：不允许进入 Python
- 待审核：不允许进入 Python
- 审核通过：允许创建导入任务
- 已发布：可被 AI 检索
- 已下线/已删除：触发 Python 删除或失效同步

### 增量同步规则

- 新增文档：创建“导入任务”
- 更新文档：创建“重导入任务”
- 删除文档：创建“删除同步任务”
- 分类迁移：根据新分类重新解析 `kb_id`，必要时做“先删后导”

---

## 6. 数据模型设计

推荐新增一张“文档同步表”，再复用现有“导入任务”表或模块。

## 6.1 新增表：`knowledge_document_sync`

建议字段：

- `id`：主键
- `document_id`：Java 文档 ID
- `category_id`：Java 分类 ID
- `python_kb_id`：目标 Python 知识库 ID
- `python_doc_id`：Python 文档 ID
- `sync_status`：同步状态
- `sync_type`：同步类型
- `source_version`：源文档版本号或更新时间戳
- `last_sync_time`：最近同步时间
- `last_error_message`：最近失败原因
- `retry_count`：重试次数
- `deleted`：逻辑删除
- `create_time`
- `update_time`

### `sync_status` 推荐值

- `WAITING`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `DELETED`

### `sync_type` 推荐值

- `IMPORT`
- `REIMPORT`
- `DELETE`

## 6.2 导入任务表

如果当前已有通用导入任务表，建议复用。

如果当前导入任务模块还比较轻，可让它承载以下信息：

- 任务来源：知识库文档
- 任务目标：Python RAG
- 任务类型：导入/重导入/删除
- 任务状态：待执行/执行中/成功/失败
- 关联主键：`document_id`
- 执行日志：接口请求、响应摘要、错误信息

---

## 7. 映射设计

## 7.1 Java 分类 -> Python 知识库

这部分你已经有一版 `categoryId -> kbId` 配置映射，可以继续沿用。

优先级建议保持：

1. 前端显式 `kbId`
2. `documentId -> categoryId -> kbId`
3. `categoryId -> kbId`
4. `default-kb-id`

## 7.2 Java 文档 -> Python 文档

这是当前必须补齐的映射。

建议通过 `knowledge_document_sync` 保存：

- Java `document_id`
- Python `python_doc_id`

这样后续才能支持：

- 文档重导入
- 文档删除同步
- 文档状态追踪
- 差异校验

---

## 8. 接口协作设计

## 8.1 Java -> Python 推荐调用

### 1. 确认知识库

调用 Python `kb` 接口确认目标 `kb_id` 是否存在。

若不存在，有两种策略：

- 严格模式：任务失败，提示映射未配置
- 自动创建模式：Java 调 Python 创建知识库接口

第一期建议用“严格模式”，更稳。

### 2. 上传文档

Java 导入任务调用 Python：

- `/api/document/upload`

建议传参：

- `kb_id`
- 文件本体
- 原始文件名
- 可选元数据，如 Java `document_id`

### 3. 删除文档

建议后续补 Python 文档删除接口，供 Java 删除同步使用。

如果 Python 当前还没有“单文档删除接口”，第一期可先做：

- Java 文档删除后，仅标记同步状态为 `DELETED`
- Python 暂不删，后续补齐

但正式方案仍建议补 Python 删除接口。

---

## 9. 任务状态机

## 9.1 导入任务状态流转

- `WAITING`
- `RUNNING`
- `SUCCESS`
- `FAILED`

### 流转规则

1. 文档审核通过
2. 创建任务，状态 `WAITING`
3. 调度器/手工触发执行，改为 `RUNNING`
4. Python 导入成功，改为 `SUCCESS`
5. Python 导入失败，改为 `FAILED`
6. 允许手工“重试”

## 9.2 删除任务状态流转

- `WAITING`
- `RUNNING`
- `SUCCESS`
- `FAILED`

删除任务成功后：

- `knowledge_document_sync.sync_status = DELETED`

---

## 10. 与审核流的联动

## 10.1 推荐联动点

- 审核通过：自动创建导入任务
- 审核驳回：不创建任务
- 审核后再次修改：将同步状态置为“待重导入”
- 文档下线：创建删除同步任务

## 10.2 为什么必须接审核流

否则会出现：

- 未审核文档被 AI 检索
- 已驳回内容仍在 Python 侧可被召回
- 文档更新后 AI 仍回答旧版本内容

---

## 11. 前端改造建议

## 11.1 知识库管理页面

建议增加以下展示：

- AI 同步状态
- 最近同步时间
- Python 知识库 ID
- Python 文档 ID
- 失败原因

## 11.2 文档操作按钮

建议新增：

- `导入到AI知识库`
- `重新导入`
- `删除AI副本`
- `查看导入记录`

## 11.3 导入任务页面

建议支持：

- 按状态筛选
- 按文档筛选
- 查看失败原因
- 手工重试
- 批量执行

---

## 12. 第一阶段最小可用方案

如果你想先尽快打通，建议先做 MVP：

### 范围

- 不做自动创建 Python 知识库
- 不做删除同步
- 不做增量 diff
- 只做“审核通过 -> 导入任务 -> Python 上传”

### MVP 包含

1. 新增 `knowledge_document_sync`
2. 文档审核通过后可创建导入任务
3. 导入任务调用 Python `/api/document/upload`
4. 成功后回写 `python_kb_id / python_doc_id / sync_status`
5. AI 问答继续复用现有 `categoryId -> kbId`

### MVP 价值

- 先让知识库管理和 AI 问答真正接通
- 后续再补删除、重导入、批量修复

---

## 13. 第二阶段增强方案

在 MVP 跑稳后，建议继续补：

- Python 文档删除接口
- 文档重导入能力
- 分类迁移自动同步
- 批量历史补导
- 定时巡检任务
- Java/Python 差异对账报表

---

## 14. 失败场景与处理

## 14.1 典型失败场景

- `kb_id` 映射缺失
- Python 服务不可用
- 文件上传失败
- 文件解析失败
- 向量化失败
- 文档重复导入
- Java 文档已删但 Python 仍存在

## 14.2 处理策略

- 所有失败都必须落导入任务日志
- 所有失败都必须可重试
- Python 失败不影响 Java 主数据保存
- AI 问答只读 `SUCCESS` 状态对应的数据

---

## 15. 推荐实施顺序

### 第 1 步

梳理 Java 文档审核通过的触发点。

### 第 2 步

新增 `knowledge_document_sync` 表。

### 第 3 步

让导入任务模块支持“知识库文档导入到 Python”。

### 第 4 步

封装 Java -> Python 文档上传客户端。

### 第 5 步

导入成功后回写 `python_doc_id` 和同步状态。

### 第 6 步

知识库管理页面展示同步状态。

### 第 7 步

补删除同步与重导入。

---

## 16. 最终结论

你的系统最合理的落地方式是：

- **知识库管理留在 Java**
- **AI 检索留在 Python**
- **导入任务承担同步桥接**

所以答案是：

**是的，应该利用“导入任务”模块，而且它应该成为知识库管理与 Python RAG 对接的核心中台。**

它不是可选装饰，而是正式落地时最适合承接“异步同步、状态跟踪、失败重试、历史补数”的模块。

如果后续继续实施，建议优先从 MVP 版本开始：

- 分类映射沿用现有 `categoryId -> kbId`
- 文档审核通过后创建导入任务
- 导入任务调用 Python 文档上传接口
- 成功后记录 `python_doc_id`
- AI 问答只读 Python 已导入内容

这样改动最小，但业务链条最完整。
