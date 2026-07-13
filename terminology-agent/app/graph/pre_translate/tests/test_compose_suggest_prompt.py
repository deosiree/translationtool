"""compose_suggest prompt 单测。"""

import pytest

from app.graph.pre_translate.prompts.compose_suggest import (
    build_compose_suggest_system_prompt,
    build_compose_suggest_user_message,
)


@pytest.mark.unit
def test_system_prompt_mentions_span_terms():
    prompt = build_compose_suggest_system_prompt("英文")
    assert "词片术语表" in prompt
    assert "FileSystem" in prompt or "File+System" in prompt
    assert "248" in prompt or "≤248" in prompt
    assert "1024" in prompt
    assert "简体中文" in prompt


@pytest.mark.unit
def test_user_message_renders_span_table():
    msg = build_compose_suggest_user_message(
        source_text="文件和系统",
        target_lang="英文",
        coverage=1.0,
        spans=[
            {"text": "文件", "translate": "File", "ambiguous": False},
            {"text": "和", "translate": None, "ambiguous": False},
            {"text": "系统", "translate": "System", "ambiguous": False},
        ],
        decomposed_translation="File和System",
    )
    assert "File" in msg
    assert "System" in msg
    assert "Naive draft" in msg
