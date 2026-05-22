"""
工具基类
"""
from abc import ABC, abstractmethod
from typing import Any, Dict


class BaseTool(ABC):
    """Agent 工具基类，所有工具需继承此类"""

    name: str = ""
    description: str = ""

    @abstractmethod
    async def run(self, **kwargs) -> Dict[str, Any]:
        """执行工具，返回统一结构:
        {
            "success": bool,
            "summary": str,
            "data": Any,
            "error": str or None,
        }
        """
        ...

    def to_openai_function(self) -> dict:
        """转为 OpenAI function-calling 格式"""
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.parameters_schema(),
            },
        }

    @abstractmethod
    def parameters_schema(self) -> dict:
        """返回 JSON Schema 格式的参数定义"""
        ...
