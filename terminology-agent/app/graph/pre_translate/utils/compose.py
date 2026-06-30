"""词级 span 确定性拼装 — 有译法用译法，否则保留原文。"""

from __future__ import annotations

from app.graph.pre_translate.utils.decompose import Span


def compose_translation(spans: list[Span]) -> str:
    """按 span 顺序拼接整句译文。

    Args:
        spans: 已 lookup 的 Span 列表。

    Returns:
        拼装后的建议译文；ambiguous 或无译法的 span 保留原文片段。
    """
    parts: list[str] = []
    for span in spans:
        if span.translate and not span.ambiguous:
            parts.append(span.translate)
        else:
            parts.append(span.text)
    return "".join(parts)
