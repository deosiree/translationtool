"""grep_retrieve 纯函数单测 — 整句 / 词级 / ambiguous。"""

from types import SimpleNamespace

import pytest

from app.graph.pre_translate.utils.grep_retrieve import grep_retrieve_candidates


@pytest.mark.unit
def test_whole_sentence_exact():
    rows = [SimpleNamespace(translate="Кнопка")]

    def lookup(word: str):
        if word == "按钮":
            return rows
        return []

    result = grep_retrieve_candidates(source_text="按钮", lookup=lookup)
    assert result.whole_sentence_exact is True
    assert result.whole_sentence_translate == "Кнопка"


@pytest.mark.unit
def test_comment_filter_via_lookup():
    def lookup(word: str):
        if word == "按钮":
            return [SimpleNamespace(translate="A"), SimpleNamespace(translate="B")]
        return []

    result = grep_retrieve_candidates(source_text="按钮", lookup=lookup)
    assert "按钮" in result.ambiguous_words


@pytest.mark.unit
def test_word_level_hit():
    def lookup(word: str):
        if word == "文件":
            return [SimpleNamespace(translate="файл")]
        return []

    result = grep_retrieve_candidates(source_text="文件管理", lookup=lookup)
    assert any(h.word == "文件" and h.translate == "файл" for h in result.hits)


@pytest.mark.unit
def test_ngram_align_hit():
    """相邻 jieba token 合并后能命中复合词。"""

    def lookup(word: str):
        if word == "文件系统":
            return [SimpleNamespace(translate="File System")]
        return []

    result = grep_retrieve_candidates(source_text="文件系统", lookup=lookup)
    assert result.whole_sentence_exact is True
    assert any(h.word == "文件系统" for h in result.hits)
