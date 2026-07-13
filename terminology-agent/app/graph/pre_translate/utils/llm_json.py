"""LLM JSON 响应解析 — 剥离 fence + Pydantic 校验。"""

from __future__ import annotations

import json
import re
from typing import TypeVar

from pydantic import BaseModel, ValidationError

T = TypeVar("T", bound=BaseModel)

_FENCE_RE = re.compile(r"^```(?:json)?\s*([\s\S]*?)\s*```$", re.IGNORECASE)


def strip_markdown_fence(content: str) -> str:
    """去掉 LLM 违规输出的 markdown code fence。"""
    text = (content or "").strip()
    match = _FENCE_RE.match(text)
    if match:
        return match.group(1).strip()
    return text


def parse_llm_output(content: str, model: type[T]) -> T | None:
    """解析并校验 LLM JSON；失败返回 None（由调用方 fallback）。"""
    text = strip_markdown_fence(content)
    if not text:
        return None
    try:
        raw = json.loads(text)
        return model.model_validate(raw)
    except (json.JSONDecodeError, TypeError, ValidationError):
        return None
