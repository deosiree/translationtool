"""segment_trace 组装单测。"""

import pytest

from app.graph.pre_translate.utils.segment_trace import (
    build_segment_trace,
    used_segmentation,
)


@pytest.mark.unit
def test_used_segmentation_by_spans():
    assert used_segmentation({"spans": [{"text": "文件"}]}) is True
    assert used_segmentation({"spans": [], "retrieval_method": "exact"}) is False


@pytest.mark.unit
def test_used_segmentation_by_method():
    assert used_segmentation({"retrieval_method": "hybrid"}) is True
    assert used_segmentation({"retrieval_method": "decomposed"}) is True
    assert used_segmentation({"retrieval_method": "grep"}) is True
    assert used_segmentation({"retrieval_method": "exact"}) is False


@pytest.mark.unit
def test_build_from_aligned_spans():
    state = {
        "retrieval_method": "decomposed",
        "spans": [
            {
                "text": "文件系统",
                "translate": "File System",
                "ambiguous": False,
                "jieba_parts": ["文件", "系统"],
            }
        ],
    }
    trace = build_segment_trace(state)
    assert trace is not None
    assert trace["jieba"] == ["文件", "系统"]
    assert trace["display"] == "文件 | 系统"
    assert trace["aligned"][0]["text"] == "文件系统"
    assert trace["aligned"][0]["jieba_parts"] == ["文件", "系统"]


@pytest.mark.unit
def test_build_none_for_exact():
    assert build_segment_trace({"retrieval_method": "exact", "spans": []}) is None
