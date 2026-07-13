"""decompose 纯函数单测。"""

import pytest

from app.graph.pre_translate.utils.decompose import decompose_to_spans


@pytest.mark.unit
def test_decompose_jieba_file_and_system():
    spans = decompose_to_spans("文件和系统")
    texts = [s.text for s in spans]
    assert "文件" in texts
    assert "系统" in texts
    assert spans[0].start == 0
    assert all(s.end > s.start for s in spans)


@pytest.mark.unit
def test_decompose_punctuation_tokens():
    spans = decompose_to_spans("文件与系统")
    texts = [s.text for s in spans]
    assert texts == ["文件", "与", "系统"]
