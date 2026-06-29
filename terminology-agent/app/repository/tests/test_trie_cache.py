"""trie_cache 单测 — load_trie_for_lang 与 WordRepository 集成。"""

from unittest.mock import AsyncMock, MagicMock

import pytest

from app.repository.trie_cache import clear_trie_cache, load_trie_for_lang


@pytest.mark.unit
async def test_trie_cache_loads_words():
    clear_trie_cache()
    session = AsyncMock()
    mock_repo = MagicMock()
    mock_repo.list_distinct_words = AsyncMock(return_value=["文件", "系统"])
    with pytest.MonkeyPatch.context() as mp:
        mp.setattr("app.repository.trie_cache.WordRepository", lambda _s: mock_repo)
        trie = await load_trie_for_lang(session, "俄文", ttl_sec=300)
    assert "文件" in trie.entries
    mock_repo.list_distinct_words.assert_awaited_once()
