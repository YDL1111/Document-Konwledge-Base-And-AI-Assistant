# 轻量 Agent 升级计划

## 1. 目标定义

本次升级不追求做一个“大而全”的通用 Agent，而是在现有 `DocBase AI 问答 + Python RAG` 基础上，升级为一个**面向知识库问答场景的轻量 Agent**。

目标边界：

- 保留现有 RAG 检索问答能力
- 增加有限的工具调用能力
- 支持有限步数的多轮执行
- 支持执行轨迹输出
- 优先做只读型能力，避免高风险自动操作

不作为第一阶段目标的能力：

- 多 Agent 协作
- 长期记忆体系
- 自动规划复杂工作流
- 高风险写操作自动执行
- 通用 MCP 全量接入

---

## 2. 当前基础与升级价值

### 2.1 现有基础

当前系统已经具备以下基础能力：

1. 前端有可用的聊天界面
2. Java 后端有会话、消息、历史持久化能力
3. Python 端已有 RAG 检索、问答、流式输出能力
4. Java -> Python 接口已经打通
5. 知识库管理、导入任务正在建设中

### 2.2 升级价值

从产品能力上看，当前系统主要是：

`用户提问 -> 检索知识库 -> 生成回答`

升级为轻量 Agent 后，可以变成：

`用户提问 -> 判断是否需要工具 -> 调用知识库/业务工具 -> 汇总结果 -> 生成回答`

这会让系统从“AI 问答助手”升级为“知识库业务助手”。

---

## 3. 总体架构建议

## 3.1 分层原则

建议采用如下分工：

- Python：负责轻量 Agent 主循环、RAG、工具选择、工具结果汇总
- Java：继续提供业务数据和业务工具接口
- 前端：展示对话、步骤状态、工具执行轨迹

### 3.2 为什么把轻量 Agent 放在 Python

原因如下：

1. 当前 RAG、模型调用、流式输出都已在 Python 侧完成
2. Python 更适合做 prompt 编排、工具选择、多步循环
3. 现阶段做轻量 Agent，Python 落地成本最低
4. 可先快速验证效果，后续再决定是否把部分能力沉到 Java

### 3.3 升级后的核心链路

升级后推荐链路如下：

1. 前端继续请求 Java `/ai/chat/stream`
2. Java 继续负责会话、权限、消息持久化、SSE 转发
3. Java 调 Python 新的 Agent 接口，例如 `/api/chat/agent/stream`
4. Python 内部执行：
   - 意图判断
   - 工具选择
   - 工具执行
   - 多步推理
   - 回答生成
5. Python 持续以 SSE 返回：
   - step
   - tool_call
   - tool_result
   - token
   - done

也就是说：

- 前端和 Java 整体交互模式尽量不推翻
- 主要升级 Python 的“内部智能决策层”

---

## 4. 第一阶段能力范围

第一阶段只做**轻量、可控、可演示、可落地**的 Agent 能力。

### 4.1 第一阶段必须实现

1. **意图识别**
   - 判断用户问题是普通知识问答
   - 还是需要调用工具

2. **工具调用**
   - 先支持只读型工具
   - 避免自动修改业务数据

3. **有限步数执行**
   - 最多 3 到 5 步
   - 防止无限循环

4. **执行轨迹输出**
   - 返回“正在检索知识库”
   - 返回“正在查询文档”
   - 返回“正在查询导入任务”
   - 提升用户对 Agent 的感知

5. **结果汇总**
   - 将工具结果整理后，再交给模型生成最终回答

### 4.2 第一阶段不做自动写操作

第一阶段不要直接做这些动作：

- 自动删除会话
- 自动删除 Python 文档
- 自动重试导入任务
- 自动修改知识库

这些能力建议第二阶段再按权限逐步开放。

---

## 5. 推荐工具清单

第一阶段建议只做 4 到 6 个工具，够用即可。

### 5.1 核心工具

#### 工具 1：`search_kb`

用途：

- 基于当前知识库做检索
- 作为原有 RAG 能力的工具化封装

输入：

- question
- kb_id
- top_k

输出：

- chunks
- sources

#### 工具 2：`get_document_detail`

用途：

- 按文档 id 查询文档详情
- 返回标题、分类、摘要、状态等信息

依赖：

- Java 提供只读接口

#### 工具 3：`list_ingest_tasks`

用途：

