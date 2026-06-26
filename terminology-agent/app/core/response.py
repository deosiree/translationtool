"""统一 API 响应包装 — 对齐 Java BaseController / HttpResponse。"""

from __future__ import annotations

from enum import IntEnum
from typing import Any

from pydantic import BaseModel


class ResponseCode(IntEnum):
    """对齐 Java ResultCode — agent 现阶段使用 200~203。"""

    SUCCESS = 200
    FAIL = 201  # audit 不存在 / 重复审核
    PARAM_ERROR = 202  # 参数校验失败
    SERVICE_ERROR = 203  # 未捕获异常
    # 204~208 登录/权限 — 预留，agent 暂无鉴权
    LOGIN_AUTH = 204
    LOGIN_EXPIRED = 205
    PERMISSION = 206


class JavaResponse(BaseModel):
    """统一 API 响应包装，供前端 request 拦截器解析。"""

    code: int = ResponseCode.SUCCESS
    message: str = "success"
    data: Any = None


def ok(data: Any = None, message: str = "success") -> JavaResponse:
    """成功响应 — 对标 BaseController.ok()。"""
    return JavaResponse(code=ResponseCode.SUCCESS, message=message, data=data)


def fail(
    message: str,
    *,
    code: ResponseCode = ResponseCode.FAIL,
    data: Any = None,
) -> JavaResponse:
    """业务/参数/服务失败 — 对标 BaseController.error()。"""
    return JavaResponse(code=int(code), message=message, data=data)
