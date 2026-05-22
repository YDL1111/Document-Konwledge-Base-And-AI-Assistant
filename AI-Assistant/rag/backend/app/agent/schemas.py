"""
Agent 运行时数据结构
"""
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional


@dataclass
class ToolCall:
    tool: str
    args: Dict[str, Any] = field(default_factory=dict)


@dataclass
class ToolResult:
    tool: str
    success: bool
    summary: str = ""
    data: Any = None
    error: Optional[str] = None


@dataclass
class AgentStep:
    step: int
    message: str
    tool_call: Optional[ToolCall] = None
    tool_result: Optional[ToolResult] = None


@dataclass
class AgentRequest:
    kb_id: int
    question: str
    conv_id: Optional[int] = None
    history: Optional[List[Dict[str, str]]] = None
    max_steps: int = 4


@dataclass
class AgentFinalAnswer:
    answer: str
    sources: List[Dict[str, Any]] = field(default_factory=list)
    steps: List[AgentStep] = field(default_factory=list)
