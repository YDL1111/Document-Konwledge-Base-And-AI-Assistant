"""
Java 后端只读业务接口工具
"""
import json
from typing import Any, Dict, Optional
from urllib.parse import urlencode

import httpx

from app.agent.tools.base import BaseTool
from app.core.config import settings

# Java 后端基础地址（可从配置或环境变量读取）
JAVA_BASE_URL = getattr(settings, "JAVA_BASE_URL", "http://localhost:8080")
JAVA_API_KEY = getattr(settings, "JAVA_API_KEY", "")


async def _java_get(path: str, params: Optional[dict] = None) -> dict:
    """调用 Java 后端 GET 接口"""
    url = f"{JAVA_BASE_URL.rstrip('/')}{path}"
    headers = {"Accept": "application/json"}
    if JAVA_API_KEY:
        headers["X-API-Key"] = JAVA_API_KEY

    from loguru import logger

    try:
        # trust_env=False 防止 Windows 系统代理干扰 localhost 请求
        async with httpx.AsyncClient(timeout=15.0, trust_env=False) as client:
            logger.info(f"Agent 调用 Java: {url} params={params}")
            resp = await client.get(url, params=params, headers=headers)
            logger.info(f"Java 响应: HTTP {resp.status_code}")
            resp.raise_for_status()
            return resp.json()
    except httpx.ConnectError:
        logger.error(f"无法连接 Java 后端: {url}，请确认 Java 服务已启动且端口正确")
        raise RuntimeError(f"无法连接 Java 后端({JAVA_BASE_URL})，请确认服务已启动")
    except httpx.HTTPStatusError as e:
        status = e.response.status_code
        logger.error(f"Java 接口返回错误: {url} HTTP {status}: {e.response.text[:300]}")
        if status == 404:
            raise RuntimeError(
                f"Java 接口不存在(404): {path}。"
                "请确认 Java 后端已重启加载最新的 AgentToolsController。"
            )
        elif status == 502 or status == 503:
            raise RuntimeError(
                f"Java 服务暂时不可用({status}): {path}。"
                "请确认 Java 后端已重启（加载 AiChatController 新增的 agent/tools 方法）。"
            )
        else:
            raise RuntimeError(f"Java 接口错误 HTTP {status}: {path}")
    except httpx.TimeoutException:
        logger.error(f"Java 接口超时: {url}")
        raise RuntimeError(f"Java 后端响应超时({JAVA_BASE_URL})，请检查服务状态")


class ListIngestTasksTool(BaseTool):
    name = "list_ingest_tasks"
    description = (
        "查询导入任务列表。支持按状态筛选（1=待处理, 2=处理中, 3=成功, 4=失败）。"
        "适用于回答'最近有哪些失败的导入任务'、'有哪些正在处理的导入'等问题。"
        "返回任务编号、状态、文档ID、错误信息等。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "status": {
                    "type": "integer",
                    "description": "任务状态筛选：1=待处理, 2=处理中, 3=成功, 4=失败",
                },
                "page_size": {
                    "type": "integer",
                    "description": "返回条数，默认10",
                },
            },
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        status = kwargs.get("status")
        page_size = kwargs.get("page_size", 10)

        params = {"pageNum": 1, "pageSize": page_size}
        if status is not None:
            params["status"] = status

        try:
            data = await _java_get("/ai/chat/agent/tools/ingest-tasks", params)
            if data.get("code") == 0:
                page = data.get("data", {})
                rows = page.get("rows", [])
                status_map = {1: "待处理", 2: "处理中", 3: "成功", 4: "失败"}
                tasks = []
                for t in rows:
                    tasks.append(
                        {
                            "task_id": t.get("taskId"),
                            "task_no": t.get("taskNo"),
                            "document_id": t.get("documentId"),
                            "status": t.get("status"),
                            "status_text": status_map.get(t.get("status"), "未知"),
                            "error_message": t.get("errorMessage"),
                            "chunk_count": t.get("chunkCount"),
                            "started_time": t.get("startedTime"),
                            "finished_time": t.get("finishedTime"),
                        }
                    )
                # 构建状态统计摘要，让 LLM 直接看到关键数据
                status_counts = {1: 0, 2: 0, 3: 0, 4: 0}
                for t in tasks:
                    st = t.get("status")
                    if st in status_counts:
                        status_counts[st] += 1
                stats_parts = [f"{status_map[s]}{c}个" for s, c in status_counts.items() if c > 0]
                stats_text = "、".join(stats_parts)

                summary = (
                    f"共 {page.get('total', 0)} 个导入任务。"
                    f"状态分布：{stats_text if stats_text else '无数据'}。"
                )
                # 附加每个任务的关键信息到 summary 中
                detail_lines = []
                for t in tasks[:8]:
                    detail_lines.append(
                        f"  - [{t['task_no']}] 状态={t['status_text']}"
                        + (f" 错误={t.get('error_message', '')[:80]}" if t.get("error_message") else "")
                    )
                if detail_lines:
                    summary += "\n" + "\n".join(detail_lines)

                return {
                    "success": True,
                    "summary": summary,
                    "data": {"total": page.get("total"), "tasks": tasks},
                    "error": None,
                }
            else:
                return {
                    "success": False,
                    "summary": f"Java 接口返回错误: {data.get('msg', '未知错误')}",
                    "data": None,
                    "error": data.get("msg"),
                }
        except Exception as e:
            return {
                "success": False,
                "summary": f"查询导入任务失败: {str(e)}",
                "data": None,
                "error": str(e),
            }


