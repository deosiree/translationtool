"""decompose 纯函数单测。"""

import pytest

from app.graph.pre_translate.utils.decompose import decompose_to_spans
from app.shared.term_word.trie import Trie


@pytest.mark.unit
def test_decompose_longest_match_spans():
    trie = Trie()
    trie.build_from_entries(["文件", "系统", "资源"])
    spans = decompose_to_spans("文件系统", trie)
    texts = [s.text for s in spans]
    assert texts == ["文件", "系统"]
    assert spans[0].start == 0 and spans[0].end == 2
    assert spans[1].start == 2 and spans[1].end == 4


@pytest.mark.unit
def test_decompose_single_char_fallback():
    trie = Trie()
    spans = decompose_to_spans("ab", trie)
    assert [s.text for s in spans] == ["a", "b"]
