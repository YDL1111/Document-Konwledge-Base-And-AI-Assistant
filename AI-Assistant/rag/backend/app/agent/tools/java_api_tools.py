"""
Java backend read-only tools for Agent.
"""

from typing import Any, Dict, List, Optional

import httpx
from loguru import logger

from app.agent.tools.base import BaseTool
from app.core.config import settings


JAVA_BASE_URL = getattr(settings, "JAVA_BASE_URL", "http://localhost:8080")
JAVA_API_KEY = getattr(settings, "INTERNAL_API_KEY", "") or getattr(
    settings, "JAVA_API_KEY", ""
)


def _status_text(status: Optional[int], status_map: Dict[int, str]) -> str:
    return status_map.get(status or 0, "unknown")


def _trim(value: Optional[str], length: int = 160) -> str:
    text = (value or "").strip()
    if len(text) <= length:
        return text
    return text[: length - 3] + "..."


def _build_headers() -> Dict[str, str]:
    headers = {"Accept": "application/json"}
    if JAVA_API_KEY:
        headers["X-API-Key"] = JAVA_API_KEY
    return headers


async def _java_get(path: str, params: Optional[dict] = None) -> dict:
    if not JAVA_API_KEY:
        raise RuntimeError(
            "Java Agent tools are unavailable because INTERNAL_API_KEY / JAVA_API_KEY is not configured."
        )

    url = f"{JAVA_BASE_URL.rstrip('/')}{path}"
    try:
        async with httpx.AsyncClient(timeout=15.0, trust_env=False) as client:
            logger.info(f"Agent calling Java GET: {url} params={params}")
            resp = await client.get(url, params=params, headers=_build_headers())
            logger.info(f"Agent Java response: {resp.status_code} url={url}")
            resp.raise_for_status()
            payload = resp.json()
    except httpx.ConnectError as exc:
        logger.error(f"Java service connect error: {url} | {exc}")
        raise RuntimeError(f"Cannot connect to Java backend: {JAVA_BASE_URL}") from exc
    except httpx.TimeoutException as exc:
        logger.error(f"Java service timeout: {url}")
        raise RuntimeError(f"Java backend timeout: {path}") from exc
    except httpx.HTTPStatusError as exc:
        status = exc.response.status_code
        body = exc.response.text[:300]
        logger.error(f"Java HTTP error {status}: {url} body={body}")
        if status in (401, 403):
            raise RuntimeError(
                f"Java backend rejected Agent tool request ({status}). Check API key and admin-only access."
            ) from exc
        if status == 404:
            raise RuntimeError(f"Java Agent endpoint not found: {path}") from exc
        raise RuntimeError(f"Java backend HTTP {status}: {path}") from exc

    if payload.get("code") != 0:
        raise RuntimeError(payload.get("msg") or f"Java backend business error: {path}")

    return payload.get("data")


