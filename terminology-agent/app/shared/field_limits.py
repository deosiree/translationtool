"""DB 对齐的字段长度常量 — LLM 输出 Schema 与工作台落库的唯一长度源。"""

from __future__ import annotations

# t_translate（db/init/schema.sql）
TRANSLATE_MAX = 1024
AUDIT_SUGGEST_MAX = 255

# format_agent_reasoning 前缀「{SOURCE_LABEL}：」最长字符数
SOURCE_LABEL_MAX = 7

# reasoning detail 段上限（拼装后 audit_suggest ≤ AUDIT_SUGGEST_MAX）
LLM_REASONING_DETAIL_MAX = AUDIT_SUGGEST_MAX - SOURCE_LABEL_MAX

# compose_suggest terms_used 单项上限
TERM_USED_ITEM_MAX = 64
TERM_USED_LIST_MAX = 20


def audit_suggest_budget() -> int:
    """llm_detail / LLM reasoning 字段允许的最大字符数。"""
    return LLM_REASONING_DETAIL_MAX


def fits_audit_suggest(text: str) -> bool:
    """拼装后的 audit_suggest 是否不超过 DB 列宽。"""
    return len(text or "") <= AUDIT_SUGGEST_MAX


def assert_fits_audit_suggest(text: str) -> str:
    """单测/开发断言：超长则抛 AssertionError。"""
    if not fits_audit_suggest(text):
        raise AssertionError(
            f"audit_suggest length {len(text)} exceeds {AUDIT_SUGGEST_MAX}"
        )
    return text
