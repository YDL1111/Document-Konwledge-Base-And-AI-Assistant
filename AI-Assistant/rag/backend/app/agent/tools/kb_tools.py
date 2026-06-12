"""
Knowledge-base retrieval tool used by Agent.
"""

from typing import Any, Dict, List, Optional

from app.agent.tools.base import BaseTool
from app.agent.tools.java_api_tools import _java_get
from app.core.config import settings
from app.services.rag import rag_service


def _normalize_doc_ids(
    document_id: Optional[int], visible_doc_ids: Optional[List[int]]
) -> Optional[List[int]]:
    if document_id is not None:
        return [int(document_id)]
    return visible_doc_ids


class SearchKBTool(BaseTool):
    name = "search_kb"
    description = (
        "Search the current knowledge base and return the most relevant chunks with source information. "
        "Supports document-scoped retrieval for administrator troubleshooting and answer grounding."
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "question": {
                    "type": "string",
                    "description": "The original user question used for knowledge retrieval.",
                },
                "document_id": {
                    "type": "integer",
                    "description": "Optional business document ID. If provided, retrieval will be constrained to that document.",
                },
                "top_k": {
                    "type": "integer",
                    "description": "Optional retrieval count override. Defaults to standard or document-focused project settings.",
                },
            },
            "required": ["question"],
        }

    async def _resolve_python_doc_ids(
        self, kb_id: int, document_id: Optional[int]
    ) -> Optional[List[int]]:
        if document_id is None:
            return None

        page = await _java_get(
            "/ai/chat/agent/tools/ingest-tasks",
            {"pageNum": 1, "pageSize": 20, "documentId": int(document_id)},
        )
        rows = page.get("rows", []) if isinstance(page, dict) else []
        python_doc_ids: List[int] = []
        for row in rows:
            if row.get("status") != 3:
                continue
            if kb_id is not None and row.get("pythonKbId") not in (None, kb_id):
                continue
            python_doc_id = row.get("pythonDocId")
            if python_doc_id is not None and python_doc_id not in python_doc_ids:
                python_doc_ids.append(int(python_doc_id))
        return python_doc_ids

    async def run(self, **kwargs) -> Dict[str, Any]:
        kb_id = kwargs.get("kb_id", 1)
        question = (kwargs.get("question") or "").strip()
        document_id = kwargs.get("document_id")
        visible_doc_ids = kwargs.get("visible_doc_ids")
        requested_top_k = kwargs.get("top_k")

        if not question:
            return {
                "success": False,
                "summary": "question is required for search_kb",
                "data": None,
                "error": "empty question",
            }

        try:
            resolved_python_doc_ids = await self._resolve_python_doc_ids(
                kb_id=kb_id, document_id=document_id
            )
            scoped_doc_ids = (
                resolved_python_doc_ids
                if document_id is not None
                else _normalize_doc_ids(document_id, visible_doc_ids)
            )
            doc_focused = scoped_doc_ids is not None and len(scoped_doc_ids) == 1
            top_k = (
                min(max(int(requested_top_k), 1), 20)
                if requested_top_k is not None
                else (
                    settings.DOC_FOCUSED_TOP_K if doc_focused else settings.TOP_K
                )
            )

            results, cache_hit = await rag_service.retrieve(
                kb_id=kb_id,
                query=question,
                k=top_k,
                strategy="similarity" if doc_focused else "hybrid",
                visible_doc_ids=scoped_doc_ids,
            )
            context, sources = rag_service._build_context(results, scoped_doc_ids)

            chunks: List[Dict[str, Any]] = []
            hit_doc_ids = []
            for doc, score in results[: min(len(results), settings.RERANK_TOP_K if not doc_focused else settings.DOC_FOCUSED_RERANK_TOP_K)]:
                doc_id = doc.metadata.get("doc_id")
                if doc_id is not None and doc_id not in hit_doc_ids:
                    hit_doc_ids.append(doc_id)
                preview = doc.page_content[:320]
                if len(doc.page_content) > 320:
                    preview += "..."
                chunks.append(
                    {
                        "doc_id": doc_id,
                        "filename": doc.metadata.get("filename", "unknown"),
                        "page": doc.metadata.get("page"),
                        "score": round(float(score), 4),
                        "chunk_index": doc.metadata.get("chunk_index"),
                        "preview": preview,
                    }
                )

            if results:
                summary = (
                    f"Retrieved {len(results)} chunks from kb {kb_id}. "
                    f"Top hit docs: {', '.join(str(doc_id) for doc_id in hit_doc_ids[:5]) or 'unknown'}."
                )
                if doc_focused and document_id is not None:
                    summary += (
                        f" Retrieval was constrained to business document {document_id}"
                        f" -> python doc {scoped_doc_ids[0]}."
                    )
                if cache_hit:
                    summary += " Result came from cache."
            else:
                summary = "No relevant chunks were found in the knowledge base."
                if doc_focused and document_id is not None:
                    summary += (
                        f" Scoped business document: {document_id}"
                        + (
                            f" -> python doc {scoped_doc_ids[0]}."
                            if scoped_doc_ids
                            else "."
                        )
                    )

            return {
                "success": True,
                "summary": summary,
                "data": {
                    "context": context,
                    "chunks": chunks,
                    "sources": sources,
                    "cache_hit": cache_hit,
                    "kb_id": kb_id,
                    "document_scoped": doc_focused,
                    "business_document_id": document_id,
                    "scoped_doc_ids": scoped_doc_ids,
                    "hit_doc_ids": hit_doc_ids,
                },
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Knowledge-base search failed: {exc}",
                "data": None,
                "error": str(exc),
            }
