"""compose_suggest 节点单测。"""

from unittest.mock import AsyncMock, patch

import pytest

from app.graph.pre_translate.constants import LLM_COMPOSE_CAP, LLM_COMPOSE_FALLBACK_CONF
from app.graph.pre_translate.nodes.features.llm.compose_suggest import compose_suggest_node


@pytest.mark.asyncio
@pytest.mark.graph
async def test_compose_suggest_success():
    state = {
        "source_text": "文件和系统",
        "target_lang": "英文",
        "coverage": 1.0,
        "decomposed_translation": "File和System",
        "spans": [
            {"text": "文件", "translate": "File", "ambiguous": False},
            {"text": "和", "translate": None, "ambiguous": False},
            {"text": "系统", "translate": "System", "ambiguous": False},
        ],
    }

    mock_response = AsyncMock()
    mock_response.content = '{"translation":"File System","reasoning":"spaced"}'

    with patch("config.settings.settings.llm_api_key", "test-key"), patch(
        "langchain_openai.ChatOpenAI"
    ) as mock_llm_cls:
        mock_llm = AsyncMock()
        mock_llm.ainvoke = AsyncMock(return_value=mock_response)
        mock_llm_cls.return_value = mock_llm

        result = await compose_suggest_node(state)

    assert result["suggested_translation"] == "File System"
    assert result["confidence"] == LLM_COMPOSE_CAP
    assert "spaced" in (result.get("llm_detail") or "")


@pytest.mark.asyncio
@pytest.mark.graph
async def test_compose_suggest_no_api_key_fallback():
    state = {
        "source_text": "文件和系统",
        "target_lang": "英文",
        "coverage": 1.0,
        "decomposed_translation": "FileSystem",
        "spans": [],
    }

    with patch("config.settings.settings.llm_api_key", ""):
        result = await compose_suggest_node(state)

    assert result["suggested_translation"] == "FileSystem"
    assert result["confidence"] == LLM_COMPOSE_FALLBACK_CONF
