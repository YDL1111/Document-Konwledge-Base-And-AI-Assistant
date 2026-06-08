# DocBase Back End

企业文档知识库系统后端工程，基于 Spring Boot 3、Spring Security 6、MyBatis-Plus、Redis、MySQL 构建。

## 项目定位

本项目用于将通用后台框架改造为企业内部文档知识库系统，当前重点建设方向包括：

- 知识库分类管理
- 文档管理与版本管理
- 文档审核、发布、编辑、删除
- 文档入库任务与 Python RAG 同步
- AI 问答会话与消息持久化
- 轻量 Agent 业务工具接口
- 权限、菜单、角色控制

## 技术栈

- Java 17
- Spring Boot 3.2.6
- Spring Security 6
- MyBatis-Plus
- Redis
- MySQL
- Maven 多模块

## 模块说明

- `docbase-admin`：管理后台启动模块与控制器入口
- `docbase-common`：通用配置、工具类、基础常量
- `docbase-domain`：领域服务、业务模型、应用服务
- `docbase-infrastructure`：缓存、持久化、框架集成、基础设施配置

## 当前业务能力

- 系统登录、用户、角色、菜单、部门等基础后台能力
- Spring Security + JWT + Redis 登录态管理
- 知识库分类、文档、文档版本、审核记录、导入任务管理
- 文档审核通过后自动创建导入任务，并对接 Python RAG 服务
- 文档更新后重新审核与重新导入，旧 Python 文档和向量数据自动清理
- 文档删除后同步删除 Python 侧文档和向量数据
- AI 问答会话、消息、来源引用持久化
- 根据用户权限计算可见文档范围，并下沉到 Python 向量检索阶段
- 为轻量 Agent 提供导入任务、文档详情等只读工具接口

## 本地启动

### 1. 准备环境

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Redis

### 2. 初始化数据库

公开仓库推荐直接使用脱敏后的快速启动脚本：

- `sql/docbase_knowledge_public_bootstrap.sql`

示例：

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End\sql
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS docbase_knowledge DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"
mysql -u root -p docbase_knowledge < docbase_knowledge_public_bootstrap.sql
```

历史迁移脚本如果包含本地数据或隐私数据，不建议提交到公开仓库。

### 3. 修改开发环境配置

配置文件位置：

- `docbase-admin/src/main/resources/application-dev.yml`

重点确认：

- MySQL 连接
- Redis 连接
- 日志目录
- 文件存储目录
- Python RAG 服务地址：`docbase.ai.python.base-url`
- 知识库映射配置：`docbase.ai.kb-mapping.*`

### 4. 启动后端

直接运行：

- `com.docbase.admin.DocBaseAdminApplication`

或在命令行执行：

```bash
mvn -pl docbase-admin spring-boot:run "-Dspring-boot.run.profiles=basic,dev"
```

## 接口文档

系统启动后可访问：

- `/swagger-ui/index.html`

## 改造说明

该项目来源于开源后台工程，当前已经完成第一轮企业文档知识库方向改造：

- Java 17 / Spring Boot 3 升级
- `javax` 到 `jakarta` 迁移
- 包名与模块命名统一为 `docbase`
- 新增知识库核心表设计与公开版初始化 SQL
- 新增知识库菜单、按钮权限与后端接口权限标识
- 落地文档上传、审核、版本、编辑、删除全生命周期
- 落地导入任务状态流转和 Python RAG 同步
- 落地 AI 问答链路、来源引用和会话消息持久化
- 落地权限下沉到 RAG 检索层，避免无权限文档被召回
- 扩展轻量 Agent 只读工具接口，支持 Python Agent 获取业务上下文
