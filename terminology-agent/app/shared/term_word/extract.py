"""从 source_text 提取 word 列表 — 基于 Trie 最长匹配。"""

from __future__ import annotations

from app.shared.term_word.trie import Trie


def extract_words(text: str, trie: Trie) -> list[str]:
    """对 text 做最长匹配切分，返回 word 序列（含未命中单字）。"""
    return trie.segment(text.strip())


def unique_words(text: str, trie: Trie) -> list[str]:
    """切分后去重，保持首次出现顺序。"""
    seen: set[str] = set()
    result: list[str] = []
    for word in extract_words(text, trie):
        if word not in seen:
            seen.add(word)
            result.append(word)
    return result
