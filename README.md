# DocumentKnowledgeBase Workspace

这是当前工作区的总览文档。  
这个工作区不是单一项目，而是由 `Java 后端主系统 + Vue 前端管理端 + Python RAG/轻量 Agent 服务` 共同组成的一套联动式系统。

它的核心目标不是只做一个“AI 问答助手”，而是围绕企业内部知识管理场景，打通：

- 文档管理
- 知识库分类与发布
- 导入任务与 Python RAG 同步
- AI 问答
- 轻量 Agent 能力扩展

---

## 项目概述

从职责上看，这个工作区可以分为三部分：

### 1. Java 项目：主业务系统

位置：
- [DocBase-Back-End](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End)

这部分是整个系统的主干，负责承载企业后台业务能力，包括：

- 用户、角色、菜单、权限
- 知识库分类管理
- 文档上传、审核、发布、版本管理、编辑与删除
- 导入任务管理与 Python RAG 同步
- AI 问答会话与消息持久化
- Java 调 Python AI 服务
- 为轻量 Agent 提供只读业务工具接口

主要技术栈：
- Java 17
- Spring Boot 3
- Spring Security 6
- MyBatis-Plus
- MySQL
- Redis
- Maven

一句话理解：  
`Java 项目是整个系统的业务中台和数据主控端。`

---

### 2. 前端项目：管理端与 AI 交互入口

位置：
- [DocBase-Front-End](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End)

这部分是用户实际操作的界面层，负责：

- 后台管理界面
- 知识库分类页面
- 文档管理页面
- 导入任务页面
- AI 问答页面
- Agent 模式开关与执行轨迹展示

主要技术栈：
- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- pnpm

一句话理解：  
`前端项目是整个系统的操作入口，既服务后台管理，也承载 AI 问答交互。`

---

### 3. Python 项目：RAG 与轻量 Agent 服务

位置：
- [AI-Assistant/rag/backend](D:/ResumeProjects/AI-Assistant/rag/backend)

这部分不是独立业务系统，而是为 Java 主系统提供 AI 能力支撑，负责：

- 文档解析
- 文档切片与向量化
- 向量检索
- 基于可见文档范围的检索过滤
- RAG 问答
- SSE 流式输出
- 文档导入后的异步处理
- 文档更新/删除后的向量数据同步
- 轻量 Agent 的 Planner / Executor / Tools

主要技术栈：
- Python 3.10+
- FastAPI
- LangChain
- ChromaDB
- SQLAlchemy
- DeepSeek API
- HuggingFace Embedding

一句话理解：  
`Python 项目是 AI 能力服务层，负责“检索、生成、工具调用”这部分能力。`

---

## 工作区目录结构

```text
D:\ResumeProjects
├─ README.md
├─ docs
│  ├─ 知识库管理-导入任务-Python-RAG-落地设计稿.md
│  └─ 轻量agent升级计划.md
├─ DocumentKnowledgeBase
│  ├─ DocBase-Back-End
│  └─ DocBase-Front-End
└─ AI-Assistant
   └─ rag
      └─ backend
```

---

## 三个项目是怎么协作的

### 场景 1：知识库文档导入

1. 用户在前端上传文档。
2. Java 后端保存文档元数据、版本信息和审核状态。
3. 文档审核通过后，Java 自动创建导入任务。
4. 用户执行导入任务，Java 调用 Python `/api/doc/upload`。
5. Python 进行文档解析、切片、向量化并写入知识库。
6. Java 记录 Python 侧文档 ID，并通过导入任务状态与 Python 文档状态建立同步关系。
7. 如果文档后续更新，会重新进入审核与导入流程；新版本导入成功后，旧的 Python 文档和向量数据会被清理。
8. 如果文档被删除，Java 会同步调用 Python 删除接口，避免 AI 继续召回旧内容。

这个链路的重点是：  
`文档主数据在 Java，AI 检索能力落在 Python。`

---

### 场景 2：AI 问答

1. 用户在前端 AI 问答页提问。
2. 前端请求 Java `/ai/chat/stream`。
3. Java 创建或续用会话，保存用户消息。
4. Java 根据当前登录用户、知识库分类和导入任务状态计算可见文档范围。
5. Java 调用 Python `/api/chat/stream`，并携带 `visible_doc_ids`。
6. Python 在向量检索阶段按 `doc_id` 过滤，只召回当前用户有权限访问的文档片段。
7. Python 基于召回片段生成回答并返回来源引用。
8. Java 转发 SSE 给前端，同时把最终回答落库。