- 查询导入任务列表
- 支持按状态筛选

典型问题：

- “最近导入失败的文档有哪些”
- “哪些导入任务还在处理中”

#### 工具 4：`get_ingest_task_detail`

用途：

- 查询单个导入任务详情
- 返回失败原因、python_kb_id、python_doc_id、时间信息

#### 工具 5：`get_kb_mapping_info`

用途：

- 查询分类与 Python 知识库映射关系
- 帮助回答“某个文档会导入到哪个知识库”

#### 工具 6：`list_documents_by_category`

用途：

- 按分类筛选文档
- 支持知识库运营型问答

### 5.2 第二阶段可扩展工具

- `retry_ingest_task`
- `process_ingest_task`
- `delete_python_document`
- `list_chat_sessions`
- `get_chat_history`

---

## 6. Python 端模块升级设计

建议在 Python 项目中新增一套轻量 Agent 模块，而不是把现有 `chat.py`、`rag.py` 直接写乱。

### 6.1 推荐目录结构

建议新增如下目录：

```text
rag/backend/app/
  agent/
    __init__.py
    schemas.py
    planner.py
    executor.py
    tools/
      __init__.py
      base.py
      kb_tools.py
      java_api_tools.py
    prompts/
      agent_system_prompt.py
```

### 6.2 各模块职责

#### `schemas.py`

定义 Agent 运行时结构：

- AgentRequest
- AgentStep
- ToolCall
- ToolResult
- AgentFinalAnswer

#### `planner.py`

负责：

- 根据用户问题决定是否调用工具
- 产出下一步动作

可先用简单方式实现：

1. prompt + LLM 输出结构化 JSON
2. JSON 中只允许：
   - `final_answer`
   - `tool_call`

#### `executor.py`

负责：

- 控制执行循环
- 调用具体工具
- 聚合工具结果
- 控制最大步数
- 输出中间事件

#### `tools/base.py`

定义工具接口：

```python
class BaseTool:
    name: str
    description: str

    async def run(self, **kwargs) -> dict:
        ...
```

#### `tools/kb_tools.py`

封装本地知识库工具：

- `search_kb`

#### `tools/java_api_tools.py`

封装 Java 后端业务接口工具：

- `get_document_detail`
- `list_ingest_tasks`
- `get_ingest_task_detail`
- `list_documents_by_category`

---

## 7. Java 与 Python 的接口协作方案

## 7.1 推荐原则

尽量保持 Java 作为主系统边界，不让 Python 直接操作数据库业务表。

推荐做法：

- Python 通过 HTTP 调 Java 的只读业务接口
- Java 控制权限、数据范围、字段输出

### 7.2 Java 侧建议新增只读 Agent 工具接口

建议在 Java 侧新增一组内部用途接口，例如：

- `GET /api/agent/tools/document/{documentId}`
- `GET /api/agent/tools/ingest-tasks`
- `GET /api/agent/tools/ingest-task/{taskId}`
- `GET /api/agent/tools/documents/by-category`
- `GET /api/agent/tools/kb-mapping`

要求：

1. 统一只读
2. 字段精简
3. 返回结构稳定
4. 可由 Python 使用 API Key 或内部鉴权访问

### 7.3 为什么不让 Python 直连 Java 数据库

不推荐 Python 直连 Java 数据库，原因：

1. 业务规则会分散
2. 权限难以统一
3. 表结构耦合太深
4. 后续维护成本高

---

## 8. Agent 主循环设计

推荐第一版采用简单可靠的有限步循环。

### 8.1 执行流程

1. 接收用户问题
2. 构造上下文：
   - 当前问题
   - 会话上下文摘要
   - 可用工具定义
3. 调用 Planner 判断下一步
4. 如果返回 `tool_call`
   - 执行工具
   - 保存工具结果
   - 继续下一轮
5. 如果返回 `final_answer`
   - 输出最终结果
   - 结束
6. 超过最大步数则兜底结束

### 8.2 最大步数建议

- 默认 `max_steps = 4`

这样既能体现 Agent 能力，又能控制复杂度。

### 8.3 失败兜底

如果工具失败：

1. 记录错误
2. 允许模型基于已获得信息继续回答
3. 不要整个对话直接崩溃

---

## 9. SSE 事件设计建议

为了让前端能感知 Agent 过程，建议扩展 SSE 事件类型。

### 9.1 推荐事件类型

