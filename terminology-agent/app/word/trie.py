"""Trie 最长匹配 — Phase 3 Grep 拆词用；P2 建库仅统计。"""

from __future__ import annotations


class Trie:
    """字符级 Trie，支持从已知词条集合做最长匹配切分。"""

    def __init__(self) -> None:
        self._root: dict = {}
        self._entries: set[str] = set()

    def add(self, phrase: str) -> None:
        """插入一条已知词条（空串忽略）。"""
        phrase = phrase.strip()
        if not phrase:
            return
        self._entries.add(phrase)
        node = self._root
        for ch in phrase:
            node = node.setdefault(ch, {})
        node["$"] = phrase

    def build_from_entries(self, entries: list[str]) -> None:
        """批量插入词条。"""
        for entry in entries:
            self.add(entry)

    @property
    def entries(self) -> frozenset[str]:
        return frozenset(self._entries)

    def longest_match_at(self, text: str, start: int) -> tuple[str, int] | None:
        """从 start 起找最长匹配，返回 (matched_phrase, end_exclusive)。"""
        node = self._root
        best: tuple[str, int] | None = None
        for i in range(start, len(text)):
            ch = text[i]
            if ch not in node:
                break
            node = node[ch]
            if "$" in node:
                best = (node["$"], i + 1)
        return best

    def segment(self, text: str) -> list[str]:
        """贪心最长匹配切分；未命中字符逐字输出（便于 Phase 3 子串 grep）。"""
        if not text:
            return []
        parts: list[str] = []
        i = 0
        while i < len(text):
            matched = self.longest_match_at(text, i)
            if matched:
                phrase, end = matched
                parts.append(phrase)
                i = end
            else:
                parts.append(text[i])
                i += 1
        return parts
