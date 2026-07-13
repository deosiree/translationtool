"""PreTranslate LLM JSON 输出 Schema — 与 field_limits / MySQL 列宽对齐。"""

from __future__ import annotations

from pydantic import BaseModel, Field, field_validator

from app.shared.field_limits import (
    LLM_REASONING_DETAIL_MAX,
    TERM_USED_ITEM_MAX,
    TERM_USED_LIST_MAX,
    TRANSLATE_MAX,
)


class PreTranslateLlmOutput(BaseModel):
    """translate_suggest / compose_suggest 共用核心字段。"""

    translation: str = Field(..., max_length=TRANSLATE_MAX)
    reasoning: str = Field(..., max_length=LLM_REASONING_DETAIL_MAX)

    @field_validator("translation", "reasoning", mode="before")
    @classmethod
    def _strip_text(cls, value):
        if value is None:
            return value
        return str(value).strip()


class ComposeSuggestLlmOutput(PreTranslateLlmOutput):
    """compose_suggest 扩展字段。"""

    terms_used: list[str] = Field(default_factory=list, max_length=TERM_USED_LIST_MAX)

    @field_validator("terms_used", mode="before")
    @classmethod
    def _normalize_terms_used(cls, value):
        if value is None:
            return []
        if not isinstance(value, list):
            return value
        return [str(item).strip()[:TERM_USED_ITEM_MAX] for item in value if str(item).strip()]
