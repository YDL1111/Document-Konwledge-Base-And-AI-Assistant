"""
RAG inference service.
"""
import json
import time
from typing import AsyncGenerator, List, Optional, Tuple

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.core.config import settings
from app.services.cache import query_cache
from app.services.retrieval_log import log_prompt, log_retrieval
from app.services.vector_store import vector_service


SYSTEM_PROMPT = """你是企业内部知识库助手，请根据以下参考文档回答用户问题。

重要规则：
1. 参考文档来自向量检索，可能与问题部分相关，请仔细阅读后作答。
2. 如果文档标题与问题不完全一致，但内容相关，也应提取并回答。
3. 只有在参考文档确实没有相关内容时，才回复“未找到相关信息”。
4. 引用内容时标注来源，例如：【来源：文件名】。
5. 回答要清晰、专业，并使用中文。

参考文档：
{context}
"""


class RAGService:
    def _get_llm(self, streaming: bool = False) -> ChatOpenAI:
        return ChatOpenAI(
            api_key=settings.DEEPSEEK_API_KEY,
            base_url=settings.DEEPSEEK_BASE_URL,
            model=settings.DEEPSEEK_MODEL,
            streaming=streaming,
            temperature=0.3,
            max_tokens=2048,
        )

    async def retrieve(
        self,
        kb_id: int,
        query: str,
        k: int = None,
        strategy: str = "similarity",
    ) -> Tuple[List[Tuple], bool]:
        k = k or settings.TOP_K
        started_at = time.time()

        cached = await query_cache.get(kb_id, query, k)
        if cached is not None:
            latency = (time.time() - started_at) * 1000
            log_retrieval(
                kb_id,
                query,
                cached,
                latency,
                cache_hit=True,
                strategy=strategy,
            )
            return cached, True

        results, filtered_out = await vector_service.similarity_search_async(
            kb_id=kb_id,
            query=query,
            k=k,
            strategy=strategy,
        )

        latency = (time.time() - started_at) * 1000
        log_retrieval(
            kb_id,
            query,
            results,
            latency,
            cache_hit=False,
            strategy=strategy,
            filtered_count=filtered_out,
        )

        if results:
            await query_cache.set(kb_id, query, k, results)

        return results, False

    def _build_context(self, results: List[Tuple]) -> Tuple[str, List[dict]]:
        top_results = results[:settings.RERANK_TOP_K]
        parts = []
        sources = []
        total_len = 0

        for index, (doc, score) in enumerate(top_results, start=1):
            filename = doc.metadata.get("filename", "未知文件")
            page = doc.metadata.get("page")
            page_info = f" 第{page}页" if page else ""
            content = doc.page_content

            if total_len + len(content) > settings.MAX_CONTEXT_LENGTH:
                remain = settings.MAX_CONTEXT_LENGTH - total_len
                if remain < 100:
                    break
                content = content[:remain] + "..."

            parts.append(
                f"[{index}] 【{filename}{page_info}】（相关度：{score:.2%}）\n{content}"
            )
            total_len += len(content)

            preview = doc.page_content[:300]
            if len(doc.page_content) > 300:
                preview += "..."

            sources.append(
                {
                    "index": index,
                    "filename": filename,
                    "page": page,
                    "score": round(float(score), 4),
                    "doc_id": doc.metadata.get("doc_id"),
                    "content": preview,
                }
            )

        return "\n\n---\n\n".join(parts), sources

    def _build_prompt_messages(self, history: List[dict]) -> list:
        messages = [("system", SYSTEM_PROMPT)]
        for message in history[-6:]:
            if message["role"] == "user":
                messages.append(("human", message["content"]))
            elif message["role"] == "assistant":
                messages.append(("ai", message["content"]))
        messages.append(("human", "{question}"))
        return messages

    async def chat(
        self,
        kb_id: int,
        question: str,
        history: Optional[List[dict]] = None,
        strategy: str = "similarity",
    ) -> Tuple[str, List[dict]]:
        history = history or []

        results, _ = await self.retrieve(kb_id, question, strategy=strategy)
        context, sources = self._build_context(results)

        log_prompt(
            kb_id=kb_id,
            question=question,
            context_length=len(context),
            history_turns=len(history) // 2,
            prompt_tokens_est=len(context) // 2 + len(question) // 2,
        )

        llm = self._get_llm(streaming=False)
        prompt = ChatPromptTemplate.from_messages(self._build_prompt_messages(history))
        chain = prompt | llm | StrOutputParser()
        answer = await chain.ainvoke({"context": context, "question": question})
        return answer, sources

    async def chat_stream(
        self,
        kb_id: int,
        question: str,
        history: Optional[List[dict]] = None,
        strategy: str = "similarity",
    ) -> AsyncGenerator[str, None]:
        history = history or []

        # Emit an early event so upstream SSE clients do not time out while retrieval starts.
        yield f"data: {json.dumps({'type': 'start', 'data': 'processing'}, ensure_ascii=False)}\n\n"

        results, _ = await self.retrieve(
            kb_id=kb_id,
            query=question,
            k=settings.TOP_K,
            strategy="hybrid",
        )
        context, sources = self._build_context(results)

        log_prompt(
            kb_id=kb_id,
            question=question,
            context_length=len(context),
            history_turns=len(history) // 2,
            prompt_tokens_est=len(context) // 2 + len(question) // 2,
        )

        yield f"data: {json.dumps({'type': 'sources', 'data': sources}, ensure_ascii=False)}\n\n"

        if not results:
            message = "根据当前知识库内容，未找到与该问题相关的信息。"
            yield f"data: {json.dumps({'type': 'token', 'data': message}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'type': 'done', 'data': message}, ensure_ascii=False)}\n\n"
            return

        llm = self._get_llm(streaming=True)
        prompt = ChatPromptTemplate.from_messages(self._build_prompt_messages(history))
        chain = prompt | llm | StrOutputParser()

        full_answer = ""
        async for chunk in chain.astream({"context": context, "question": question}):
            full_answer += chunk
            yield f"data: {json.dumps({'type': 'token', 'data': chunk}, ensure_ascii=False)}\n\n"

        yield f"data: {json.dumps({'type': 'done', 'data': full_answer}, ensure_ascii=False)}\n\n"

    def test_connection(self) -> bool:
        try:
            import httpx

            response = httpx.get(
                f"{settings.DEEPSEEK_BASE_URL}/models",
                headers={"Authorization": f"Bearer {settings.DEEPSEEK_API_KEY}"},
                timeout=5,
            )
            return response.status_code == 200
        except Exception:
            return False


rag_service = RAGService()
