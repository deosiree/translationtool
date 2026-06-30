"""lookup_lexemes 单测。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from app.graph.pre_translate.nodes.features.io.lookup_lexemes import lookup_lexeme_spans
from app.graph.pre_translate.utils.decompose import Span


@pytest.mark.unit
async def test_lookup_lexeme_spans_unique():
    repo = AsyncMock()
    repo.find_by_word = AsyncMock(
        side_effect=lambda word, **_: {
            "文件": [SimpleNamespace(translate="File")],
            "系统": [SimpleNamespace(translate="System")],
        }.get(word, [])
    )
    spans = [
        Span("文件", 0, 2),
        Span("系统", 2, 4),
    ]
    result = await lookup_lexeme_spans(
        repo, spans=spans, target_lang="英文", comment="", department=None
    )
    assert result[0].translate == "File"
    assert result[1].translate == "System"
    assert repo.find_by_word.await_count == 2


@pytest.mark.unit
async def test_lookup_ambiguous():
    repo = AsyncMock()
    repo.find_by_word = AsyncMock(
        return_value=[
            SimpleNamespace(translate="A"),
            SimpleNamespace(translate="B"),
        ]
    )
    spans = [Span("按钮", 0, 2)]
    result = await lookup_lexeme_spans(
        repo, spans=spans, target_lang="英文", comment="", department=None
    )
    assert result[0].ambiguous is True
    assert result[0].translate is None