- `start`
- `step`
- `tool_call`
- `tool_result`
- `token`
- `sources`
- `error`
- `done`

### 9.2 事件示例

#### step

```json
{
  "type": "step",
  "data": {
    "step": 1,
    "message": "正在分析问题并选择工具"
  }
}
```

#### tool_call

```json
{
  "type": "tool_call",
  "data": {
    "tool": "list_ingest_tasks",
    "args": {
      "status": "failed"
    }
  }
}
```

#### tool_result

```json
{
  "type": "tool_result",
  "data": {
    "tool": "list_ingest_tasks",
    "summary": "共找到 3 个失败导入任务"
  }
}
```

### 9.3 前端兼容建议

前端第一版可以不重做 UI，只需：

1. 忽略不认识的事件不报错
2. 在消息区域增加“执行轨迹”折叠面板
3. 继续兼容 `token` / `done`

---

## 10. 升级实施阶段

建议分 4 个阶段推进。

### 阶段一：最小闭环

目标：

- 新增 Agent Stream 接口
- 支持 `search_kb`
- 支持 `list_ingest_tasks`
- 跑通工具调用 + 最终回答

交付标准：

- 能回答普通知识问题
- 能回答“最近有哪些失败导入任务”这类问题

### 阶段二：补齐只读工具

目标：

- 接入文档详情
- 接入分类文档列表
- 接入 KB 映射信息

交付标准：

- 能做跨文档、跨任务、跨分类的综合问答

### 阶段三：前端轨迹可视化

目标：

- 展示 step / tool_call / tool_result
- 提升 Agent 感知

交付标准：

- 用户能看到 Agent 在做什么

### 阶段四：谨慎开放操作型工具

目标：

- 逐步开放重试导入任务等操作

交付标准：

- 有权限校验
- 有二次确认
- 有日志和审计

---

## 11. 风险点与应对

### 风险 1：Agent 乱调用工具

应对：

- 工具清单控制在 4 到 6 个
- 明确工具说明
- 限制参数结构

### 风险 2：无限循环

应对：

- 限制最大步数
- 连续失败直接结束

### 风险 3：Java 业务接口不稳定

应对：

- 先做只读接口
- 保持返回结构简单稳定

### 风险 4：前端不兼容新事件

应对：

- 前端先忽略新事件
- 逐步接入轨迹展示

### 风险 5：与现有 AI 问答逻辑耦合过深

应对：

- 新增独立 Agent 路径
- 不要直接改坏原有 `/api/chat/stream`

---

## 12. 推荐第一版落地方案

如果只做第一版，建议范围如下：

### Python 端

新增：

- `/api/chat/agent/stream`
- `agent/planner.py`
- `agent/executor.py`
- `agent/tools/base.py`
- `agent/tools/kb_tools.py`
- `agent/tools/java_api_tools.py`

先支持工具：

- `search_kb`
- `list_ingest_tasks`
- `get_ingest_task_detail`

### Java 端

新增只读接口：

- 导入任务列表查询
- 导入任务详情查询
- 文档详情查询

### 前端

先不重构页面，只做：

- 新增“Agent 模式”开关
- SSE 兼容 step/tool_call/tool_result
- 消息区域显示简版执行轨迹

---

## 13. 面试表达建议

后续如果这套能力做出来，建议这样表达：

### 项目定位

“我把原有的 RAG 问答系统升级成了一个面向知识库管理场景的轻量 Agent。”

### 技术亮点

1. 保留原有 RAG 链路，不推翻重做
2. 在 Python 侧增加工具选择和多步执行
3. 通过 Java 提供业务工具接口，保证权限和业务边界
4. 支持执行轨迹输出，提升用户可感知性

### 工程亮点

1. 没有盲目引入复杂 Agent 框架
2. 从业务价值出发控制能力边界
3. 先做只读工具，降低风险
4. 保证老的 AI 问答链路可继续使用

---

## 14. 结论

最适合当前项目的轻量 Agent 升级路径不是“彻底重写”，而是：

`现有 RAG 问答 -> 工具增强问答 -> 轻量多步 Agent -> 知识库业务助手`

核心原则：

1. Python 负责轻量 Agent 编排与 RAG
2. Java 负责业务工具与权限边界
3. 前端负责轨迹展示与交互增强
4. 第一版只做少量高价值、低风险工具

这样最容易快速落地，也最符合当前项目阶段。

