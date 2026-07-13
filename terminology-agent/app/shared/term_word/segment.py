"""通用中文分词切界 — jieba 默认词典（非术语库驱动）。"""

from __future__ import annotations

import jieba

_initialized = False


def _ensure_jieba() -> None:
    global _initialized
    if not _initialized:
        jieba.initialize()
        _initialized = True


def segment_source_text(text: str) -> list[tuple[str, int, int]]:
    """对 text 做 jieba 通用分词，返回 (token, start, end) 列表。

    切界仅依赖 jieba 默认词频，不 load_userdict(term_word)。

    Args:
        text: 待切分原文。

    Returns:
        非空白 token 的 (词, start, end) 列表；end 为 exclusive。
    """
    raw = (text or "").strip()
    if not raw:
        return []
    _ensure_jieba()
    return [
        (token, start, end)
        for token, start, end in jieba.tokenize(raw, mode="default")
        if token.strip()
    ]