class GetIngestTaskDetailTool(BaseTool):
    name = "get_ingest_task_detail"
    description = (
        "查询单个导入任务的详细信息，包括失败原因、处理时间、关联的知识库和文档ID等。"
        "适用于用户询问某个具体导入任务的详情或失败原因。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "task_id": {
                    "type": "integer",
                    "description": "导入任务ID",
                },
            },
            "required": ["task_id"],
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        task_id = kwargs.get("task_id")
        if not task_id:
            return {
                "success": False,
                "summary": "缺少 task_id 参数",
                "data": None,
                "error": "missing task_id",
            }

        try:
            data = await _java_get(f"/ai/chat/agent/tools/ingest-task/{task_id}")
            if data.get("code") == 0:
                t = data.get("data", {})
                status_map = {1: "待处理", 2: "处理中", 3: "成功", 4: "失败"}
                return {
                    "success": True,
                    "summary": f"任务 {t.get('taskNo')} 详情：状态={status_map.get(t.get('status'), '未知')}",
                    "data": {
                        "task_id": t.get("taskId"),
                        "task_no": t.get("taskNo"),
                        "document_id": t.get("documentId"),
                        "status": t.get("status"),
                        "status_text": status_map.get(t.get("status"), "未知"),
                        "error_message": t.get("errorMessage"),
                        "chunk_count": t.get("chunkCount"),
                        "retry_count": t.get("retryCount"),
                        "python_kb_id": t.get("pythonKbId"),
                        "python_doc_id": t.get("pythonDocId"),
                        "started_time": t.get("startedTime"),
                        "finished_time": t.get("finishedTime"),
                    },
                    "error": None,
                }
            else:
                return {
                    "success": False,
                    "summary": f"Java 接口返回错误: {data.get('msg', '未知错误')}",
                    "data": None,
                    "error": data.get("msg"),
                }
        except Exception as e:
            return {
                "success": False,
                "summary": f"查询任务详情失败: {str(e)}",
                "data": None,
                "error": str(e),
            }


class GetDocumentDetailTool(BaseTool):
    name = "get_document_detail"
    description = (
        "根据文档ID查询文档基本信息，包括标题、分类、摘要、状态、版本号等。"
        "适用于用户询问某个具体文档的详情、状态或元数据。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "document_id": {
                    "type": "integer",
                    "description": "文档ID",
                },
            },
            "required": ["document_id"],
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        doc_id = kwargs.get("document_id")
        if not doc_id:
            return {"success": False, "summary": "缺少 document_id 参数", "data": None, "error": "missing document_id"}

        try:
            data = await _java_get(f"/ai/chat/agent/tools/document/{doc_id}")
            if data.get("code") == 0:
                d = data.get("data", {})
                status_map = {1: "草稿", 2: "待审核", 3: "已发布", 4: "已驳回", 5: "已归档"}
                return {
                    "success": True,
                    "summary": (
                        f"文档「{d.get('title', '未知')}」："
                        f"状态={status_map.get(d.get('status'), '未知')}，"
                        f"分类ID={d.get('categoryId')}，"
                        f"版本={d.get('currentVersionNo', '无')}"
                    ),
                    "data": {
                        "document_id": d.get("documentId"),
                        "title": d.get("title"),
                        "category_id": d.get("categoryId"),
                        "summary": d.get("summary"),
                        "tags": d.get("tags"),
                        "status": d.get("status"),
                        "status_text": status_map.get(d.get("status"), "未知"),
                        "current_version_no": d.get("currentVersionNo"),
                        "creator_id": d.get("creatorId"),
                        "update_time": d.get("updateTime"),
                    },
                    "error": None,
                }
            return {"success": False, "summary": f"查询失败: {data.get('msg')}", "data": None, "error": data.get("msg")}
        except Exception as e:
            return {"success": False, "summary": f"查询文档详情失败: {str(e)}", "data": None, "error": str(e)}


