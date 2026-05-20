"""
Vector store service based on ChromaDB.
"""
import asyncio
import time
import uuid
from typing import List, Optional, Tuple

import chromadb
from chromadb.config import Settings as ChromaSettings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_ollama import OllamaEmbeddings
from loguru import logger

from app.core.config import settings
from app.services.cache import embedding_cache


class AsyncEmbeddingWrapper:
    def __init__(self):
        self._emb = None
        self._semaphore: Optional[asyncio.Semaphore] = None

    def _get_emb(self):
        if self._emb is None:
            if settings.EMBEDDING_PROVIDER.lower() == "huggingface":
                self._emb = HuggingFaceEmbeddings(
                    model_name=settings.HF_EMBEDDING_MODEL,
                    model_kwargs={"device": settings.HF_EMBEDDING_DEVICE},
                    encode_kwargs={
                        "normalize_embeddings": settings.HF_NORMALIZE_EMBEDDINGS
                    },
                )
                logger.info(f"Embedding model loaded: {settings.HF_EMBEDDING_MODEL}")
            else:
                self._emb = OllamaEmbeddings(
                    base_url=settings.OLLAMA_BASE_URL,
                    model=settings.OLLAMA_EMBEDDING_MODEL,
                )
                logger.info(
                    f"Embedding model loaded: {settings.OLLAMA_EMBEDDING_MODEL}"
                )
        return self._emb

    def _get_semaphore(self) -> asyncio.Semaphore:
        if self._semaphore is None:
            self._semaphore = asyncio.Semaphore(settings.EMBEDDING_CONCURRENCY)
        return self._semaphore

    def _sync_embed_documents(self, texts: List[str]) -> List[List[float]]:
        return self._get_emb().embed_documents(texts)

    def _sync_embed_query(self, text: str) -> List[float]:
        return self._get_emb().embed_query(text)

    async def aembed_documents(self, texts: List[str]) -> List[List[float]]:
        if not texts:
            return []

        cached_vectors, miss_indices = await embedding_cache.get_batch(texts)
        if not miss_indices:
            logger.debug(f"EmbeddingCache full hit: {len(texts)} texts")
            return cached_vectors  # type: ignore

        miss_texts = [texts[i] for i in miss_indices]
        new_vectors = await self._async_batch_embed(miss_texts)

        await embedding_cache.set_batch(miss_texts, new_vectors)

        for idx, vector in zip(miss_indices, new_vectors):
            cached_vectors[idx] = vector

        logger.debug(
            f"EmbeddingCache: {len(texts) - len(miss_indices)} hits, "
            f"{len(miss_indices)} misses"
        )
        return cached_vectors  # type: ignore

    async def aembed_query(self, text: str) -> List[float]:
        cached = await embedding_cache.get(text)
        if cached is not None:
            logger.debug("EmbeddingCache query HIT")
            return cached

        loop = asyncio.get_event_loop()
        async with self._get_semaphore():
            vector = await loop.run_in_executor(None, self._sync_embed_query, text)

        await embedding_cache.set(text, vector)
        return vector

    async def _async_batch_embed(self, texts: List[str]) -> List[List[float]]:
        result: List[List[float]] = []
        loop = asyncio.get_event_loop()
        batch_size = settings.EMBED_BATCH_SIZE

        for index in range(0, len(texts), batch_size):
            batch = texts[index:index + batch_size]
            async with self._get_semaphore():
                started_at = time.time()
                vectors = await loop.run_in_executor(
                    None,
                    self._sync_embed_documents,
                    batch,
                )
                elapsed = (time.time() - started_at) * 1000
                logger.debug(
                    f"Embed batch {index // batch_size + 1}: "
                    f"{len(batch)} texts in {elapsed:.1f}ms"
                )
            result.extend(vectors)
        return result

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        batch_size = settings.EMBED_BATCH_SIZE
        result = []
        for index in range(0, len(texts), batch_size):
            batch = texts[index:index + batch_size]
            started_at = time.time()
            vectors = self._sync_embed_documents(batch)
            elapsed = (time.time() - started_at) * 1000
            logger.debug(f"sync embed_documents batch: {len(batch)} in {elapsed:.1f}ms")
            result.extend(vectors)
        return result

    def embed_query(self, text: str) -> List[float]:
        return self._sync_embed_query(text)


