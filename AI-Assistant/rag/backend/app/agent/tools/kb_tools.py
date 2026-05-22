"""
知识库工具 - search_kb
"""
import json
from typing import Any, Dict

from app.agent.tools.base import BaseTool
from app.services.rag import rag_service


class SearchKBTool(BaseTool):
    name = "search_kb"
    description = (
        "搜索当前知识库，获取与问题相关的文档片段。"
        "适用于需要从知识库中查找具体信息的问题。"
        "返回相关文档片段及其来源信息。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "question": {
                    "type": "string",
                    "description": "要在知识库中搜索的问题或关键词",
                },
                "top_k": {
                    "type": "integer",
                    "description": "返回结果数量，默认5",
                },
            },
            "required": ["question"],
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        kb_id = kwargs.get("kb_id", 1)
        question = kwargs.get("question", "")
        top_k = kwargs.get("top_k", 5)

        if not question.strip():
            return {
                "success": False,
                "summary": "搜索问题不能为空",
                "data": None,
                "error": "empty question",
            }

        try:
            from app.core.config import settings
            # 与标准模式保持一致：用 TOP_K 检索 + RERANK_TOP_K 重排
            results, cache_hit = await rag_service.retrieve(
                kb_id=kb_id, query=question, k=settings.TOP_K, strategy="hybrid"
            )
            # 复用标准模式的 context 构建，保证内容完整、格式一致
            context, sources = rag_service._build_context(results)

            # chunks 用于执行轨迹展示（预览截断），context 用于 LLM（完整内容）
            chunks = []
            for doc, score in results[:settings.RERANK_TOP_K]:
                preview = doc.page_content[:300]
                if len(doc.page_content) > 300:
                    preview += "..."
                chunks.append({
                    "filename": doc.metadata.get("filename", "未知"),
                    "page": doc.metadata.get("page"),
                    "score": round(float(score), 4),
                    "preview": preview,
                })

            summary = f"共检索到 {len(results)} 个相关片段"
            if not results:
                summary = "未找到相关文档片段"

            return {
                "success": True,
                "summary": summary,
                "data": {
                    "context": context,       # 完整上下文，直接喂给 LLM
                    "chunks": chunks,         # 预览信息，用于轨迹展示
                    "sources": sources,
                    "cache_hit": cache_hit,
                },
                "error": None,
            }
        except Exception as e:
            return {
                "success": False,
                "summary": f"知识库搜索失败: {str(e)}",
                "data": None,
                "error": str(e),
            }