class ListDocumentsByCategoryTool(BaseTool):
    name = "list_documents_by_category"
    description = (
        "按分类查询文档列表，支持按状态筛选。"
        "文档状态码：1=草稿, 2=待审核, 3=已发布, 4=已驳回, 5=已归档。"
        "适用于用户询问'某个分类下有哪些文档'、'已发布的文档有哪些'等问题。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "category_id": {
                    "type": "integer",
                    "description": "分类ID（可选，不传则查全部）",
                },
                "status": {
                    "type": "integer",
                    "description": "文档状态筛选（可选）：1=草稿, 2=待审核, 3=已发布, 4=已驳回, 5=已归档",
                },
                "page_size": {
                    "type": "integer",
                    "description": "返回条数，默认10",
                },
            },
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        category_id = kwargs.get("category_id")
        status = kwargs.get("status")
        page_size = kwargs.get("page_size", 10)
        params = {"pageNum": 1, "pageSize": page_size}
        if category_id is not None:
            params["categoryId"] = category_id
        if status is not None:
            params["status"] = status

        try:
            data = await _java_get("/ai/chat/agent/tools/documents", params)
            if data.get("code") == 0:
                page = data.get("data", {})
                rows = page.get("rows", [])
                status_map = {1: "草稿", 2: "待审核", 3: "已发布", 4: "已驳回", 5: "已归档"}
                docs = []
                for d in rows:
                    docs.append({
                        "document_id": d.get("documentId"),
                        "title": d.get("title"),
                        "category_id": d.get("categoryId"),
                        "status": d.get("status"),
                        "status_text": status_map.get(d.get("status"), "未知"),
                        "current_version_no": d.get("currentVersionNo"),
                        "summary": (d.get("summary") or "")[:150],
                        "update_time": d.get("updateTime"),
                    })

                # 统计状态分布
                status_counts = {}
                for d in docs:
                    st = d["status_text"]
                    status_counts[st] = status_counts.get(st, 0) + 1
                stats_text = "、".join(f"{k}{v}个" for k, v in status_counts.items())

                return {
                    "success": True,
                    "summary": (
                        f"共 {page.get('total', 0)} 个文档（当前页 {len(docs)} 条）。"
                        + (f"状态分布：{stats_text}。" if stats_text else "")
                    ),
                    "data": {"total": page.get("total"), "documents": docs},
                    "error": None,
                }
            return {"success": False, "summary": f"查询失败: {data.get('msg')}", "data": None, "error": data.get("msg")}
        except Exception as e:
            return {"success": False, "summary": f"查询文档列表失败: {str(e)}", "data": None, "error": str(e)}


class GetKbMappingInfoTool(BaseTool):
    name = "get_kb_mapping_info"
    description = (
        "查询 Java 业务分类与 Python 知识库的映射关系。"
        "返回默认知识库ID及各分类对应的知识库ID。"
        "适用于用户询问'某个文档会导入到哪个知识库'、'分类和知识库的对应关系'等问题。"
    )

    def parameters_schema(self) -> dict:
        return {"type": "object", "properties": {}}

    async def run(self, **kwargs) -> Dict[str, Any]:
        try:
            data = await _java_get("/ai/chat/agent/tools/kb-mapping")
            if data.get("code") == 0:
                info = data.get("data", {})
                default_kb = info.get("defaultKbId", "未知")
                mappings = info.get("categoryMappings", {})
                mapping_text = "、".join(f"分类{cid}→KB{kid}" for cid, kid in mappings.items())
                return {
                    "success": True,
                    "summary": f"默认知识库ID={default_kb}。分类映射：{mapping_text if mapping_text else '无'}。",
                    "data": {"default_kb_id": default_kb, "category_mappings": mappings},
                    "error": None,
                }
            return {"success": False, "summary": f"查询失败: {data.get('msg')}", "data": None, "error": data.get("msg")}
        except Exception as e:
            return {"success": False, "summary": f"查询KB映射失败: {str(e)}", "data": None, "error": str(e)}


