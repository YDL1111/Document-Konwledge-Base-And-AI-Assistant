"""
Agent Planner - 基于 LLM 决定下一步动作：tool_call 或 final_answer
"""
import json
import re
from typing import Dict, List, Optional, Tuple

from langchain_openai import ChatOpenAI

from app.agent.prompts import AGENT_SYSTEM_PROMPT
from app.agent.tools.base import BaseTool
from app.core.config import settings


class AgentPlanner:
    """使用 LLM 规划 Agent 下一步动作"""

    def __init__(self, tools: List[BaseTool]):
        self._tools = tools
        self._tool_descriptions = self._build_tool_descriptions()

    def _build_tool_descriptions(self) -> str:
        lines = []
        for tool in self._tools:
            schema = tool.parameters_schema()
            props = schema.get("properties", {})
            required = schema.get("required", [])
            param_descs = []
            for name, prop in props.items():
                req_mark = "（必填）" if name in required else "（可选）"
                param_descs.append(f"    - {name}{req_mark}: {prop.get('description', '')}")
            params_text = "\n".join(param_descs) if param_descs else "    无参数"
            lines.append(f"- **{tool.name}**: {tool.description}\n  参数：\n{params_text}")
        return "\n".join(lines)

    def _get_llm(self) -> ChatOpenAI:
        return ChatOpenAI(
            api_key=settings.DEEPSEEK_API_KEY,
            base_url=settings.DEEPSEEK_BASE_URL,
            model=settings.DEEPSEEK_MODEL,
            streaming=False,
            temperature=0.1,
            max_tokens=1024,
        )

    def _format_history(self, history: Optional[List[Dict[str, str]]]) -> str:
        if not history:
            return "（无历史对话）"
        lines = []
        for msg in history[-6:]:
            role = "用户" if msg["role"] == "user" else "助手"
            content = msg.get("content", "")[:300]
            lines.append(f"{role}: {content}")
        return "\n".join(lines)

    async def decide(
        self,
        question: str,
        history: Optional[List[Dict[str, str]]] = None,
        tool_results: Optional[List[Dict]] = None,
    ) -> Tuple[str, Optional[str], Optional[dict]]:
        """决定下一步动作

        Returns:
            (action, tool_name_or_None, args_or_None)
            action: "tool_call" 或 "final_answer"
        """
        prompt = AGENT_SYSTEM_PROMPT.format(
            tool_descriptions=self._tool_descriptions,
            history=self._format_history(history),
            question=question,
        )

        # 如果有工具执行结果，追加到 prompt
        if tool_results:
            results_text = "\n".join(
                f"[工具 {r['tool']} 执行{'成功' if r.get('success') else '失败'}]: {r.get('summary', '')}"
                for r in tool_results[-3:]  # 只保留最近3个结果
            )
            prompt += f"\n\n## 最近工具执行结果\n{results_text}\n\n请根据工具结果继续下一步。记住以 JSON 格式回复。"

        llm = self._get_llm()
        response = await llm.ainvoke(prompt)
        content = response.content if hasattr(response, "content") else str(response)

        return self._parse_response(content)

    def _parse_response(self, content: str) -> Tuple[str, Optional[str], Optional[dict]]:
        """解析 LLM 返回的 JSON"""
        # 尝试提取 JSON
        content = content.strip()

        # 去掉可能的 markdown 代码块包装
        if content.startswith("```"):
            parts = content.split("\n", 1)
            if len(parts) > 1:
                content = parts[1]
            if content.endswith("```"):
                content = content[:-3]
            content = content.strip()

        # 尝试找到 JSON 对象
        json_match = re.search(r'\{.*\}', content, re.DOTALL)
        if json_match:
            content = json_match.group(0)

        try:
            data = json.loads(content)
        except json.JSONDecodeError:
            # 如果解析失败，默认当作 final_answer
            return ("final_answer", None, {"answer": content})

        action = data.get("action", "final_answer")

        if action == "tool_call":
            tool_name = data.get("tool", "")
            args = data.get("args", {})
            if not tool_name:
                return ("final_answer", None, {"answer": "无法确定需要调用哪个工具。"})
            return ("tool_call", tool_name, args)

        # final_answer 或其他
        answer = data.get("answer", content)
        return ("final_answer", None, {"answer": answer})
