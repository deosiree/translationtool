"""resolve_translation_source 意图节点单元测试。"""

import pytest

from app.graph.pre_translate.constants import FUZZY_AUTO_FLOOR
from app.graph.pre_translate.nodes.intentions.resolve_translation_source import (
    resolve_translation_source_node,
)
from app.graph.pre_translate.domain.translation_source import TranslationSource


@pytest.mark.graph
@pytest.mark.asyncio
async def test_resolve_exact_is_term():
    state = {
        "exact_hit": True,
        "fuzzy_hit": False,
        "retrieval_method": "exact",
        "retrieval_confidence": 1.0,
    }
    result = await resolve_translation_source_node(state)
    assert result["translation_source"] == TranslationSource.TERM.value
    assert "精确匹配" in (result.get("llm_detail") or "")


@pytest.mark.graph
@pytest.mark.asyncio
async def test_resolve_fuzzy_high_confidence_is_term():
    state = {
        "exact_hit": False,
        "fuzzy_hit": True,
        "retrieval_method": "fuzzy",
        "retrieval_confidence": FUZZY_AUTO_FLOOR,
    }
    result = await resolve_translation_source_node(state)
    assert result["translation_source"] == TranslationSource.TERM.value


@pytest.mark.graph
@pytest.mark.asyncio
async def test_resolve_none_is_llm():
    state = {
        "exact_hit": False,
        "fuzzy_hit": False,
        "retrieval_method": "none",
        "retrieval_confidence": 0.0,
    }
    result = await resolve_translation_source_node(state)
    assert result["translation_source"] == TranslationSource.LLM.value
    assert "未命中" in (result.get("llm_detail") or "")


@pytest.mark.graph
@pytest.mark.asyncio
async def test_resolve_fuzzy_low_is_llm():
    state = {
        "exact_hit": False,
        "fuzzy_hit": True,
        "retrieval_method": "fuzzy",
        "retrieval_confidence": 0.6,
    }
    result = await resolve_translation_source_node(state)
    assert result["translation_source"] == TranslationSource.LLM.value
