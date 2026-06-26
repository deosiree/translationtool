"""PreTranslate mappers 与 retrieval 纯函数单元测试。"""

import pytest

from app.graph.pre_translate.utils.retrieval import (
    parse_target_lang,
    similarity,
    strip_placeholders,
)
from app.services.pre_translate.mappers import map_graph_state_to_agent_meta


@pytest.mark.unit
def test_strip_placeholders_removes_percent_n():
    """strip_placeholders 应去除 %N 占位符。"""
    text = "正在查询第 %1/%2 个路径的OID..."
    assert strip_placeholders(text) == "正在查询第 / 个路径的OID..."


@pytest.mark.unit
def test_similarity_identical_is_one():
    """完全相同文本相似度应为 1.0。"""
    assert similarity("admin", "admin") == 1.0


@pytest.mark.unit
def test_parse_target_lang_splits_on_dash():
    """parse_target_lang 应按 '-' 分割取目标语。"""
    assert parse_target_lang("中文-俄文") == "俄文"


@pytest.mark.unit
def test_map_graph_state_to_agent_meta_six_fields():
    """map_graph_state_to_agent_meta 应输出六字段 agent_meta。"""
    meta = map_graph_state_to_agent_meta(
        {
            "confidence": 1.0,
            "review_status": "auto_approved",
            "suggested_translation": "t",
            "similar_terms": [],
            "retrieval_method": "exact",
            "llm_reasoning": "基于术语：精确匹配",
        }
    )
    assert set(meta.keys()) == {
        "confidence",
        "review_status",
        "suggested_translation",
        "similar_terms",
        "retrieval_method",
        "reasoning",
    }
    assert meta["reasoning"] == "基于术语：精确匹配"