class ListIngestTasksTool(BaseTool):
    name = "list_ingest_tasks"
    description = (
        "List ingest tasks for administrator troubleshooting. Suitable for questions such as "
        "'which tasks failed recently', 'how many are processing', or 'which document import is abnormal'."
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "status": {
                    "type": "integer",
                    "description": "Optional task status filter: 1=pending, 2=processing, 3=success, 4=failed.",
                },
                "document_id": {
                    "type": "integer",
                    "description": "Optional document ID filter.",
                },
                "page_size": {
                    "type": "integer",
                    "description": "How many tasks to return, default 10 and max 30.",
                },
            },
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        status = kwargs.get("status")
        document_id = kwargs.get("document_id")
        page_size = min(max(int(kwargs.get("page_size", 10)), 1), 30)

        params: Dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if status is not None:
            params["status"] = status
        if document_id is not None:
            params["documentId"] = document_id

        status_map = {1: "pending", 2: "processing", 3: "success", 4: "failed"}

        try:
            page = await _java_get("/ai/chat/agent/tools/ingest-tasks", params)
            rows = page.get("rows", []) if isinstance(page, dict) else []

            tasks: List[Dict[str, Any]] = []
            counts = {1: 0, 2: 0, 3: 0, 4: 0}
            for row in rows:
                task_status = row.get("status")
                if task_status in counts:
                    counts[task_status] += 1
                tasks.append(
                    {
                        "task_id": row.get("taskId"),
                        "task_no": row.get("taskNo"),
                        "document_id": row.get("documentId"),
                        "version_id": row.get("versionId"),
                        "status": task_status,
                        "status_text": _status_text(task_status, status_map),
                        "retry_count": row.get("retryCount"),
                        "chunk_count": row.get("chunkCount"),
                        "python_kb_id": row.get("pythonKbId"),
                        "python_doc_id": row.get("pythonDocId"),
                        "error_message": row.get("errorMessage"),
                        "started_time": row.get("startedTime"),
                        "finished_time": row.get("finishedTime"),
                    }
                )

            stats = ", ".join(
                f"{label}={counts[code]}" for code, label in status_map.items() if counts[code] > 0
            ) or "no task stats"
            detail_lines = [
                (
                    f"[{task['task_no']}] doc={task['document_id']} "
                    f"status={task['status_text']} "
                    f"pythonDoc={task['python_doc_id'] or '-'} "
                    f"error={_trim(task['error_message'], 60) or '-'}"
                )
                for task in tasks[:8]
            ]
            summary = f"Found {page.get('total', 0)} ingest tasks. Status distribution: {stats}."
            if detail_lines:
                summary += " " + " | ".join(detail_lines)

            return {
                "success": True,
                "summary": summary,
                "data": {
                    "total": page.get("total", 0),
                    "tasks": tasks,
                    "status_counts": counts,
                },
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Failed to query ingest tasks: {exc}",
                "data": None,
                "error": str(exc),
            }


class GetIngestTaskDetailTool(BaseTool):
    name = "get_ingest_task_detail"
    description = (
        "Get one ingest task in detail, including Python sync IDs, chunk count, retry count and failure message."
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "task_id": {
                    "type": "integer",
                    "description": "Ingest task ID.",
                }
            },
            "required": ["task_id"],
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        task_id = kwargs.get("task_id")
        if not task_id:
            return {
                "success": False,
                "summary": "task_id is required",
                "data": None,
                "error": "missing task_id",
            }

        status_map = {1: "pending", 2: "processing", 3: "success", 4: "failed"}
        try:
            row = await _java_get(f"/ai/chat/agent/tools/ingest-task/{task_id}")
            data = {
                "task_id": row.get("taskId"),
                "task_no": row.get("taskNo"),
                "document_id": row.get("documentId"),
                "version_id": row.get("versionId"),
                "task_type": row.get("taskType"),
                "status": row.get("status"),
                "status_text": _status_text(row.get("status"), status_map),
                "retry_count": row.get("retryCount"),
                "chunk_count": row.get("chunkCount"),
                "trace_id": row.get("traceId"),
                "python_kb_id": row.get("pythonKbId"),
                "python_doc_id": row.get("pythonDocId"),
                "error_message": row.get("errorMessage"),
                "started_time": row.get("startedTime"),
                "finished_time": row.get("finishedTime"),
            }
            summary = (
                f"Task {data['task_no']} is {data['status_text']}. "
                f"document={data['document_id']}, version={data['version_id']}, "
                f"pythonKb={data['python_kb_id'] or '-'}, pythonDoc={data['python_doc_id'] or '-'}, "
                f"chunkCount={data['chunk_count'] or 0}, retryCount={data['retry_count'] or 0}."
            )
            if data["error_message"]:
                summary += f" Error: {_trim(data['error_message'], 120)}."

            return {
                "success": True,
                "summary": summary,
                "data": data,
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Failed to query ingest task detail: {exc}",
                "data": None,
                "error": str(exc),
            }


