"""normalize_segment_trace_dict / finalize_jieba_tokens 门禁单测。"""

import pytest

from app.graph.pre_translate.utils.segment_trace import (
    finalize_jieba_tokens,
    normalize_segment_trace_dict,
)


@pytest.mark.unit
def test_finalize_jieba_tokens_dedupe_then_filter():
    jieba, display = finalize_jieba_tokens(
        ["文件", "、", "系统", "、", "资源", "文件"]
    )
    assert jieba == ["文件", "系统", "资源"]
    assert display == "文件 | 系统 | 资源"


@pytest.mark.unit
def test_normalize_segment_trace_dict_rewrites_jieba_display():
    dirty = {
        "jieba": ["文件", "、", "系统", "、", "资源", "文件"],
        "display": "文件 | 、 | 系统 | 、 | 资源 | 文件",
        "aligned": [{"text": "文件"}],
    }
    clean = normalize_segment_trace_dict(dirty)
    assert clean["jieba"] == ["文件", "系统", "资源"]
    assert clean["display"] == "文件 | 系统 | 资源"
    assert clean["aligned"] == [{"text": "文件"}]


@pytest.mark.unit
def test_normalize_segment_trace_dict_none():
    assert normalize_segment_trace_dict(None) is None
