"""
Application settings.
"""
from typing import List
import os

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "Enterprise RAG System"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    SECRET_KEY: str = "your-secret-key"

    HOST: str = "0.0.0.0"
    PORT: int = 8000

    DATABASE_URL: str = "mysql+pymysql://root:123456@localhost:3306/docbase_knowledge"
    DATABASE_POOL_SIZE: int = 10
    DATABASE_MAX_OVERFLOW: int = 20

    LLM_PROVIDER: str = "deepseek"
    DEEPSEEK_API_KEY: str = "your_deepseek_api_key"
    DEEPSEEK_BASE_URL: str = "https://api.deepseek.com"
    DEEPSEEK_MODEL: str = "deepseek-v4-flash"

    EMBEDDING_PROVIDER: str = "huggingface"
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_EMBEDDING_MODEL: str = "qwen3-embedding:4b"

    HF_EMBEDDING_MODEL: str = "BAAI/bge-m3"
    HF_EMBEDDING_DEVICE: str = "cpu"
    HF_NORMALIZE_EMBEDDINGS: bool = True
    HF_LOCAL_FILES_ONLY: bool = True

    CHROMA_PERSIST_DIR: str = "./chroma_db"
    CHROMA_COLLECTION_NAME: str = "rag_documents"

    UPLOAD_DIR: str = "./uploads"
    MAX_FILE_SIZE: int = 104857600
    ALLOWED_EXTENSIONS: str = "pdf,doc,docx,txt,xls,xlsx,jpg,jpeg,png,gif,bmp"

    # RAG
    CHUNK_SIZE: int = 800
    CHUNK_OVERLAP: int = 120
    TOP_K: int = 8
    RERANK_TOP_K: int = 5
    RETRIEVAL_SCORE_THRESHOLD: float = 0.1
    MAX_CONTEXT_LENGTH: int = 6000

    # More permissive retrieval settings for single-document Q&A
    DOC_FOCUSED_TOP_K: int = 12
    DOC_FOCUSED_RERANK_TOP_K: int = 8
    DOC_FOCUSED_SCORE_THRESHOLD: float = 0.03

    QUERY_CACHE_TTL: int = 300
    QUERY_CACHE_MAX_SIZE: int = 500
    EMBEDDING_CACHE_MAX_SIZE: int = 2000

    EMBEDDING_CONCURRENCY: int = 3
    EMBED_BATCH_SIZE: int = 20

    JAVA_BASE_URL: str = "http://localhost:8080"
    JAVA_API_KEY: str = ""
    INTERNAL_API_KEY: str = ""

    AGENT_MAX_STEPS: int = 4

    LOG_DIR: str = "./logs"
    LOG_RETRIEVAL: bool = True
    LOG_PROMPT: bool = True
    LOG_LEVEL: str = "INFO"

    @property
    def allowed_extensions_list(self) -> List[str]:
        return [ext.strip().lower() for ext in self.ALLOWED_EXTENSIONS.split(",")]

    def ensure_dirs(self):
        os.makedirs(self.UPLOAD_DIR, exist_ok=True)
        os.makedirs(self.CHROMA_PERSIST_DIR, exist_ok=True)
        os.makedirs(self.LOG_DIR, exist_ok=True)

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
settings.ensure_dirs()
