"""词条拆解 — jieba 切界输出带 offset 的 spans。"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.shared.term_word.segment import segment_source_text


@dataclass
class Span:
    """拆解 span：原文片段及可选译法（lookup 后填充）。"""

    text: str
    start: int
    end: int
    translate: str | None = None
    ambiguous: bool = False

    def to_dict(self) -> dict[str, Any]:
        return {
            "text": self.text,
            "start": self.start,
            "end": self.end,
            "translate": self.translate,
            "ambiguous": self.ambiguous,
        }


def decompose_to_spans(source_text: str) -> list[Span]:
    """对 source_text 做 jieba 通用分词，返回带 start/end 的 spans。

    Args:
        source_text: 待译词条原文。

    Returns:
        按顺序排列的 Span 列表。
    """
    text = (source_text or "").strip()
    if not text:
        return []

    return [
        Span(text=token, start=start, end=end)
        for token, start, end in segment_source_text(text)
    ]
