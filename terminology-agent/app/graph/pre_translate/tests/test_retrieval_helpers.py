"""PreTranslate 检索纯函数单元测试。"""

import pytest

from app.graph.pre_translate.utils.retrieval import (
    fuzzy_confidence,
    parse_target_lang,
    similarity,
    strip_placeholders,
)


@pytest.mark.unit
def test_strip_placeholders_removes_percent_n():
    text = "正在查询第 %1/%2 个路径的OID..."
    assert strip_placeholders(text) == "正在查询第 / 个路径的OID..."


@pytest.mark.unit
def test_similarity_identical_is_one():
    assert similarity("admin", "admin") == 1.0


@pytest.mark.unit
def test_parse_target_lang_splits_on_dash():
    assert parse_target_lang("中文-俄文") == "俄文"


@pytest.mark.unit
def test_fuzzy_confidence_capped_at_095():
    assert fuzzy_confidence(1.0) == 0.95
