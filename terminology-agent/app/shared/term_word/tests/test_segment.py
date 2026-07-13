"""segment_source_text 单测 — jieba 切界 SSOT。"""

import pytest

from app.shared.term_word.segment import segment_source_text


@pytest.mark.unit
def test_segment_computer_machine_not_term_driven():
    """切界来自 jieba，非术语 Trie；与 term_word 内容无关。"""
    tokens = [t for t, _, _ in segment_source_text("计算机器")]
    assert "计算机" in tokens or "计算" in tokens
    assert tokens == [t for t, _, _ in segment_source_text("计算机器")]


@pytest.mark.unit
def test_segment_file_and_system_definition():
    tokens = [t for t, _, _ in segment_source_text("文件与系统资源的定义")]
    assert "文件" in tokens
    assert "与" in tokens
    assert "的" in tokens
    assert "定义" in tokens


@pytest.mark.unit
def test_segment_offsets_cover_text():
    text = "文件和系统"
    spans = segment_source_text(text)
    assert spans
    for token, start, end in spans:
        assert text[start:end] == token
    assert "".join(t for t, _, _ in spans) == text.replace(" ", "")
