"""extract 单测。"""

import pytest

from app.word.extract import extract_words, unique_words
from app.word.trie import Trie


@pytest.mark.unit
def test_unique_words_preserves_order():
    trie = Trie()
    trie.build_from_entries(["查询", "路径"])
    words = unique_words("查询路径查询", trie)
    assert words == ["查询", "路径"]
