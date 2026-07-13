"""LLM 拼装结果词片术语校验。"""

from __future__ import annotations

from app.graph.pre_translate.utils.decompose import Span


def validate_mandatory_terms(translation: str, spans: list[Span]) -> tuple[bool, list[str]]:
    """检查词片术语译法是否出现在 LLM 输出中（case-insensitive）。

    Args:
        translation: LLM 返回译文。
        spans: lookup 后的 spans。

    Returns:
        ``(ok, missing_terms)`` — missing 为未出现的词片术语 translate 列表。
    """
    if not translation:
        return False, []
    lowered = translation.lower()
    missing: list[str] = []
    for span in spans:
        if not span.translate or span.ambiguous:
            continue
        term = str(span.translate).strip()
        if term and term.lower() not in lowered:
            missing.append(term)
    return len(missing) == 0, missing
