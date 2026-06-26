"""业务异常与全局 exception handlers — 统一 Java 风格错误响应。"""

from __future__ import annotations

import logging
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.response import ResponseCode, JavaResponse, fail

logger = logging.getLogger(__name__)


class ApiError(Exception):
    """可预期的业务错误 — router/service 中 raise，由 handler 转为 {code, message, data}。"""

    def __init__(
        self,
        message: str,
        code: ResponseCode = ResponseCode.FAIL,
        data: Any = None,
    ):
        """构造业务异常。

        Args:
            message: 返回给前端的错误文案。
            code: Java 风格业务码，默认 201（FAIL）。
            data: 可选附加数据。
        """
        self.message = message
        self.code = code
        self.data = data
        super().__init__(message)


def _to_json_response(response: JavaResponse) -> JSONResponse:
    """Java 风格：HTTP 始终 200，错误靠 body.code 区分。"""
    return JSONResponse(status_code=200, content=response.model_dump())


async def api_error_handler(_request: Request, exc: ApiError) -> JSONResponse:
    """将 ApiError 转为 {code, message, data} JSON 响应。"""
    return _to_json_response(fail(exc.message, code=exc.code, data=exc.data))


async def validation_error_handler(
    _request: Request, exc: RequestValidationError
) -> JSONResponse:
    """将 Pydantic 校验失败转为 code=202 的参数错误响应。"""
    errors = exc.errors()
    first = errors[0] if errors else {}
    loc = ".".join(str(part) for part in first.get("loc", ()))
    msg = first.get("msg", "参数不正确")
    message = f"{loc}: {msg}" if loc else msg
    return _to_json_response(
        fail(message, code=ResponseCode.PARAM_ERROR, data=errors)
    )


async def http_exception_handler(_request: Request, exc: HTTPException) -> JSONResponse:
    """将 FastAPI HTTPException 转为 code=201 的业务失败响应。"""
    detail = exc.detail if isinstance(exc.detail, str) else str(exc.detail)
    return _to_json_response(fail(detail, code=ResponseCode.FAIL))


async def unhandled_exception_handler(_request: Request, exc: Exception) -> JSONResponse:
    """捕获未处理异常，记录日志并返回 code=203 服务错误。"""
    logger.exception("Unhandled exception: %s", exc)
    return _to_json_response(
        fail("服务异常，请稍后重试", code=ResponseCode.SERVICE_ERROR)
    )


def register_exception_handlers(app: FastAPI) -> None:
    """注册全局异常处理器到 FastAPI 应用。"""
    app.add_exception_handler(ApiError, api_error_handler)
    app.add_exception_handler(RequestValidationError, validation_error_handler)
    app.add_exception_handler(HTTPException, http_exception_handler)
    app.add_exception_handler(Exception, unhandled_exception_handler)
