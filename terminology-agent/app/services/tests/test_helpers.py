"""PreTranslateService 纯函数单元测试。"""

import pytest

from app.services.pre_translate_service import (
    _parse_target_lang,
    _similarity,
    _strip_placeholders,
)


@pytest.mark.unit
def test_strip_placeholders_removes_percent_n():
    """%1/%2 占位符应被剥离，避免相似度误判。"""
    text = "正在查询第 %1/%2 个路径的OID..."
    assert _strip_placeholders(text) == "正在查询第 / 个路径的OID..."


@pytest.mark.unit
def test_similarity_identical_is_one():
    """完全相同字符串相似度为 1.0。"""
    assert _similarity("admin", "admin") == 1.0


@pytest.mark.unit
def test_parse_target_lang_splits_on_dash():
    """translateType「中文-俄文」应解析出目标语种「俄文」。"""
    assert _parse_target_lang("中文-俄文") == "俄文"
