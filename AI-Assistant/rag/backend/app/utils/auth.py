"""
API Key 认证中间件 — 校验 X-API-Key 请求头，与 Java 端 ApiKeyAuthFilter 配合使用。
"""
from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware
from app.core.config import settings
from loguru import logger

# 无需认证的路径前缀（健康检查等）
PUBLIC_PATHS = {
    "/api/system/health",
    "/",
    "/docs",
    "/openapi.json",
    "/redoc",
}


class ApiKeyMiddleware(BaseHTTPMiddleware):
    """校验 X-API-Key 请求头的中间件。

    规则：
    1. 未配置 INTERNAL_API_KEY 时跳过校验（开发模式兼容）
    2. 白名单路径（health、docs）跳过
    3. 其他所有请求必须携带匹配的 X-API-Key 头
    """

    async def dispatch(self, request: Request, call_next):
        # 未配置密钥 → 跳过（向后兼容本地开发环境）
        if not settings.INTERNAL_API_KEY:
            return await call_next(request)

        # 白名单路径跳过认证
        path = request.url.path
        if path in PUBLIC_PATHS or path.startswith("/docs") or path.startswith("/openapi"):
            return await call_next(request)

        # 校验 X-API-Key
        api_key = request.headers.get("X-API-Key")
        if not api_key:
            logger.warning(f"Missing X-API-Key header: {request.method} {path}")
            raise HTTPException(status_code=401, detail="Missing X-API-Key header")

        if api_key != settings.INTERNAL_API_KEY:
            logger.warning(f"Invalid X-API-Key: {request.method} {path}")
            raise HTTPException(status_code=403, detail="Invalid X-API-Key")

        return await call_next(request)
