"""
Agent Executor - 控制执行循环、调用工具、聚合结果、输出 SSE 事件
"""
import json
import traceback
from typing import Any, AsyncGenerator, Dict, List, Optional

from langchain_core.messages import SystemMessage
from langchain_openai import ChatOpenAI

from app.agent.planner import AgentPlanner
from app.agent.schemas import AgentRequest, AgentStep, ToolCall, ToolResult
from app.agent.tools.base import BaseTool
from app.core.config import settings
from loguru import logger


# 工具结果汇总后生成最终回答的系统提示（对齐标准模式的 SYSTEM_PROMPT 风格）
FINAL_ANSWER_PROMPT = """你是企业内部知识库助手，请根据以下参考文档回答用户问题。

重要规则：
1. 参考文档来自向量检索，可能与问题部分相关，请仔细阅读后作答。
2. 如果文档标题与问题不完全一致，但内容相关，也应提取并回答。
3. 只有在参考文档确实没有相关内容时，才回复"未找到相关信息"。
4. 引用内容时标注来源，例如：【来源：文件名】。
5. 回答要清晰、专业，并使用中文。
6. 如果信息充足，请尽量全面、详细地回答，逐条列出相关内容。

参考文档：
{context}

## 工具查询结果
{tool_results}

## 用户问题
{question}
"""


