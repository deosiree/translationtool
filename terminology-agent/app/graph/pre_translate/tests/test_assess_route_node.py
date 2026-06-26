"""assess_route 功能节点单元测试。"""

import pytest

from app.graph.pre_translate.nodes.features.workflow.assess_route import assess_route_node


@pytest.mark.graph
@pytest.mark.asyncio
async def test_assess_auto_approved_above_threshold():
    state = {
        "confidence": 0.95,
        "confidence_threshold": 0.8,
        "suggested_translation": "Перевод",
    }
    result = await assess_route_node(state)
    assert result["review_status"] == "auto_approved"


@pytest.mark.graph
@pytest.mark.asyncio
async def test_assess_needs_human_below_threshold():
    state = {
        "confidence": 0.65,
        "confidence_threshold": 0.8,
        "suggested_translation": "Перевод",
    }
    result = await assess_route_node(state)
    assert result["review_status"] == "needs_human"


@pytest.mark.graph
@pytest.mark.asyncio
async def test_assess_needs_human_no_translation():
    state = {
        "confidence": 0.9,
        "confidence_threshold": 0.8,
        "suggested_translation": None,
    }
    result = await assess_route_node(state)
    assert result["review_status"] == "needs_human"
