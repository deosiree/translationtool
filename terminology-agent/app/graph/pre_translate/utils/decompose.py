"""词条最长匹配拆解 — Trie segment 输出带 offset 的 spans。"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.shared.term_word.trie import Trie


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


def decompose_to_spans(source_text: str, trie: Trie) -> list[Span]:
    """对 source_text 做 Trie 最长匹配拆解，返回带 start/end 的 spans。

    Args:
        source_text: 待译词条原文。
        trie: approved word 构建的 Trie。

    Returns:
        按顺序排列的 Span 列表；未命中字符逐字成 span。
    """
    text = (source_text or "").strip()
    if not text:
        return []

    spans: list[Span] = []
    i = 0
    while i < len(text):
        matched = trie.longest_match_at(text, i)
        if matched:
            phrase, end = matched
            spans.append(Span(text=phrase, start=i, end=end))
            i = end
        else:
            spans.append(Span(text=text[i], start=i, end=i + 1))
            i += 1
    return spans
