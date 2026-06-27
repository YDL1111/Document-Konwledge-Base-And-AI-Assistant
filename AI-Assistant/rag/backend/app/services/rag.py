"""
RAG inference service.
"""
import json
import re
import time
from typing import AsyncGenerator, List, Optional, Tuple

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.core.config import settings
from app.services.cache import query_cache
from app.services.retrieval_log import log_prompt, log_retrieval
from app.services.vector_store import vector_service


SYSTEM_PROMPT = """你是企业内部知识库助手，请严格基于参考文档回答用户问题。
回答规则：
1. 优先提取参考文档中最直接、最关键的信息作答，不要泛泛而谈。
2. 如果用户是在问某一篇文档的内容，请优先总结该文档中的核心段落，不要被无关片段干扰。
3. 如果已经召回到相关片段，但信息不完整，可以先给出基于已知内容的结论，再明确说明不确定部分。
4. 只有在参考文档里确实没有可支撑回答的内容时，才回答“未找到相关信息”。
5. 不要回答“请提供更具体的问题”这类空泛兜底话术，除非用户的问题本身完全无法理解。
6. 回答要使用中文，并在合适的位置自然标注来源，例如：【来源：文件名】。
参考文档：
{context}
"""


class RAGService:
    @staticmethod
    def _escape_prompt_text(text: str) -> str:
        if not text:
            return ""
        return text.replace("{", "{{").replace("}", "}}")

    @staticmethod
    def _normalize_text(text: str) -> str:
        if not text:
            return ""
        text = text.lower()
        text = re.sub(r"\.(pdf|doc|docx|txt)$", " ", text)
        text = re.sub(r"[^0-9a-z\u4e00-\u9fff]+", " ", text)
        text = re.sub(r"\s+", " ", text)
        return text.strip()

    def _extract_query_terms(self, query: str) -> List[str]:
        normalized = self._normalize_text(query)
        if not normalized:
            return []
        return [term for term in normalized.split(" ") if len(term) >= 2]

    def _filename_boost(self, query: str, doc) -> float:
        filename = self._normalize_text(doc.metadata.get("filename", ""))
        if not filename:
            return 0.0

        normalized_query = self._normalize_text(query)
        if not normalized_query:
            return 0.0

        boost = 0.0
        if filename and filename in normalized_query:
            boost += 0.45

        for term in self._extract_query_terms(query):
            if term in filename:
                boost += 0.08
        return min(boost, 0.6)

    def _rerank_results(
        self,
        query: str,
        results: List[Tuple],
        visible_doc_ids: Optional[List[int]] = None,
    ) -> List[Tuple]:
        if not results:
            return results

        doc_focused = self._is_doc_focused_query(visible_doc_ids)
        reranked = []
        for doc, score in results:
            boosted_score = float(score)
            if not doc_focused:
                boosted_score += self._filename_boost(query, doc)
            reranked.append((doc, boosted_score))

        reranked.sort(key=lambda item: item[1], reverse=True)
        return reranked

    def _get_llm(self, streaming: bool = False) -> ChatOpenAI:
        return ChatOpenAI(
            api_key=settings.DEEPSEEK_API_KEY,
            base_url=settings.DEEPSEEK_BASE_URL,
            model=settings.DEEPSEEK_MODEL,
            streaming=streaming,
            temperature=0.2,
            max_tokens=2048,
        )

    def _is_doc_focused_query(self, visible_doc_ids: Optional[List[int]]) -> bool:
        return visible_doc_ids is not None and len(visible_doc_ids) == 1

    async def retrieve(
        self,
        kb_id: int,
        query: str,
        k: int = None,
        strategy: str = "similarity",
        visible_doc_ids: Optional[List[int]] = None,
    ) -> Tuple[List[Tuple], bool]:
        doc_focused = self._is_doc_focused_query(visible_doc_ids)
        if doc_focused:
            k = k or settings.DOC_FOCUSED_TOP_K
            score_threshold = settings.DOC_FOCUSED_SCORE_THRESHOLD
            strategy = "similarity"
        else:
            k = k or settings.TOP_K
            score_threshold = settings.RETRIEVAL_SCORE_THRESHOLD

        started_at = time.time()
        scoped_query = visible_doc_ids is not None

        cached = None if scoped_query else await query_cache.get(kb_id, query, k)
        if cached is not None:
            latency = (time.time() - started_at) * 1000
            reranked_cached = self._rerank_results(query, cached, visible_doc_ids)
            log_retrieval(
                kb_id,
                query,
                reranked_cached,
                latency,
                cache_hit=True,
                strategy=strategy,
            )
            return reranked_cached, True

        results, filtered_out = await vector_service.similarity_search_async(
            kb_id=kb_id,
            query=query,
            k=k,
            strategy=strategy,
            score_threshold=score_threshold,
            filter_doc_ids=visible_doc_ids,
        )
        results = self._rerank_results(query, results, visible_doc_ids)

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

        if results and not scoped_query:
            await query_cache.set(kb_id, query, k, results)

        return results, False

    def _build_context(
        self, results: List[Tuple], visible_doc_ids: Optional[List[int]] = None
    ) -> Tuple[str, List[dict]]:
        rerank_top_k = (
            settings.DOC_FOCUSED_RERANK_TOP_K
            if self._is_doc_focused_query(visible_doc_ids)
            else settings.RERANK_TOP_K
        )
        top_results = results[:rerank_top_k]
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

            parts.append(f"[{index}] 【{filename}{page_info}】（相关度：{score:.2%}）\n{content}")
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
                messages.append(("human", self._escape_prompt_text(message["content"])))
            elif message["role"] == "assistant":
                messages.append(("ai", self._escape_prompt_text(message["content"])))
        messages.append(("human", "用户问题：{question}"))
        return messages

    async def chat(
        self,
        kb_id: int,
        question: str,
        history: Optional[List[dict]] = None,
        strategy: str = "similarity",
        visible_doc_ids: Optional[List[int]] = None,
    ) -> Tuple[str, List[dict]]:
        history = history or []

        results, _ = await self.retrieve(
            kb_id, question, strategy=strategy, visible_doc_ids=visible_doc_ids
        )
        context, sources = self._build_context(results, visible_doc_ids)

        log_prompt(
            kb_id=kb_id,
            question=question,
            context_length=len(context),
            history_turns=len(history) // 2,
            prompt_tokens_est=len(context) // 2 + len(question) // 2,
        )

        if not results:
            return "未找到相关信息。", []

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
        visible_doc_ids: Optional[List[int]] = None,
    ) -> AsyncGenerator[str, None]:
        history = history or []

        yield f"data: {json.dumps({'type': 'start', 'data': 'processing'}, ensure_ascii=False)}\n\n"

        doc_focused = self._is_doc_focused_query(visible_doc_ids)
        results, _ = await self.retrieve(
            kb_id=kb_id,
            query=question,
            k=settings.DOC_FOCUSED_TOP_K if doc_focused else settings.TOP_K,
            strategy="similarity" if doc_focused else "hybrid",
            visible_doc_ids=visible_doc_ids,
        )
        context, sources = self._build_context(results, visible_doc_ids)

        log_prompt(
            kb_id=kb_id,
            question=question,
            context_length=len(context),
            history_turns=len(history) // 2,
            prompt_tokens_est=len(context) // 2 + len(question) // 2,
        )

        if not results:
            message = "根据当前知识库内容，未找到与该问题相关的信息。"
            yield f"data: {json.dumps({'type': 'token', 'data': message}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'type': 'done', 'data': {'answer': message, 'sources': []}}, ensure_ascii=False)}\n\n"
            return

        llm = self._get_llm(streaming=True)
        prompt = ChatPromptTemplate.from_messages(self._build_prompt_messages(history))
        chain = prompt | llm | StrOutputParser()

        full_answer = ""
        async for chunk in chain.astream({"context": context, "question": question}):
            if not chunk:
                continue
            full_answer += chunk
            yield f"data: {json.dumps({'type': 'token', 'data': chunk}, ensure_ascii=False)}\n\n"

        if "未找到相关信息" in full_answer:
            sources = []
        if sources:
            yield f"data: {json.dumps({'type': 'sources', 'data': sources}, ensure_ascii=False)}\n\n"

        yield f"data: {json.dumps({'type': 'done', 'data': {'answer': full_answer, 'sources': sources}}, ensure_ascii=False)}\n\n"

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
