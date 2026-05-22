# SQL 使用说明

这个目录现在按“新库初始化入口”和“历史迁移/修复脚本”两类来整理。

## 目录说明

- `docbase_knowledge_bootstrap.sql`
  作用：给全新数据库做一次性初始化，得到当前项目期望的最终结构和菜单状态。
- `docbase_knowledge.sql`
  作用：基础系统表与种子数据。
- `knowledge-base-init.sql`
  作用：知识库、文档、导入任务、AI 问答等业务表初始化。
- `knowledge-document-audit-upgrade.sql`
  作用：补文档审核字段与菜单按钮。
- `knowledge-document-audit-history-upgrade.sql`
  作用：补文档审核历史表。
- `migration-fix-ai-chat-schema.sql`
  作用：把 AI 问答相关表结构对齐到当前 Java 代码所需字段。
- `migration-add-ingest-python-fields.sql`
  作用：给导入任务增加 Python RAG 对接字段。
- `knowledge-base-reconciliation.sql`
  作用：知识库/AI 菜单、按钮、角色授权的最终统一状态。
- `archive/`
  作用：存放历史阶段性 repair / verify / one-off 脚本，保留追溯价值，但不再作为新环境初始化入口。

## 推荐用法

### 1. 初始化全新数据库

适用场景：

- 本地第一次搭环境
- 新建一个空的 `docbase_knowledge` 数据库

推荐做法：先进入当前目录，再执行 bootstrap。

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End\sql
mysql -u root -p123456 < docbase_knowledge_bootstrap.sql
```

原因：

- `docbase_knowledge_bootstrap.sql` 内部使用了相对路径 `SOURCE ./xxx.sql`
- 从当前目录执行最稳妥

### 2. 升级已有数据库

适用场景：

- 旧库已经存在
- 只想补某次功能演进缺失的字段或菜单

不要直接跑 `docbase_knowledge_bootstrap.sql`，而是按需要单独执行迁移脚本，例如：

```bash
mysql -u root -p123456 -D docbase_knowledge < migration-fix-ai-chat-schema.sql
mysql -u root -p123456 -D docbase_knowledge < migration-add-ingest-python-fields.sql
```

如果是菜单/权限历史状态不一致，再酌情执行：

```bash
mysql -u root -p123456 -D docbase_knowledge < knowledge-base-reconciliation.sql
```

## 为什么要这样整理

之前这个目录里混放了几类脚本：

- 全量初始化脚本
- 增量迁移脚本
- 一次性修复脚本
- 校验脚本
- 临时合并脚本

这会带来两个问题：

1. 新同学很难判断“第一次启动到底该跑哪个”
2. 有些老修补脚本已经被后续脚本整合，再单独执行容易混淆

现在的整理原则是：

- 新库初始化只认 `docbase_knowledge_bootstrap.sql`
- 旧库升级按需执行单独 migration
- 历史 repair/verify 脚本统一放进 `archive/`

## 注意事项

### 1. bootstrap 仅适用于空库

`docbase_knowledge.sql` 和 `knowledge-base-init.sql` 中包含大量 `CREATE TABLE` 与种子数据插入，不适合直接对已有业务库重复执行。

### 2. AI 问答表最终结构以当前 Java 实体为准

当前 Java 代码依赖的关键字段包括：

- `ai_chat_session.python_conv_id`
- `ai_chat_message.sources_json`
- `ai_chat_message.python_conv_id`
- `ai_chat_message.error_flag`
- `ai_chat_message.error_message`
- `ai_chat_message.del_flag`
- `knowledge_ingest_task.python_kb_id`
- `knowledge_ingest_task.python_doc_id`

所以新库初始化时必须包含相关 migration。

### 3. archive 不代表一定永远无用

`archive/` 里的脚本主要是不再推荐作为默认入口，但在某些历史环境排障、回溯演进、比对菜单状态时仍然可能有参考价值。