这个链路的重点是：  
`会话与权限体系在 Java，权限边界下沉到 Python 检索层，检索与生成在 Python。`

---

### 场景 3：轻量 Agent

1. 管理员用户在前端打开 Agent 模式，普通用户当前仅使用标准 AI 问答模式。
2. 前端请求 Java `/ai/chat/agent/stream`。
3. Java 保存会话并转发给 Python Agent 接口。
4. Python 由 Planner 决策是否调用工具。
5. Python 可调用：
   - `search_kb`：本地知识库检索，支持按业务文档定向召回
   - `list_ingest_tasks`：查询导入任务列表
   - `get_ingest_task_detail`：查询单个导入任务详情
   - `get_document_detail`：查询知识文档详情
   - `list_documents_by_category`：按分类查询文档列表
   - `get_kb_mapping_info`：查询业务分类与 Python 知识库映射关系
6. 前端展示最终回答和执行轨迹。

这个链路的重点是：  
`Agent 逻辑仍然在 Python，但业务上下文和数据根基仍然在 Java。`

补充说明：  
- 当前 Agent 能力仅对管理员用户开放，Java 端会校验管理员身份后才允许进入 `/ai/chat/agent/stream`。
- Agent 侧工具以只读能力为主，避免在第一阶段引入高风险业务写操作。

---

## 当前已经覆盖的主要能力

### Java 侧

- 基础后台权限体系
- 知识库分类管理
- 文档上传、审核、发布、版本管理、编辑、删除
- 导入任务管理
- AI 问答会话与消息持久化
- Python RAG 对接
- 权限下沉到 RAG 检索层
- 文档更新/删除后的 Python 向量数据同步
- Agent 只读工具接口

### 前端侧

- 后台管理页面
- 文档管理页面
- 导入任务页面
- AI 问答页面
- 会话列表与历史切换
- Agent 模式与执行轨迹展示

### Python 侧

- 文档上传与解析
- 向量化入库
- RAG 检索问答
- 按 `visible_doc_ids` 过滤向量召回结果
- SSE 流式事件输出
- 轻量 Agent 第一版闭环

---

## 环境要求

### 基础依赖

- Windows 10/11
- Git
- MySQL 8.x
- Redis

### Java 项目

- JDK 17
- Maven 3.9+

### 前端项目

- Node.js 18+
- pnpm

### Python 项目

- Python 3.10+
- 建议使用虚拟环境

---

## 快速启动

推荐启动顺序：

1. MySQL
2. Redis
3. Python RAG 服务
4. Java 后端
5. 前端

这样可以尽量避免：

- Java 调 Python 超时
- 导入任务执行失败
- AI 问答报服务不可用

---

### 1. 准备数据库

当前 Java 开发库名：
- `docbase_knowledge`

配置参考：
- [application-dev.yml](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-admin/src/main/resources/application-dev.yml)

公开仓库推荐使用脱敏后的快速启动脚本：

- [docbase_knowledge_public_bootstrap.sql](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/sql/docbase_knowledge_public_bootstrap.sql)

这个脚本包含当前项目需要的表结构和少量演示数据，不包含私人账号、聊天记录、上传文件路径或真实知识库内容。

执行命令：

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End\sql
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS docbase_knowledge DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"
mysql -u root -p docbase_knowledge < docbase_knowledge_public_bootstrap.sql
```

更多说明见：
- [SQL README](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/sql/README.md)

---

### 2. 启动 Redis

默认配置：

- host: `localhost`
- port: `6379`

---

### 3. 启动 Python 服务

```bash
cd D:\ResumeProjects\AI-Assistant\rag\backend
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

注意：请确认命令行前缀已经进入 `venv`，不要直接用 Conda `base` 或全局 Python 启动，否则可能出现 `ModuleNotFoundError: fastapi` 等依赖缺失问题。

或：

```bash
cd D:\ResumeProjects\AI-Assistant\rag\backend
venv\Scripts\activate
uvicorn main:app --reload --port 8000
```

默认地址：
- `http://localhost:8000`

---

### 4. 启动 Java 后端

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End
mvn spring-boot:run
```

或：

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End
mvn -pl docbase-admin spring-boot:run "-Dspring-boot.run.profiles=basic,dev"
```

默认地址：
- `http://localhost:8080`

Swagger：
- `http://localhost:8080/swagger-ui/index.html`

---

### 5. 启动前端

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Front-End
pnpm install
pnpm dev
```

如果 Husky 报错：

```powershell
$env:HUSKY=0
pnpm install
```

默认访问：
- `http://localhost`

---

## Docker Desktop 部署

