# 企业文档知识库后端改造骨架

## 当前定位

当前后端工程不再作为通用开放接口平台扩展，而是收敛为企业文档知识库系统的业务底座。

保留的核心能力：

- 登录认证
- 用户、角色、部门、菜单
- 参数配置
- 操作日志、登录日志
- 统一异常、统一返回、缓存与基础设施

弱化或移除的能力：

- `docbase-api` 开放接口模块
- 与知识库主链路无关的演示型或通用型扩展功能

## 改造后的模块职责

### `docbase-admin`

负责后台管理入口与控制器层，后续新增的知识库控制器建议集中在以下包内：

- `com.docbase.admin.controller.knowledge`
- `com.docbase.admin.controller.ai`

### `docbase-domain`

负责知识库领域模型与应用服务，建议后续围绕以下目录建模：

- `com.docbase.domain.knowledge.category`
- `com.docbase.domain.knowledge.document`
- `com.docbase.domain.knowledge.ingest`
- `com.docbase.domain.knowledge.permission`
- `com.docbase.domain.ai.chat`
- `com.docbase.domain.ai.audit`

### `docbase-infrastructure`

负责外部集成与基础设施封装，建议后续新增：

- `com.docbase.infrastructure.minio`
- `com.docbase.infrastructure.client.python`
- `com.docbase.infrastructure.client.python.dto`

## 推荐的数据表方向

这一轮不落 SQL，只固定骨架命名：

- `knowledge_category`
- `knowledge_document`
- `knowledge_document_version`
- `knowledge_ingest_task`
- `knowledge_document_acl`
- `ai_chat_session`
- `ai_chat_message`
- `ai_audit_log`

## 下一阶段编码入口

建议按这个顺序进入真正开发：

1. Java 17 与 Spring Boot 3 升级
2. MinIO 接入与文档上传链路改造
3. 知识库分类、文档、版本、任务模型落地
4. Java 到 Python 的内部接口客户端封装
5. AI 问答与审计链路接入
