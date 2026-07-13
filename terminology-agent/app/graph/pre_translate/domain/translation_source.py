"""翻译来源枚举与 Agent 说明（llm_reasoning）格式化。"""

from __future__ import annotations

from enum import Enum

from app.shared.field_limits import AUDIT_SUGGEST_MAX, fits_audit_suggest


class TranslationSource(str, Enum):
    """预翻译结果来源 — term / llm / hybrid（Phase 3a 启用 hybrid）。"""

    TERM = "term"
    LLM = "llm"
    HYBRID = "hybrid"


SOURCE_LABEL: dict[TranslationSource, str] = {
    TranslationSource.TERM: "基于术语",
    TranslationSource.LLM: "基于LLM机翻",
    TranslationSource.HYBRID: "基于混合检索",
}

# 拼装超长时回退短模板（不 slice 截断）
_SHORT_DETAIL: dict[TranslationSource, str] = {
    TranslationSource.TERM: "精确匹配术语库",
    TranslationSource.LLM: "LLM机翻生成",
    TranslationSource.HYBRID: "词片拼装自动批准",
}


def format_agent_reasoning(
    source: TranslationSource,
    detail: str | None = None,
) -> str:
    """将 translation_source 与补充说明格式化为前端 Agent 说明列文本。

    拼装后若超过 ``AUDIT_SUGGEST_MAX``，回退为来源标签 + 短模板（不截断半句）。

    Args:
        source: 翻译来源枚举。
        detail: 可选补充说明（如「精确匹配术语库」）。

    Returns:
        形如「基于术语：精确匹配术语库」的展示文本，长度 ≤255。
    """
    base = SOURCE_LABEL[source]
    if detail:
        result = f"{base}：{detail}"
        if not fits_audit_suggest(result):
            result = f"{base}：{_SHORT_DETAIL[source]}"
        return result
    return base


def format_agent_reasoning_with_meta(
    source: TranslationSource,
    detail: str | None = None,
) -> tuple[str, bool]:
    """同 ``format_agent_reasoning``，并返回是否触发了短模板 fallback。"""
    base = SOURCE_LABEL[source]
    if not detail:
        return base, False
    primary = f"{base}：{detail}"
    if fits_audit_suggest(primary):
        return primary, False
    return f"{base}：{_SHORT_DETAIL[source]}", True
