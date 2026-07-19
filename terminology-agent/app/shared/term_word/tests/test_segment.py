"""segment_source_text 单测 — jieba 切界 SSOT。"""

import pytest

from app.shared.term_word.segment import segment_source_text


@pytest.mark.unit
def test_segment_computer_machine_not_term_driven():
    """切界来自 jieba，非术语 Trie；与 term_word 内容无关。"""
    tokens = [t for t, _, _ in segment_source_text("计算机器")]
    assert "计算机" in tokens or "计算" in tokens
    assert tokens == [t for t, _, _ in segment_source_text("计算机器")]


@pytest.mark.unit
def test_segment_file_and_system_definition():
    tokens = [t for t, _, _ in segment_source_text("文件与系统资源的定义")]
    assert "文件" in tokens
    # 停用词（与、的）已被过滤
    assert "与" not in tokens
    assert "的" not in tokens
    assert "定义" in tokens


@pytest.mark.unit
def test_segment_offsets_cover_text():
    text = "文件和系统"
    spans = segment_source_text(text)
    assert spans
    for token, start, end in spans:
        assert text[start:end] == token
    # 停用词（和）被过滤，剩余 token 拼回不含停用词
    joined = "".join(t for t, _, _ in spans)
    assert "文件" in joined
    assert "系统" in joined


# ---------- 占位符 %N 合并与过滤 ----------


@pytest.mark.unit
def test_placeholder_merge_percent_digit():
    """% + 连续数字 → 合并为 %1，然后被过滤。"""
    tokens = [t for t, _, _ in segment_source_text("第%1页")]
    # %1、第、页 均被过滤
    assert not tokens


@pytest.mark.unit
def test_placeholder_merge_multi_digit():
    """% + 多位数字 → %12，然后被过滤。"""
    tokens = [t for t, _, _ in segment_source_text("第%12项")]
    assert not tokens


@pytest.mark.unit
def test_placeholder_merge_file_percent():
    """文件%1 → 仅保留文件。"""
    tokens = [t for t, _, _ in segment_source_text("文件%1")]
    assert "文件" in tokens
    assert "%1" not in tokens
    assert "%" not in tokens


@pytest.mark.unit
def test_placeholder_merge_slash_concat():
    """%1/%2 → 全为占位符/符号→结果为空。"""
    tokens = [t for t, _, _ in segment_source_text("%1/%2")]
    assert not tokens


@pytest.mark.unit
def test_placeholder_merge_dash_concat():
    """%1_%2 → 全为占位符/符号→结果为空。"""
    tokens = [t for t, _, _ in segment_source_text("%1_%2")]
    assert not tokens


@pytest.mark.unit
def test_filter_special_chars():
    """纯特殊字符（/、%、_）被过滤，中文词保留。"""
    tokens = [t for t, _, _ in segment_source_text("文件/系统")]
    assert "文件" in tokens
    assert "系统" in tokens or "系统" in "".join(tokens)
    # 单独的 / 被过滤
    assert "/" not in tokens


@pytest.mark.unit
def test_placeholder_normal_chinese_unchanged():
    """正常中文词界不受影响。"""
    tokens = [t for t, _, _ in segment_source_text("文件和系统资源的定义")]
    assert "文件" in tokens
    assert "系统" in tokens or "系统资源" in tokens
    assert "定义" in tokens


@pytest.mark.unit
def test_placeholder_offsets_cover_text():
    """含占位符+停用词的词条，保留的 token 偏移仍正确。"""
    text = "第%1页和%2项"
    spans = segment_source_text(text)
    # 第、页、项均为停用词 → 全部过滤 → 结果为空
    assert not spans
