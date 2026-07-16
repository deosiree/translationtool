"""Phase 3d n-gram 合并 lookup 单测。"""

from app.graph.pre_translate.utils.align_spans import align_spans_with_lexicon
from app.graph.pre_translate.utils.decompose import Span


def test_merge_bigram_when_compound_in_lexicon():
    spans = [Span("文件", 0, 2), Span("系统", 2, 4)]
    lexicon = {
        "文件系统": ("File System", False),
        "文件": ("File", False),
        "系统": ("System", False),
    }
    out = align_spans_with_lexicon(spans, lambda w: lexicon.get(w, (None, False)))
    assert len(out) == 1
    assert out[0].text == "文件系统"
    assert out[0].translate == "File System"
    assert out[0].jieba_parts == ("文件", "系统")


def test_no_merge_when_compound_missing():
    spans = [Span("文件", 0, 2), Span("系统", 2, 4)]
    lexicon = {
        "文件": ("File", False),
        "系统": ("System", False),
    }
    out = align_spans_with_lexicon(spans, lambda w: lexicon.get(w, (None, False)))
    assert [s.text for s in out] == ["文件", "系统"]
    assert [s.translate for s in out] == ["File", "System"]
    assert out[0].jieba_parts == ("文件",)


def test_no_merge_when_compound_ambiguous():
    spans = [Span("文件", 0, 2), Span("系统", 2, 4)]
    lexicon = {
        "文件系统": (None, True),
        "文件": ("File", False),
        "系统": ("System", False),
    }
    out = align_spans_with_lexicon(spans, lambda w: lexicon.get(w, (None, False)))
    assert [s.text for s in out] == ["文件", "系统"]


def test_skip_non_contiguous_offsets():
    spans = [Span("文件", 0, 2), Span("系统", 3, 5)]  # gap at index 2
    lexicon = {"文件系统": ("X", False), "文件": ("File", False), "系统": ("System", False)}
    out = align_spans_with_lexicon(spans, lambda w: lexicon.get(w, (None, False)))
    assert [s.text for s in out] == ["文件", "系统"]