class GetDocumentDetailTool(BaseTool):
    name = "get_document_detail"
    description = (
        "Get one business document's metadata, including title, category, status, visibility, version and audit remark."
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "document_id": {
                    "type": "integer",
                    "description": "Business document ID.",
                }
            },
            "required": ["document_id"],
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        document_id = kwargs.get("document_id")
        if not document_id:
            return {
                "success": False,
                "summary": "document_id is required",
                "data": None,
                "error": "missing document_id",
            }

        status_map = {1: "draft", 2: "pending_audit", 3: "published", 4: "rejected", 5: "archived"}
        visibility_map = {1: "public", 2: "department", 3: "private"}

        try:
            row = await _java_get(f"/ai/chat/agent/tools/document/{document_id}")
            data = {
                "document_id": row.get("documentId"),
                "title": row.get("title"),
                "doc_code": row.get("docCode"),
                "category_id": row.get("categoryId"),
                "dept_id": row.get("deptId"),
                "status": row.get("status"),
                "status_text": _status_text(row.get("status"), status_map),
                "visibility": row.get("visibility"),
                "visibility_text": _status_text(row.get("visibility"), visibility_map),
                "current_version_no": row.get("currentVersionNo"),
                "summary": row.get("summary"),
                "tags": row.get("tags"),
                "audit_remark": row.get("auditRemark"),
                "creator_id": row.get("creatorId"),
                "update_time": row.get("updateTime"),
                "has_ai_import": row.get("hasAiImport"),
            }
            summary = (
                f"Document {data['document_id']} '{data['title']}' is {data['status_text']} "
                f"under category {data['category_id']}, visibility={data['visibility_text']}, "
                f"version={data['current_version_no'] or '-'}."
            )
            if data["summary"]:
                summary += f" Summary: {_trim(data['summary'], 100)}."
            if data["audit_remark"]:
                summary += f" Audit remark: {_trim(data['audit_remark'], 80)}."

            return {
                "success": True,
                "summary": summary,
                "data": data,
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Failed to query document detail: {exc}",
                "data": None,
                "error": str(exc),
            }


class ListDocumentsByCategoryTool(BaseTool):
    name = "list_documents_by_category"
    description = (
        "List business documents by category and optional status. Suitable for questions such as "
        "'which published documents are under category 8' or 'what documents can be imported to AI'."
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "category_id": {
                    "type": "integer",
                    "description": "Optional category ID filter.",
                },
                "status": {
                    "type": "integer",
                    "description": "Optional document status filter: 1=draft, 2=pending audit, 3=published, 4=rejected, 5=archived.",
                },
                "page_size": {
                    "type": "integer",
                    "description": "How many documents to return, default 10 and max 30.",
                },
            },
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        category_id = kwargs.get("category_id")
        status = kwargs.get("status")
        page_size = min(max(int(kwargs.get("page_size", 10)), 1), 30)
        params: Dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if category_id is not None:
            params["categoryId"] = category_id
        if status is not None:
            params["status"] = status

        status_map = {1: "draft", 2: "pending_audit", 3: "published", 4: "rejected", 5: "archived"}
        visibility_map = {1: "public", 2: "department", 3: "private"}

        try:
            page = await _java_get("/ai/chat/agent/tools/documents", params)
            rows = page.get("rows", []) if isinstance(page, dict) else []

            documents: List[Dict[str, Any]] = []
            status_counts: Dict[str, int] = {}
            for row in rows:
                status_text = _status_text(row.get("status"), status_map)
                visibility_text = _status_text(row.get("visibility"), visibility_map)
                status_counts[status_text] = status_counts.get(status_text, 0) + 1
                documents.append(
                    {
                        "document_id": row.get("documentId"),
                        "title": row.get("title"),
                        "doc_code": row.get("docCode"),
                        "category_id": row.get("categoryId"),
                        "dept_id": row.get("deptId"),
                        "status": row.get("status"),
                        "status_text": status_text,
                        "visibility": row.get("visibility"),
                        "visibility_text": visibility_text,
                        "current_version_no": row.get("currentVersionNo"),
                        "summary": row.get("summary"),
                        "tags": row.get("tags"),
                        "creator_id": row.get("creatorId"),
                        "update_time": row.get("updateTime"),
                        "has_ai_import": row.get("hasAiImport"),
                    }
                )

            stats = ", ".join(f"{k}={v}" for k, v in status_counts.items()) or "no document stats"
            detail_lines = [
                (
                    f"[{doc['document_id']}] {doc['title']} "
                    f"status={doc['status_text']} visibility={doc['visibility_text']} "
                    f"version={doc['current_version_no'] or '-'} aiImport={doc['has_ai_import']}"
                )
                for doc in documents[:8]
            ]
            summary = f"Found {page.get('total', 0)} documents. Status distribution: {stats}."
            if detail_lines:
                summary += " " + " | ".join(detail_lines)

            return {
                "success": True,
                "summary": summary,
                "data": {
                    "total": page.get("total", 0),
                    "documents": documents,
                    "status_counts": status_counts,
                },
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Failed to query documents: {exc}",
                "data": None,
                "error": str(exc),
            }


