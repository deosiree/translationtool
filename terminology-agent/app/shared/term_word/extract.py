"""从 source_text 提取 word 列表 — 基于 jieba 通用分词。"""

from __future__ import annotations

from app.shared.term_word.segment import segment_source_text


def extract_words(text: str) -> list[str]:
    """对 text 做 jieba 切分，返回 word 序列。"""
    return [token for token, _, _ in segment_source_text(text)]


def unique_words(text: str) -> list[str]:
    """切分后去重，保持首次出现顺序。"""
    seen: set[str] = set()
    result: list[str] = []
    for word in extract_words(text):
        if word not in seen:
            seen.add(word)
            result.append(word)
    return result
