"""
FastAPI application entrypoint.
"""
from contextlib import asynccontextmanager
import sys

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from loguru import logger

from app.api import chat, document, kb, system
from app.core.config import settings
from app.core.database import init_db
from app.services.retrieval_log import setup_rag_loggers


logger.remove()
logger.add(
    sys.stdout,
    level="INFO",
    format="<green>{time:HH:mm:ss}</green> | <level>{level}</level> | {message}",
)
logger.add("logs/app.log", rotation="10 MB", retention="7 days", level="INFO")


@asynccontextmanager
async def lifespan(app: FastAPI):
    embedding_model = (
        settings.HF_EMBEDDING_MODEL
        if settings.EMBEDDING_PROVIDER.lower() == "huggingface"
        else settings.OLLAMA_EMBEDDING_MODEL
    )

    logger.info("Starting RAG System...")
    init_db()
    setup_rag_loggers()
    logger.info("Database initialized")
    logger.info(f"LLM Model: {settings.DEEPSEEK_MODEL}")
    logger.info(
        f"Embedding Model: {embedding_model} "
        f"(provider={settings.EMBEDDING_PROVIDER})"
    )
    yield
    logger.info("Shutting down")


app = FastAPI(
    title="Enterprise RAG Knowledge Base",
    description="Enterprise internal RAG knowledge base service.",
    version=settings.APP_VERSION,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}")
    return JSONResponse(
        status_code=500,
        content={"code": 500, "message": str(exc), "data": None},
    )


app.include_router(kb.router)
app.include_router(document.router)
app.include_router(chat.router)
app.include_router(system.router)


@app.get("/")
async def root():
    return {
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "docs": "/docs",
        "author": "hua",
        "package": "top.modelx.rag",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
        log_level="info",
    )