class ListCategoriesTool(BaseTool):
    name = "list_categories"
    description = (
        "List direct child categories under one parent category. Suitable for questions such as "
        "'what subcategories are under category 1' or 'show the children of category 8'."
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "parent_id": {
                    "type": "integer",
                    "description": "Parent category ID. Omit or use 0 for root categories.",
                }
            },
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        parent_id = kwargs.get("parent_id", 0)
        params: Dict[str, Any] = {"parentId": parent_id}

        try:
            rows = await _java_get("/ai/chat/agent/tools/categories", params)
            categories: List[Dict[str, Any]] = []
            for row in rows or []:
                categories.append(
                    {
                        "category_id": row.get("categoryId"),
                        "parent_id": row.get("parentId"),
                        "category_name": row.get("categoryName"),
                        "dept_id": row.get("deptId"),
                        "status": row.get("status"),
                        "sort_num": row.get("sortNum"),
                        "remark": row.get("remark"),
                    }
                )

            detail_lines = [
                f"[{item['category_id']}] {item['category_name']} status={item['status']}"
                for item in categories[:10]
            ]
            summary = f"Found {len(categories)} child categories under parent {parent_id}."
            if detail_lines:
                summary += " " + " | ".join(detail_lines)

            return {
                "success": True,
                "summary": summary,
                "data": {
                    "parent_id": parent_id,
                    "categories": categories,
                },
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Failed to query categories: {exc}",
                "data": None,
                "error": str(exc),
            }


class GetKbMappingInfoTool(BaseTool):
    name = "get_kb_mapping_info"
    description = (
        "Get the mapping between Java business categories and Python knowledge-base IDs, including the default kb."
    )

    def parameters_schema(self) -> dict:
        return {"type": "object", "properties": {}}

    async def run(self, **kwargs) -> Dict[str, Any]:
        try:
            data = await _java_get("/ai/chat/agent/tools/kb-mapping")
            default_kb_id = data.get("defaultKbId")
            mappings = data.get("categoryMappings") or {}
            normalized = {str(key): value for key, value in mappings.items()}
            mapping_text = ", ".join(
                f"category {key} -> kb {value}" for key, value in normalized.items()
            ) or "no mappings"

            return {
                "success": True,
                "summary": f"Default kb is {default_kb_id}. Category mappings: {mapping_text}.",
                "data": {
                    "default_kb_id": default_kb_id,
                    "category_mappings": normalized,
                },
                "error": None,
            }
        except Exception as exc:
            return {
                "success": False,
                "summary": f"Failed to query KB mapping: {exc}",
                "data": None,
                "error": str(exc),
            }