class VectorStoreService:
    def __init__(self):
        self._emb_wrapper = AsyncEmbeddingWrapper()
        self._client: Optional[chromadb.PersistentClient] = None
        self._stores: dict = {}

    def _get_client(self) -> chromadb.PersistentClient:
        if self._client is None:
            self._client = chromadb.PersistentClient(
                path=settings.CHROMA_PERSIST_DIR,
                settings=ChromaSettings(anonymized_telemetry=False),
            )
        return self._client

    def _collection_name(self, kb_id: int) -> str:
        return f"kb_{kb_id}"

    def get_store(self, kb_id: int) -> Chroma:
        if kb_id not in self._stores:
            self._stores[kb_id] = Chroma(
                client=self._get_client(),
                collection_name=self._collection_name(kb_id),
                embedding_function=self._emb_wrapper,
            )
        return self._stores[kb_id]

    def _text_splitter(self) -> RecursiveCharacterTextSplitter:
        return RecursiveCharacterTextSplitter(
            chunk_size=settings.CHUNK_SIZE,
            chunk_overlap=settings.CHUNK_OVERLAP,
            separators=["\n\n", "\n", "。", "，", "；", ".", "!", "?", ";", " ", ""],
            length_function=len,
        )

    async def add_documents_async(
        self,
        kb_id: int,
        documents: List[Document],
        doc_id: int,
        filename: str,
    ) -> int:
        splitter = self._text_splitter()
        chunks = splitter.split_documents(documents)

        if not chunks:
            logger.warning(f"No chunks for doc_id={doc_id} filename={filename}")
            return 0

        for index, chunk in enumerate(chunks):
            chunk.metadata.update(
                {
                    "doc_id": str(doc_id),
                    "kb_id": str(kb_id),
                    "filename": filename,
                    "chunk_index": str(index),
                }
            )

        texts = [chunk.page_content for chunk in chunks]
        started_at = time.time()
        vectors = await self._emb_wrapper.aembed_documents(texts)
        embed_ms = (time.time() - started_at) * 1000
        logger.info(
            f"[EMBED] doc_id={doc_id} | chunks={len(chunks)} | "
            f"embed_time={embed_ms:.1f}ms"
        )

        store = self.get_store(kb_id)
        batch_size = 50
        for index in range(0, len(chunks), batch_size):
            batch_chunks = chunks[index:index + batch_size]
            _ = vectors[index:index + batch_size]
            ids = [str(uuid.uuid4()) for _ in batch_chunks]
            store.add_documents(batch_chunks, ids=ids)

        logger.info(
            f"[VECTOR_STORE] Added kb={kb_id} doc_id={doc_id} "
            f"filename={filename} chunks={len(chunks)}"
        )
        return len(chunks)

    async def similarity_search_async(
        self,
        kb_id: int,
        query: str,
        k: int = None,
        strategy: str = "similarity",
        score_threshold: float = None,
        filter_doc_ids: Optional[List[int]] = None,
    ) -> Tuple[List[Tuple[Document, float]], int]:
        k = k or settings.TOP_K
        threshold = (
            score_threshold
            if score_threshold is not None
            else settings.RETRIEVAL_SCORE_THRESHOLD
        )
        store = self.get_store(kb_id)

        where_filter = None
        if filter_doc_ids:
            if len(filter_doc_ids) == 1:
                where_filter = {"doc_id": str(filter_doc_ids[0])}
            else:
                where_filter = {"doc_id": {"$in": [str(doc_id) for doc_id in filter_doc_ids]}}

        try:
            loop = asyncio.get_event_loop()

            if strategy == "mmr":
                raw_docs = await loop.run_in_executor(
                    None,
                    lambda: store.max_marginal_relevance_search(
                        query,
                        k=k,
                        fetch_k=k * 3,
                        filter=where_filter,
                    ),
                )
                raw = [(doc, 1.0) for doc in raw_docs]
            elif strategy == "hybrid":
                sim_results = await loop.run_in_executor(
                    None,
                    lambda: store.similarity_search_with_score(
                        query,
                        k=k,
                        filter=where_filter,
                    ),
                )
                mmr_docs = await loop.run_in_executor(
                    None,
                    lambda: store.max_marginal_relevance_search(
                        query,
                        k=k // 2 + 1,
                        fetch_k=k * 2,
                        filter=where_filter,
                    ),
                )
                seen = {id(doc) for doc, _ in sim_results}
                raw = list(sim_results)
                for doc in mmr_docs:
                    if id(doc) not in seen and len(raw) < k:
                        raw.append((doc, 0.8))
                        seen.add(id(doc))
            else:
                raw = await loop.run_in_executor(
                    None,
                    lambda: store.similarity_search_with_score(
                        query,
                        k=k,
                        filter=where_filter,
                    ),
                )

            filtered = []
            filtered_out = 0
            for doc, score in raw:
                import math

                similarity = math.exp(-float(score))
                if similarity >= threshold:
                    filtered.append((doc, similarity))
                else:
                    filtered_out += 1
                    logger.debug(
                        f"[FILTER] score={score:.4f} sim={similarity:.4f} "
                        f"< threshold={threshold}"
                    )

            filtered.sort(key=lambda item: item[1], reverse=True)
            return filtered, filtered_out

        except Exception as exc:
            logger.error(f"[VECTOR_STORE] Search error kb={kb_id}: {exc}")
            return [], 0

    def delete_documents(self, kb_id: int, doc_id: int):
        try:
            collection = self._get_client().get_collection(self._collection_name(kb_id))
            result = collection.get(where={"doc_id": str(doc_id)})
            if result and result["ids"]:
                collection.delete(ids=result["ids"])
                logger.info(
                    f"[VECTOR_STORE] Deleted {len(result['ids'])} vectors "
                    f"kb={kb_id} doc_id={doc_id}"
                )
            if kb_id in self._stores:
                del self._stores[kb_id]
        except Exception as exc:
            logger.error(f"[VECTOR_STORE] Delete doc error: {exc}")

    def delete_kb(self, kb_id: int):
        try:
            self._get_client().delete_collection(self._collection_name(kb_id))
            self._stores.pop(kb_id, None)
            logger.info(f"[VECTOR_STORE] Deleted collection kb={kb_id}")
        except Exception as exc:
            logger.error(f"[VECTOR_STORE] Delete KB error: {exc}")

    def get_kb_stats(self, kb_id: int) -> dict:
        try:
            collection = self._get_client().get_collection(self._collection_name(kb_id))
            return {"vector_count": collection.count()}
        except Exception:
            return {"vector_count": 0}


vector_service = VectorStoreService()
