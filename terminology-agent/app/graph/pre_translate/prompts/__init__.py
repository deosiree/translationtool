"""PreTranslate LLM prompt 模板。"""

from __future__ import annotations

from app.graph.pre_translate.prompts.pre_translate_suggest import (
    build_pre_translate_system_prompt,
    build_pre_translate_user_message,
)
from app.graph.pre_translate.prompts.suggest import (
    SUGGEST_SYSTEM_PROMPT,
    build_suggest_user_message,
)

__all__ = [
    "SUGGEST_SYSTEM_PROMPT",
    "build_pre_translate_system_prompt",
    "build_pre_translate_user_message",
    "build_suggest_user_message",
]
