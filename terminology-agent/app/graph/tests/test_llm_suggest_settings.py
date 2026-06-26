"""llm_suggest 节点 — 配置应经 Settings（.env）读取。"""



from unittest.mock import AsyncMock, MagicMock, patch



import pytest





@pytest.mark.graph

@pytest.mark.asyncio

async def test_llm_suggest_reads_llm_config_from_settings():

    """ChatOpenAI 参数应来自 settings，而非 os.environ fallback。"""

    from app.graph.nodes.llm.suggest import llm_suggest_node



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

        "is_new_term": True,

        "source_text": "测试词条",

        "llm_reasoning": "button label",

    }



    with patch("app.graph.nodes.llm.suggest.settings", mock_settings):

        with patch("langchain_openai.ChatOpenAI", return_value=mock_llm) as mock_chat:

            result = await llm_suggest_node(state)



    mock_chat.assert_called_once_with(

        api_key="test-key",

        base_url="https://api.test.com/v1",

        model="deepseek-v4-flash",

        temperature=0.25,

    )

    assert result["suggested_translation"] == "Test"

    assert result["next_node"] == "review"





@pytest.mark.graph

@pytest.mark.asyncio

async def test_llm_suggest_missing_api_key_from_settings():

    """settings.llm_api_key 为空时应降级到人工审核，不调用 LLM。"""

    from app.graph.nodes.llm.suggest import llm_suggest_node



    mock_settings = MagicMock()

    mock_settings.llm_api_key = ""



    state = {"is_new_term": True, "source_text": "测试", "llm_reasoning": ""}



    with patch("app.graph.nodes.llm.suggest.settings", mock_settings):

        with patch("langchain_openai.ChatOpenAI") as mock_chat:

            result = await llm_suggest_node(state)



    mock_chat.assert_not_called()

    assert "[LLM 未配置" in result["suggested_translation"]

    assert result["next_node"] == "review"

