"""translate_suggest 节点 — 配置应经 Settings（.env）读取。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest


@pytest.mark.graph
@pytest.mark.asyncio
async def test_translate_suggest_reads_llm_config_from_settings():
    from app.graph.pre_translate.nodes.features.llm.translate_suggest import translate_suggest_node

    mock_settings = MagicMock()
    mock_settings.llm_api_key = "test-key"
    mock_settings.llm_base_url = "https://api.test.com/v1"
    mock_settings.llm_model = "deepseek-v4-flash"
    mock_settings.llm_temperature = 0.25

    mock_llm = MagicMock()
    mock_llm.ainvoke = AsyncMock(
        return_value=MagicMock(
            content='{"translation": "Test", "reasoning": "ok"}',
        )
    )

    state = {
        "source_text": "测试词条",
        "target_lang": "俄文",
        "similar_terms": [],
    }

    with patch(
        "app.graph.pre_translate.nodes.features.llm.translate_suggest.settings",
        mock_settings,
    ):
        with patch("langchain_openai.ChatOpenAI", return_value=mock_llm) as mock_chat:
            result = await translate_suggest_node(state)

    mock_chat.assert_called_once_with(
        api_key="test-key",
        base_url="https://api.test.com/v1",
        model="deepseek-v4-flash",
        temperature=0.25,
    )
    assert result["suggested_translation"] == "Test"
    assert result["llm_detail"] == "ok"


@pytest.mark.graph
@pytest.mark.asyncio
async def test_translate_suggest_missing_api_key_no_fake_translation():
    from app.graph.pre_translate.nodes.features.llm.translate_suggest import translate_suggest_node

    mock_settings = MagicMock()
    mock_settings.llm_api_key = ""

    state = {"source_text": "测试", "target_lang": "俄文", "similar_terms": []}

    with patch(
        "app.graph.pre_translate.nodes.features.llm.translate_suggest.settings",
        mock_settings,
    ):
        with patch("langchain_openai.ChatOpenAI") as mock_chat:
            result = await translate_suggest_node(state)

    mock_chat.assert_not_called()
    assert result["suggested_translation"] is None
    assert result["error"] == "LLM not configured"