如果不想分别启动 MySQL、Redis、Java、前端和 Python，可以使用根目录下的 Docker Compose 一键启动。

### 1. 前置条件

- 已安装并启动 Docker Desktop。
- Docker Desktop 使用 Linux containers。
- 本机端口不要被占用：`3080`、`8081`、`8001`、`3307`、`6380`、`3001`。
- 如果使用 HuggingFace Embedding，请确保本机已有模型缓存，或允许容器联网下载模型。

当前 `docker-compose.yml` 中 Python RAG 默认挂载本机 HuggingFace 缓存：

```yaml
C:/Users/15567/.cache/huggingface:/root/.cache/huggingface:ro
```

如果换电脑部署，需要把左侧路径改成新机器上的 HuggingFace 缓存目录；如果没有缓存，导入任务可能会在向量化阶段报 `couldn't connect to https://huggingface.co`。

### 2. 启动命令

在工作区根目录执行：

```bash
cd D:\ResumeProjects
set DEEPSEEK_API_KEY=your_deepseek_api_key
set DOCBASE_INTERNAL_API_KEY=your_internal_agent_api_key
docker compose build
docker compose up -d
```

如果使用 PowerShell，也可以写成：

```powershell
$env:DEEPSEEK_API_KEY="your_deepseek_api_key"
$env:DOCBASE_INTERNAL_API_KEY="your_internal_agent_api_key"
docker compose up -d --build
```

查看服务状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f docbase-backend
docker compose logs -f python-rag-backend
docker compose logs -f docbase-frontend
```

停止服务：

```bash
docker compose down
```

如果只修改了 Python RAG 代码，可以只重建 Python 服务：

```bash
docker compose build python-rag-backend
docker compose up -d python-rag-backend
```

### 3. Docker 端口说明

| 服务 | 容器名 | 宿主机访问地址 |
| --- | --- | --- |
| DocBase 前端 | `docbase-frontend` | `http://localhost:3080` |
| Java 后端 | `docbase-backend` | `http://localhost:8081` |
| Python RAG 后端 | `python-rag-backend` | `http://localhost:8001` |
| MySQL | `docbase-mysql` | `localhost:3307` |
| Redis | `docbase-redis` | `localhost:6380` |
| RAG 独立前端 | `rag-frontend` | `http://localhost:3001` |

日常使用主入口：

```text
http://localhost:3080
```

前端通过 Nginx 将 `/dev-api/**` 代理到 Java 后端，因此浏览器里一般访问 `3080` 即可。

### 4. Docker 数据初始化

MySQL 容器首次启动时会加载：

```text
DocumentKnowledgeBase/DocBase-Back-End/sql
```

公开仓库建议只保留脱敏后的启动脚本，例如：

```text
DocumentKnowledgeBase/DocBase-Back-End/sql/docbase_knowledge_public_bootstrap.sql
```

注意：Docker volume 一旦创建，后续修改 SQL 文件不会自动重新初始化数据库。如果需要用新的 SQL 重建 Docker 数据库，需要先删除旧 volume：

```bash
docker compose down -v
docker compose up -d mysql
```

这会清空 Docker 内的 MySQL、Redis、ChromaDB 等 volume 数据，请只在确认不需要保留 Docker 测试数据时执行。

---

## 常用开发命令

### Java

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End
mvn spring-boot:run
mvn test
./mvnw -q -DskipTests compile
```

### 前端

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Front-End
pnpm dev
pnpm.cmd typecheck
pnpm build
```

### Python

```bash
cd D:\ResumeProjects\AI-Assistant\rag\backend
venv\Scripts\activate
python main.py
uvicorn main:app --reload --port 8000
```

---

## 关键配置文件

### Java

- [application.yml](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-admin/src/main/resources/application.yml)
- [application-dev.yml](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/docbase-admin/src/main/resources/application-dev.yml)

重点关注：

- 数据库连接
- Redis 连接
- `docbase.ai.python.base-url`
- `docbase.ai.python.api-key`
- `docbase.ai.kb-mapping.*`

### Python

- [config.py](D:/ResumeProjects/AI-Assistant/rag/backend/app/core/config.py)

重点关注：

- `DATABASE_URL`
- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `HF_EMBEDDING_MODEL`
- `HF_LOCAL_FILES_ONLY`
- `JAVA_BASE_URL`
- `JAVA_API_KEY`
- `AGENT_MAX_STEPS`

### 前端

- `D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Front-End\.env`
- `D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Front-End\.env.development`

重点确认：

- `VITE_APP_BASE_API`
