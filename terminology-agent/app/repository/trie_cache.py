"""term_word approved 词表 Trie 进程内缓存。

Grep 线 retrieve 按 ``target_lang`` 加载 DISTINCT ``word`` 建 Trie，避免每条请求全表扫描。
"""

from __future__ import annotations

import time

from sqlalchemy.ext.asyncio import AsyncSession

from app.repository.word_repo import WordRepository
from app.shared.term_word.trie import Trie

_CACHE: dict[str, tuple[Trie, float]] = {}
_DEFAULT_TTL_SEC = 300


async def load_trie_for_lang(
    session: AsyncSession,
    target_lang: str,
    *,
    ttl_sec: int = _DEFAULT_TTL_SEC,
) -> Trie:
    """按 target_lang 加载 approved word 列表并构建 Trie（带 TTL 缓存）。

    Args:
        session: 异步 DB 会话。
        target_lang: 目标语种；空则返回空 Trie。
        ttl_sec: 缓存 TTL（秒）；默认 300。

    Returns:
        可用于 ``grep_retrieve_candidates`` 的 Trie 实例。
    """
    lang = (target_lang or "").strip()
    if not lang:
        return Trie()

    now = time.monotonic()
    cached = _CACHE.get(lang)
    if cached and now - cached[1] < ttl_sec:
        return cached[0]

    words = await WordRepository(session).list_distinct_words(lang)
    trie = Trie()
    trie.build_from_entries(words)
    _CACHE[lang] = (trie, now)
    return trie


def clear_trie_cache() -> None:
    """清空进程内 Trie 缓存（测试或 ``build_word_index --rebuild`` 后调用）。"""
    _CACHE.clear()
