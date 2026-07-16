"""Phase 3d：jieba span 相邻 n-gram 合并 lookup（术语对齐，非切界）。

切界仍由 jieba SSOT；仅当合并串在 term_word 有唯一译法时合并 span。
合并后 Span.jieba_parts 保留原始 jieba token，供 trace。
"""

from __future__ import annotations

from typing import Callable

from app.graph.pre_translate.constants import ALIGN_MAX_NGRAM
from app.graph.pre_translate.utils.decompose import Span

# word -> (translate, ambiguous)
LookupFn = Callable[[str], tuple[str | None, bool]]


def _contiguous(spans: list[Span], start: int, n: int) -> bool:
    """合并窗口内 offset 必须首尾相接（无夹杂未切字符）。"""
    for j in range(start + 1, start + n):
        if spans[j].start != spans[j - 1].end:
            return False
    return True


def align_spans_with_lexicon(
    spans: list[Span],
    lookup: LookupFn,
    *,
    max_ngram: int = ALIGN_MAX_NGRAM,
) -> list[Span]:
    """从左到右贪心：优先更长 n-gram（≤max_ngram）唯一命中则合并。

    Args:
        spans: jieba 切界后的 Span（尚未或已可忽略 translate）。
        lookup: 同步查表 ``word -> (translate, ambiguous)``。
        max_ngram: 最大合并窗口（默认 3）。

    Returns:
        对齐后的 Span 列表；未合并项 ``jieba_parts`` 为单 token。
    """
    if not spans:
        return []

    out: list[Span] = []
    i = 0
    n_spans = len(spans)

    while i < n_spans:
        merged = False
        upper = min(max_ngram, n_spans - i)
        for n in range(upper, 1, -1):
            if not _contiguous(spans, i, n):
                continue
            parts = spans[i : i + n]
            compound = "".join(p.text for p in parts)
            translate, ambiguous = lookup(compound)
            if translate and not ambiguous:
                out.append(
                    Span(
                        text=compound,
                        start=parts[0].start,
                        end=parts[-1].end,
                        translate=translate,
                        ambiguous=False,
                        jieba_parts=tuple(p.text for p in parts),
                    )
                )
                i += n
                merged = True
                break
        if merged:
            continue

        one = spans[i]
        translate, ambiguous = lookup(one.text)
        out.append(
            Span(
                text=one.text,
                start=one.start,
                end=one.end,
                translate=translate,
                ambiguous=ambiguous,
                jieba_parts=(one.text,),
            )
        )
        i += 1

    return out
