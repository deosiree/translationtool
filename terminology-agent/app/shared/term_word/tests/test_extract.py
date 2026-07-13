"""extract 单测。"""

import pytest

from app.shared.term_word.extract import extract_words, unique_words


@pytest.mark.unit
def test_unique_words_preserves_order():
    words = unique_words("文件/系统")
    assert words == ["文件", "/", "系统"]


@pytest.mark.unit
def test_extract_words_matches_segment():
    assert extract_words("文件与系统") == ["文件", "与", "系统"]