class AgentExecutor:
    """轻量 Agent 执行器"""

    def __init__(self, tools: List[BaseTool]):
        self._planner = AgentPlanner(tools)
        self._tools: Dict[str, BaseTool] = {t.name: t for t in tools}
        self._max_steps = 4

    def _get_llm(self, streaming: bool = False) -> ChatOpenAI:
        return ChatOpenAI(
            api_key=settings.DEEPSEEK_API_KEY,
            base_url=settings.DEEPSEEK_BASE_URL,
            model=settings.DEEPSEEK_MODEL,
            streaming=streaming,
            temperature=0.3,
            max_tokens=2048,
        )

    async def run_stream(
        self, request: AgentRequest
    ) -> AsyncGenerator[str, None]:
        """执行 Agent 并以 SSE 流式输出"""
        max_steps = min(request.max_steps, 5)
        steps: List[AgentStep] = []
        tool_results_for_context: List[Dict] = []
        kb_context = ""
        kb_sources: List[Dict] = []

        # 发送开始事件
        yield self._sse("start", {"message": "Agent 开始分析问题", "max_steps": max_steps})

        for step_num in range(1, max_steps + 1):
            # 1. 发送 step 事件
            yield self._sse("step", {"step": step_num, "message": f"正在分析第 {step_num} 步"})

            # 2. Planner 决策
            action, tool_name, args = await self._planner.decide(
                question=request.question,
                history=request.history,
                tool_results=tool_results_for_context,
            )

            if action == "final_answer" or tool_name is None:
                # Planner 决定直接回答
                yield self._sse("step", {"step": step_num, "message": "正在生成最终回答"})
                break

            # 3. 执行工具调用
            tool = self._tools.get(tool_name)
            if tool is None:
                logger.warning(f"Unknown tool: {tool_name}")
                tool_results_for_context.append({
                    "tool": tool_name,
                    "success": False,
                    "summary": f"未知工具: {tool_name}",
                })
                yield self._sse("error", {"message": f"未知工具: {tool_name}"})
                continue

            # 发送 tool_call 事件
            tool_call = ToolCall(tool=tool_name, args=args or {})
            yield self._sse("tool_call", {"tool": tool_name, "args": args or {}})

            # 执行工具（注入 kb_id）
            if args is None:
                args = {}
            if "kb_id" not in args:
                args["kb_id"] = request.kb_id
            # 兜底：search_kb 的 question 为空时，用原始用户问题填充
            if tool_name == "search_kb" and not args.get("question", "").strip():
                args["question"] = request.question

            try:
                result = await tool.run(**args)
            except Exception as e:
                result = {"success": False, "summary": str(e), "data": None, "error": str(e)}

            # 发送 tool_result 事件
            tool_result = ToolResult(
                tool=tool_name,
                success=result.get("success", False),
                summary=result.get("summary", ""),
                data=result.get("data"),
                error=result.get("error"),
            )
            yield self._sse("tool_result", {
                "tool": tool_name,
                "success": tool_result.success,
                "summary": tool_result.summary,
            })

            # 记录步骤
            steps.append(AgentStep(step=step_num, message=f"调用工具 {tool_name}", tool_call=tool_call, tool_result=tool_result))
            tool_results_for_context.append({
                "tool": tool_name,
                "success": tool_result.success,
                "summary": tool_result.summary,
                "data": result.get("data"),
            })

            # 如果是 search_kb 且成功，直接使用 _build_context 生成的完整上下文
            if tool_name == "search_kb" and tool_result.success and result.get("data"):
                kb_context = result["data"].get("context", "")
                kb_sources = result["data"].get("sources", [])

        # 4. 生成最终回答
        yield self._sse("step", {"step": "final", "message": "正在汇总结果并生成最终回答"})

        final_answer = ""
        try:
            async for sse_chunk in self._generate_final_answer(
                question=request.question,
                kb_context=kb_context,
                kb_sources=kb_sources,
                tool_results=tool_results_for_context,
            ):
                yield sse_chunk
                # 提取答案文本
                try:
                    data = json.loads(sse_chunk.replace("data: ", "").strip())
                    if data.get("type") == "token":
                        final_answer += data.get("data", "")
                except Exception:
                    pass
        except Exception as e:
            logger.error(f"Final answer generation failed: {type(e).__name__}: {e}\n{traceback.format_exc()}")
            yield self._sse("error", {"message": str(e)})

        # 5. 发送完成事件
        yield self._sse("done", {
            "answer": final_answer,
            "sources": kb_sources,
            "steps": [
                {
                    "step": s.step,
                    "message": s.message,
                    "tool_call": {"tool": s.tool_call.tool, "args": s.tool_call.args} if s.tool_call else None,
                    "tool_result": {
                        "tool": s.tool_result.tool,
                        "success": s.tool_result.success,
                        "summary": s.tool_result.summary,
                    } if s.tool_result else None,
                }
                for s in steps
            ],
        })

    async def _generate_final_answer(
        self,
        question: str,
        kb_context: str,
        kb_sources: List[Dict],
        tool_results: List[Dict],
    ) -> AsyncGenerator[str, None]:
        """流式生成最终汇总回答，失败时降级为非流式"""
        # 格式化工具结果
        tool_results_text = "（无工具查询结果）"
        if tool_results:
            lines = []
            for r in tool_results:
                status = "成功" if r.get("success") else "失败"
                lines.append(f"- [{r['tool']}] {status}: {r.get('summary', '')}")
            tool_results_text = "\n".join(lines)

        # 构建系统提示
        # kb_context 已在 SearchKBTool 中通过 _build_context 按 MAX_CONTEXT_LENGTH 截断
        system_text = FINAL_ANSWER_PROMPT.format(
            context=kb_context or "（未检索知识库）",
            tool_results=tool_results_text,
            question=question,
        )

        if not kb_context:
            system_text = f"""你是企业内部知识库助手。请根据以下信息回答用户问题。

## 工具查询结果
{tool_results_text}

## 用户问题
{question}

请基于工具查询结果给出清晰、专业的回答。使用中文。"""

        messages = [
            SystemMessage(content=system_text),
        ]

        # 尝试流式生成，失败降级为非流式
        try:
            llm = self._get_llm(streaming=True)
            async for chunk in llm.astream(messages):
                if not chunk:
                    continue
                content = chunk.content if hasattr(chunk, "content") else str(chunk)
                if content:
                    yield self._sse("token", content)
        except Exception as e:
            logger.warning(f"Streaming final answer failed ({type(e).__name__}: {e}), falling back to non-streaming")
            try:
                llm = self._get_llm(streaming=False)
                response = await llm.ainvoke(messages)
                content = response.content if hasattr(response, "content") else str(response)
                if content:
                    yield self._sse("token", content)
            except Exception as e2:
                logger.error(f"Non-streaming fallback also failed: {type(e2).__name__}: {e2}\n{traceback.format_exc()}")
                yield self._sse("token", f"抱歉，生成回答时遇到错误: {e2}")

        # 如果有来源，再发一次 sources
        if kb_sources:
            yield self._sse("sources", kb_sources)

    @staticmethod
    def _sse(event_type: str, data: Any) -> str:
        return f"data: {json.dumps({'type': event_type, 'data': data}, ensure_ascii=False)}\n\n"
