## Main (2026-05-14T01:23:41.174Z)

好，我已经看了网站上所有专题的知识点目录，现在帮你做一个完整的**项目需求 → 知识点映射**。

---

## 📚 你需要学习的技术 & 对应网站知识点

### 一、RAG 核心链路（最重要，占比 60%）

这部分是你项目的**核心中的核心**，网站的「RAG 面试专题」基本全覆盖。

| 你的项目要做的事 | 对应题目 | 学到什么程度 | 优先级 |
|----------------|---------|------------|--------|
| 理解 RAG 完整工作流 | [题1](/ai/rag/1_whatisrag.html) 完整 RAG 系统工作流程 | **必须透彻**，你的项目就是 RAG 系统 | 🔴 P0 |
| RAG 解决什么问题 | [题2](/ai/rag/2_rag_problems.html) | 理解即可，面试用 | 蓝 P1 |
| RAG vs 微调 | [题3](/ai/rag/3_rag_vs_finetune.html) | 理解，面试高频 | 蓝 P1 |
| 文档切割策略（Chunking） | [题4](/ai/rag/4_chunking.html) | **必须透彻**，你的项目要用 RecursiveCharacterTextSplitter + 调参 | 🔴 P0 |
| 语义被切割的问题 | [题5](/ai/rag/5_semantic_cuts.html) | **必须透彻**，重叠窗口策略 | 🔴 P0 |
| Embedding 是什么、怎么选 | [题6](/ai/rag/6_embedding.html) | **必须透彻**，你要从 OllamaEmbedding 换到 BGE-M3 | 🔴 P0 |
| Embedding 算法原理 | [题7](/ai/rag/7_embedding_algos.html) | 了解即可 | 蓝 P2 |
| 向量数据库选型对比 | [题8](/ai/rag/8_vectordb.html) | **必须透彻**，你要知道为什么选 ChromaDB 不用 Milvus | 🔴 P0 |
| 向量数据库实践 | [题9](/ai/rag/9_vectordb_practice.html) | **必须透彻**，ChromaDB 实际使用 | 🔴 P0 |
| RAG 在线查询工作流 | [题10](/ai/rag/10_online_workflow.html) | **必须透彻**，你的问答接口就是这个流程 | 🔴 P0 |
| 向量检索 vs 关键词检索 | [题11](/ai/rag/11_retrieval_types.html) | **透彻**，为后续混合检索做准备 | 🔴 P0 |
| Query Rewrite | [题12](/ai/rag/12_query_rewrite.html) | 了解，Phase 2+ 可选 | 🟡 P2 |
| 多路召回 | [题13](/ai/rag/13_multi_retrieval.html) | 了解，Phase 2+ 可选 | 🟡 P2 |
| 检索优化策略 | [题14](/ai/rag/14_retrieval_opt.html) | **透彻**，你的 Rerank 就是优化 | 🔴 P0 |
| 复杂 RAG 范式 | [题15](/ai/rag/15_advanced_paradigms.html) | 了解即可，面试加分 | 蓝 P2 |
| 图数据库增强 | [题16](/ai/rag/16_graph_db.html) | 不需要，项目不涉及 | ⚪ 跳过 |
| 规避幻觉 | [题17](/ai/rag/17_hallucination.html) | **透彻**，你的 Prompt 护栏就是这个 | 🔴 P0 |
| 量化 RAG 效果 | [题18](/ai/rag/18_evaluation.html) | 了解，面试加分 | 蓝 P2 |
| 知识库动态更新 | [题19](/ai/rag/19_dynamic_update.html) | **透彻**，你的文档入库/删除/版本管理就是这个 | 🔴 P0 |
| RAG 落地最难的地方 | [题20](/ai/rag/20_hardest_parts.html) | **透彻**，面试必问开放题 | 🔴 P0 |

---

### 二、Prompt 工程 & LLM 基础（占比 20%）

你虽然不训练模型，但**调用 LLM + 写 Prompt 是日常活**。

| 你的项目要做的事 | 对应题目 | 学到什么程度 | 优先级 |
|----------------|---------|------------|--------|
| 写好 RAG Prompt 模板 | [大模型工程-题16](/ai/llm/prompt_engineering.html) | **必须透彻**，你的 Prompt 模板直接写在这里 | 🔴 P0 |
| CoT（思维链） | [大模型工程-题17](/ai/llm/cot.html) | 了解，RAG 中可借鉴 | 🟡 P1 |
| 幻觉 & 缓解 | [大模型工程-题18](/ai/llm/hallucination.html) | **透彻**，和 RAG 题17 呼应 | 🔴 P0 |
| 模型选型理由 | [大模型工程-题22](/ai/llm/model_selection.html) | **透彻**，面试必问「为什么选 DeepSeek 不选别的」 | 🔴 P0 |
| 解码策略 | [大模型工程-题12](/ai/llm/decoding_strategies.html) | 了解 | 🟡 P2 |
| 温度/Top-P/Top-K | [大模型工程-题13](/ai/llm/temperature_top_p_top_k.html) | **透彻**，你调 `temperature=0.3` 需要知道为什么 | 🔴 P0 |
| Transformer 架构 | [大模型工程-题2](/ai/llm/transformer_architecture.html) | 了解即可 | 蓝 P2 |
| KV Cache | [大模型工程-题14](/ai/llm/kv_cache_prompt_caching.html) | 了解 | 🟡 P2 |

