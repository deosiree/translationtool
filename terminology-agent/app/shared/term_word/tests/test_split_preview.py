"""拆分预览 / 停用词 / 批量 LLM 补译单测。"""

from unittest.mock import AsyncMock, patch

import pytest

from app.shared.term_word.split_llm_fill import (
    build_fill_jobs_from_items,
    merge_fill_into_candidates,
)
from app.shared.term_word.split_preview import (
    boost_split_jieba_words,
    split_entry_to_candidates,
    split_items_preview,
)
from app.shared.term_word.stopwords import filter_cn_tokens, is_cn_stopword


def test_filter_yu_stopword():
    assert is_cn_stopword("与")
    assert filter_cn_tokens(["文件", "与", "系统"]) == ["文件", "系统"]


def test_split_file_and_system_zip_align():
    rows = split_entry_to_candidates(
        entry="文件与系统",
        translate="File and System",
        target_lang="英文",
    )
    words = {r["word"]: r["translate"] for r in rows}
    assert "与" not in words
    assert words["文件"] == "File"
    assert words["系统"] == "System"
    assert all(r["status"] == "1" for r in rows)
    assert all(r["use_llm"] is False for r in rows)


def test_split_get_file_failed_no_misalign_zip():
    """中英停用词丢弃不对称时禁止 zip，不得出现失败→file。"""
    rows = split_entry_to_candidates(
        entry="获取文件失败",
        translate="Failed to get file",
        target_lang="英文",
    )
    words = {r["word"]: r["translate"] for r in rows}
    assert words.get("失败") != "file"
    assert words.get("获取") != "Failed"
    # 无可靠对齐时回退整词条
    assert any(
        r["word"] == "获取文件失败" and r["translate"] == "Failed to get file"
        for r in rows
    )
    assert all(r["use_llm"] is False for r in rows)


def test_split_field_length_segmentation():
    """切界种子应切出字段|长度，且可对称 zip。"""
    boost_split_jieba_words()
    rows = split_entry_to_candidates(
        entry="字段长度",
        translate="Field length",
        target_lang="英文",
    )
    words = {r["word"]: r["translate"] for r in rows}
    assert "段长度" not in words
    assert words.get("字段") == "Field"
    assert words.get("长度") == "length"
    assert all(r["use_llm"] is False for r in rows)


def test_split_drops_without_translate():
    rows = split_entry_to_candidates(
        entry="未知短语甲乙",
        translate="",
        target_lang="英文",
    )
    assert rows == []


def test_split_lexicon_priority():
    rows = split_entry_to_candidates(
        entry="文件与系统",
        translate="File and System",
        target_lang="英文",
        lexicon={"文件": "Document"},
    )
    words = {r["word"]: r["translate"] for r in rows}
    assert words["文件"] == "Document"
    assert words["系统"] == "System"


def test_split_items_dedupe():
    items = [
        {"entry": "文件与系统", "translate": "File and System", "target_lang": "英文"},
        {"entry": "文件系统", "translate": "File System", "target_lang": "英文"},
    ]
    rows = split_items_preview(items)
    keys = [(r["word"], r["target_lang"]) for r in rows]
    assert len(keys) == len(set(keys))


def test_build_fill_jobs_only_missing_translate():
    items = [
        {"entry": "获取文件失败", "translate": "Failed to get file", "target_lang": "英文"},
    ]
    candidates = [
        {
            "word": "获取",
            "translate": "",
            "target_lang": "英文",
            "source_entry": "获取文件失败",
            "use_llm": False,
        },
        {
            "word": "文件",
            "translate": "file",
            "target_lang": "英文",
            "source_entry": "获取文件失败",
            "use_llm": False,
        },
        {
            "word": "失败",
            "translate": "",
            "target_lang": "英文",
            "source_entry": "获取文件失败",
            "use_llm": False,
        },
    ]
    jobs = build_fill_jobs_from_items(items, candidates)
    assert len(jobs) == 1
    assert set(jobs[0]["fragments"]) == {"获取", "失败"}


def test_merge_fill_keeps_use_llm_false():
    candidates = [
        {
            "word": "失败",
            "translate": "",
            "target_lang": "英文",
            "source_entry": "获取文件失败",
            "use_llm": False,
            "status": "1",
        },
        {
            "word": "文件",
            "translate": "file",
            "target_lang": "英文",
            "source_entry": "获取文件失败",
            "use_llm": False,
            "status": "1",
        },
    ]
    fills = {"获取文件失败": {"失败": "Failed"}}
    merged = merge_fill_into_candidates(candidates, fills)
    by_word = {r["word"]: r for r in merged}
    assert by_word["失败"]["translate"] == "Failed"
    assert by_word["文件"]["translate"] == "file"
    assert all(r["use_llm"] is False for r in merged)
    assert "失败" in by_word


@pytest.mark.asyncio
async def test_fill_fragments_with_llm_one_call_multi_entry():
    from app.shared.term_word.split_llm_fill import fill_fragments_with_llm

    jobs = [
        {
            "id": "e0",
            "entry": "获取文件失败",
            "translate": "Failed to get file",
            "fragments": ["获取", "文件", "失败"],
        },
        {
            "id": "e1",
            "entry": "字段长度",
            "translate": "Field length",
            "fragments": ["字段", "长度"],
        },
    ]
    mock_resp = AsyncMock()
    mock_resp.content = (
        '{"results":['
        '{"id":"e0","items":['
        '{"word":"获取","translate":"get"},'
        '{"word":"文件","translate":"file"},'
        '{"word":"失败","translate":"Failed"}]},'
        '{"id":"e1","items":['
        '{"word":"字段","translate":"Field"},'
        '{"word":"长度","translate":"length"}]}'
        "]}"
    )
    mock_llm = AsyncMock()
    mock_llm.ainvoke = AsyncMock(return_value=mock_resp)

    with (
        patch("config.settings.settings.llm_api_key", "test-key"),
        patch("langchain_openai.ChatOpenAI", return_value=mock_llm),
    ):
        fills = await fill_fragments_with_llm(jobs)

    assert mock_llm.ainvoke.await_count == 1
    assert fills["获取文件失败"]["失败"] == "Failed"
    assert fills["获取文件失败"]["失败"] != "file"
    assert fills["字段长度"]["长度"] == "length"
