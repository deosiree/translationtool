"""normalize_cn_lexemes SSOT — 先保序去重，再滤停用词。"""

import pytest

from app.shared.term_word.stopwords import normalize_cn_lexemes


@pytest.mark.unit
def test_normalize_dedupe_then_filter_stopwords():
    """先去重再过滤，避免同一词片重复做停用词判定。"""
    assert normalize_cn_lexemes(
        ["文件", "、", "系统", "、", "资源", "文件"]
    ) == ["文件", "系统", "资源"]


@pytest.mark.unit
def test_normalize_strips_blank_and_drops_punct():
    assert normalize_cn_lexemes([" 文件 ", "", "、", "  ", "系统"]) == [
        "文件",
        "系统",
    ]


@pytest.mark.unit
def test_normalize_preserves_order_of_first_seen():
    assert normalize_cn_lexemes(["系统", "文件", "系统"]) == ["系统", "文件"]


@pytest.mark.unit
def test_normalize_empty():
    assert normalize_cn_lexemes([]) == []
    assert normalize_cn_lexemes(None) == []  # type: ignore[arg-type]