**大模型工程专题中可以直接跳过的（你的项目不碰训练和部署）：**
- 题1（LLM 概念）、题3（MHA/GQA）、题4（位置编码）、题5（分词器）——了解即可
- 题6~11（训练、Scaling Law、LoRA、RLHF/DPO）——**全部跳过**，你不做训练
- 题15（量化）——跳过
- 题19（MoE）——了解
- 题20（部署框架）——了解
- 题21（评测指标）——了解

---

### 三、工具调用 & 接口设计（占比 10%）

你写的是 REST API 接口 + API Key 鉴权 + 异步任务，这部分不需要 MCP/Agent，但基础概念要了解。

| 你的项目要做的事 | 对应题目 | 学到什么程度 | 优先级 |
|----------------|---------|------------|--------|
| Function Calling 原理 | [工具调用-题1](/ai/tools/1_function_calling.html) | 了解，面试高频 | 🟡 P1 |
| MCP 协议 | [工具调用-题4~5](/ai/tools/4_what_is_mcp.html) | 了解即可 | 🟡 P2 |
| FC vs MCP 对比 | [工具调用-题6~7](/ai/tools/6_mcp_vs_fc.html) | 了解，面试加分 | 🟡 P2 |
| Agent Skill | [工具调用-题9~11](/ai/tools/9_skill.html) | 了解 | 🟡 P2 |
| SSE vs WebSocket | [工具调用-题14](/ai/tools/14_sse_vs_websocket.html) | **透彻**，你原来有 SSE 流式，Java 端要用 | 🔴 P0 |
| LLM 网关 | [工具调用-题16](/ai/tools/16_llm_gateway.html) | 了解 | 🟡 P2 |

---

### 四、Agent（占比 10%，后续扩展用）

Phase 4 你才涉及 Agent，但基础概念要提前了解。

| 你的项目要做的事 | 对应题目 | 学到什么程度 | 优先级 |
|----------------|---------|------------|--------|
| Agent 概念 | [Agent-题1](/ai/agent/1_whatisagent.html) | **透彻**，面试官会问 | 🔴 P0 |
| Agent 架构组件 | [Agent-题2](/ai/agent/2_components.html) | **透彻** | 🔴 P0 |
| Workflow/Agent/Tools 区别 | [Agent-题3](/ai/agent/3_workflow_tools.html) | **透彻** | 🔴 P0 |
| ReAct 范式 | [Agent-题5](/ai/agent/5_react.html) | 了解 | 🟡 P2 |
| ReAct/Plan-and-Execute/Reflection | [Agent-题6](/ai/agent/6_three_patterns.html) | 了解 | 🟡 P2 |
| 记忆机制 | [Agent-题8](/ai/agent/8_memory.html) | 了解，你项目有历史消息管理 | 🟡 P1 |
| Multi-Agent | [Agent-题10](/ai/agent/10_multiagent.html) | 了解 | 🟡 P2 |

---

## 📋 学习路线图（按时间排序）

### 第一遍：RAG 专题（3-5 天，精读）

**顺序：** 题1 → 题4 → 题5 → 题6 → 题8 → 题9 → 题10 → 题11 → 题14 → 题17 → 题19 → 题20

**标准：** 每一题要能**用自己的话讲清楚**，不是背答案。做到：
- 能画出完整 RAG 流程图（离线索引构建 + 在线查询）
- 能说出你项目里每个环节对应 RAG 的哪个阶段
- 能解释为什么选 BGE-M3 不选 Ollama Embedding
- 能说出 ChromaDB 适合什么场景、不适合什么场景

### 第二遍：LLM 工程专题（2 天，挑着读）

**重点读：** 题13（温度参数）、题16（Prompt 工程）、题18（幻觉）、题22（模型选型）

**标准：** 能解释你项目里 `temperature=0.3` 的原因，能说出 Prompt 模板里每句话的作用。

### 第三遍：Agent + 工具调用（2 天，泛读）

**重点读：** Agent 题1、题2、题3 | 工具调用 题1、题14

**标准：** 面试能答「什么是 Agent、你项目里有没有用到 Agent」。

### 第四遍：RAG 专题第二轮（项目做完后再读）

做完项目后回来看题12（Query Rewrite）、题13（多路召回）、题15（复杂 RAG 范式），把实际经验套上去。

---

## 🔴 你必须搞懂的 Top 8 知识点

| 排名 | 知识点 | 对应题目 | 为什么必须 |
|------|--------|---------|-----------|
| 1 | RAG 完整工作流 | RAG-题1、题10 | 你的项目就是 RAG 系统 |
| 2 | 文档 Chunking 策略 | RAG-题4、题5 | 你要调 chunk_size/overlap 参数 |
| 3 | Embedding 选择 | RAG-题6 | 你换了 BGE-M3，面试必问为什么 |
| 4 | 向量数据库 | RAG-题8、题9 | ChromaDB 原理 + 使用 |
| 5 | Prompt 工程 | LLM-题16 | 你要手写 Prompt 模板 |
| 6 | 幻觉规避 | RAG-题17 + LLM-题18 | 你的 Prompt 护栏就是这个 |
| 7 | 检索优化（Rerank） | RAG-题14 | Phase 2 要做 Rerank |
| 8 | 模型选型理由 | LLM-题22 | 为什么选 DeepSeek |

**把这 8 个搞透，其他当面试题看一遍就行。** 项目做完了回头看第二轮，会有完全不同的体会。