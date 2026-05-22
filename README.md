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
- 文档上传、审核、发布、版本管理
- 导入任务管理
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
- RAG 问答
- SSE 流式输出
- 文档导入后的异步处理
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
│  ├─ Codex会话交接-当前进度.md
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

这个链路的重点是：  
`文档主数据在 Java，AI 检索能力落在 Python。`

---

### 场景 2：AI 问答

1. 用户在前端 AI 问答页提问。
2. 前端请求 Java `/ai/chat/stream`。
3. Java 创建或续用会话，保存用户消息。
4. Java 调用 Python `/api/chat/stream`。
5. Python 检索知识库并生成回答。
6. Java 转发 SSE 给前端，同时把最终回答落库。

这个链路的重点是：  
`会话与权限体系在 Java，检索与生成在 Python。`

---

### 场景 3：轻量 Agent

1. 用户在前端打开 Agent 模式。
2. 前端请求 Java `/ai/chat/agent/stream`。
3. Java 保存会话并转发给 Python Agent 接口。
4. Python 由 Planner 决策是否调用工具。
5. Python 可调用：
   - 本地知识库检索工具
   - Java 提供的只读业务工具接口
6. 前端展示最终回答和执行轨迹。

这个链路的重点是：  
`Agent 逻辑仍然在 Python，但业务上下文和数据根基仍然在 Java。`

---

## 当前已经覆盖的主要能力

### Java 侧

- 基础后台权限体系
- 知识库分类管理
- 文档上传、审核、发布、版本管理
- 导入任务管理
- AI 问答会话与消息持久化
- Python RAG 对接
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

如果你已经接入“导入任务 -> Python RAG 同步”能力，需要执行迁移脚本：

- [migration-add-ingest-python-fields.sql](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/sql/migration-add-ingest-python-fields.sql)

执行命令：

```bash
mysql -u root -p123456 -D docbase_knowledge -e "source D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/sql/migration-add-ingest-python-fields.sql"
```

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
pnpm typecheck
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

---

## 建议重点阅读的子项目文档

- [DocBase-Back-End/README.md](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Back-End/README.md)
- [DocBase-Front-End/README.md](D:/ResumeProjects/DocumentKnowledgeBase/DocBase-Front-End/README.md)
- [AI-Assistant/rag/README.md](D:/ResumeProjects/AI-Assistant/rag/README.md)

---

## docs 目录补充文档

当前比较重要的补充文档有：

- [Codex会话交接-当前进度.md](D:/ResumeProjects/docs/Codex会话交接-当前进度.md)
- [知识库管理-导入任务-Python-RAG-落地设计稿.md](D:/ResumeProjects/docs/知识库管理-导入任务-Python-RAG-落地设计稿.md)
- [轻量agent升级计划.md](D:/ResumeProjects/docs/轻量agent升级计划.md)

---

## 当前项目的定位建议

如果你要向别人介绍这个工作区，更准确的说法不是：

- “这是一个 AI 问答助手项目”

而是：

- “这是一个企业知识库管理系统工作区，Java 负责业务主系统，Vue 负责管理端与问答交互，Python 负责 RAG 和轻量 Agent 能力。”

这样更符合当前项目实际，也更适合写进简历、README 或面试说明里。