class ListChatSessionsTool(BaseTool):
    name = "list_chat_sessions"
    description = (
        "查询AI问答的聊天会话列表，返回会话标题、最后消息时间等信息。"
        "适用于用户询问'最近有哪些对话'、'当前有多少个会话'等问题。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "page_size": {
                    "type": "integer",
                    "description": "返回条数，默认10",
                },
            },
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        page_size = kwargs.get("page_size", 10)
        try:
            data = await _java_get("/ai/chat/agent/tools/chat-sessions", {"pageNum": 1, "pageSize": page_size})
            if data.get("code") == 0:
                page = data.get("data", {})
                rows = page.get("rows", [])
                sessions = []
                for s in rows:
                    sessions.append({
                        "session_id": s.get("sessionId"),
                        "title": s.get("sessionTitle"),
                        "user_id": s.get("userId"),
                        "last_message_time": s.get("lastMessageTime"),
                        "status": s.get("status"),
                    })
                # 摘要包含会话标题列表，让 LLM 能直接描述
                title_lines = []
                for s in sessions:
                    title_lines.append(
                        f"  - [会话{s['session_id']}] {s['title']} "
                        f"（最后消息: {s.get('last_message_time', '未知')}）"
                    )
                summary = (
                    f"共 {page.get('total', 0)} 个聊天会话（当前页 {len(sessions)} 条）。\n"
                    + "\n".join(title_lines)
                )
                return {
                    "success": True,
                    "summary": summary,
                    "data": {"total": page.get("total"), "sessions": sessions},
                    "error": None,
                }
            return {"success": False, "summary": f"查询失败: {data.get('msg')}", "data": None, "error": data.get("msg")}
        except Exception as e:
            return {"success": False, "summary": f"查询会话列表失败: {str(e)}", "data": None, "error": str(e)}


class GetChatHistoryTool(BaseTool):
    name = "get_chat_history"
    description = (
        "查询指定聊天会话的历史消息记录，返回用户和AI的对话内容及来源引用。"
        "适用于用户询问'之前的对话内容'、'某个会话里讨论了什么'等问题。"
    )

    def parameters_schema(self) -> dict:
        return {
            "type": "object",
            "properties": {
                "session_id": {
                    "type": "integer",
                    "description": "会话ID",
                },
            },
            "required": ["session_id"],
        }

    async def run(self, **kwargs) -> Dict[str, Any]:
        session_id = kwargs.get("session_id")
        if not session_id:
            return {"success": False, "summary": "缺少 session_id 参数", "data": None, "error": "missing session_id"}

        # 清洗：从"会话7"中提取数字7，兼容 Planner 可能传入非纯数字的情况
        if isinstance(session_id, str):
            import re
            nums = re.findall(r'\d+', session_id)
            if nums:
                session_id = int(nums[0])

        try:
            data = await _java_get(f"/ai/chat/agent/tools/chat-sessions/{session_id}/messages")
            if data.get("code") == 0:
                messages = data.get("data", [])
                role_map = {1: "用户", 2: "AI"}
                msgs = []
                for m in messages:
                    msgs.append({
                        "message_id": m.get("messageId"),
                        "role": m.get("messageRole"),
                        "role_text": role_map.get(m.get("messageRole"), "未知"),
                        "content": (m.get("messageContent") or "")[:300],
                        "sources_count": len(m.get("sources") or []),
                        "create_time": m.get("createTime"),
                    })

                # 对话摘要 + 内容预览
                user_count = sum(1 for m in msgs if m["role"] == 1)
                ai_count = sum(1 for m in msgs if m["role"] == 2)

                preview_lines = []
                for m in msgs[:10]:  # 最近10条
                    role_label = m["role_text"]
                    content_preview = m["content"][:120]
                    preview_lines.append(f"  [{role_label}] {content_preview}")

                summary = f"会话共 {len(msgs)} 条消息（用户{user_count}条，AI{ai_count}条）。"
                if preview_lines:
                    summary += "\n最近对话：\n" + "\n".join(preview_lines)

                return {
                    "success": True,
                    "summary": summary,
                    "data": {"session_id": session_id, "total": len(msgs), "messages": msgs},
                    "error": None,
                }
            return {"success": False, "summary": f"查询失败: {data.get('msg')}", "data": None, "error": data.get("msg")}
        except Exception as e:
            return {"success": False, "summary": f"查询聊天历史失败: {str(e)}", "data": None, "error": str(e)}
