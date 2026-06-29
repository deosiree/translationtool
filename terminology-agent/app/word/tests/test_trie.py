"""Trie 最长匹配单测。"""

import pytest

from app.word.trie import Trie


@pytest.mark.unit
def test_longest_match_prefers_longer_phrase():
    trie = Trie()
    trie.add("查询")
    trie.add("查询路径")
    matched = trie.longest_match_at("正在查询路径OID", 2)
    assert matched == ("查询路径", 6)


@pytest.mark.unit
def test_segment_greedy_longest():
    trie = Trie()
    trie.build_from_entries(["正在查询", "OID"])
    parts = trie.segment("正在查询OID")
    assert parts == ["正在查询", "OID"]


@pytest.mark.unit
def test_unmatched_characters_emitted_singly():
    trie = Trie()
    trie.add("AB")
    assert trie.segment("XABY") == ["X", "AB", "Y"]
