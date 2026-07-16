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
    # Phase 3d：除单字外还会试探 bigram「文件系统」（无唯一译法则不合并）
    assert repo.find_by_word.await_count >= 2
    assert any(
        c.args[0] == "文件系统" for c in repo.find_by_word.await_args_list
    )


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


@pytest.mark.unit
async def test_lookup_merges_bigram_when_compound_exists():
    """Phase 3d：库有「文件系统」时合并相邻 jieba span。"""
    repo = AsyncMock()

    async def find(word, **_):
        table = {
            "文件系统": [SimpleNamespace(translate="File System")],
            "文件": [SimpleNamespace(translate="File")],
            "系统": [SimpleNamespace(translate="System")],
        }
        return table.get(word, [])

    repo.find_by_word = AsyncMock(side_effect=find)
    spans = [Span("文件", 0, 2), Span("系统", 2, 4)]
    result = await lookup_lexeme_spans(
        repo, spans=spans, target_lang="英文", comment="", department=None
    )
    assert len(result) == 1
    assert result[0].text == "文件系统"
    assert result[0].translate == "File System"
    assert result[0].jieba_parts == ("文件", "系统")
