"""Grep 线检索纯函数 — jieba 切词 + term_word lookup（含 Phase 3d n-gram 对齐）。

不含 IO；``lookup`` 由调用方注入（单测 mock / 节点内 WordRepository）。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable

from app.graph.pre_translate.constants import GREP_WORD_SCORE
from app.graph.pre_translate.utils.align_spans import align_spans_with_lexicon
from app.graph.pre_translate.utils.decompose import Span
from app.shared.term_word.segment import segment_source_text


@dataclass
class GrepHit:
    """单条 Grep 命中。"""

    word: str
    translate: str
    score: float
    ambiguous: bool = False


@dataclass
class GrepRetrieveResult:
    """Grep 检索汇总结果。"""

    hits: list[GrepHit]
    whole_sentence_exact: bool
    whole_sentence_translate: str | None
    ambiguous_words: list[str]


def _pick_translate(rows: list[Any]) -> tuple[str | None, bool]:
    """从 lookup 结果选取唯一译法；多行则视为 ambiguous。"""
    if not rows:
        return None, False
    if len(rows) > 1:
        return None, True
    translate = getattr(rows[0], "translate", None) or rows[0].get("translate")
    return (str(translate).strip() if translate else None), False


def grep_retrieve_candidates(
    *,
    source_text: str,
    lookup: Callable[[str], list[Any]],
) -> GrepRetrieveResult:
    """对 source_text 做整句 + jieba/n-gram 对齐后的词级 Grep lookup。"""
    text = (source_text or "").strip()
    if not text:
        return GrepRetrieveResult([], False, None, [])

    hits: list[GrepHit] = []
    ambiguous_words: list[str] = []

    whole_rows = lookup(text)
    whole_translate, whole_ambiguous = _pick_translate(whole_rows)
    if whole_ambiguous:
        ambiguous_words.append(text)
    whole_exact = whole_translate is not None and not whole_ambiguous

    if whole_exact and whole_translate:
        hits.append(
            GrepHit(word=text, translate=whole_translate, score=1.0, ambiguous=False)
        )

    spans = [
        Span(text=token, start=start, end=end)
        for token, start, end in segment_source_text(text)
    ]
    cache: dict[str, tuple[str | None, bool, dict]] = {}

    def cached_lookup(word: str) -> tuple[str | None, bool, dict]:
        if word not in cache:
            t, a = _pick_translate(lookup(word))
            cache[word] = (t, a, {})
        return cache[word]

    aligned = align_spans_with_lexicon(spans, cached_lookup)
    seen: set[str] = set()
    if whole_exact:
        seen.add(text)

    for span in aligned:
        word = span.text
        if word in seen:
            continue
        seen.add(word)
        if span.ambiguous:
            ambiguous_words.append(word)
            hits.append(GrepHit(word=word, translate="", score=0.0, ambiguous=True))
        elif span.translate:
            hits.append(
                GrepHit(
                    word=word,
                    translate=span.translate,
                    score=GREP_WORD_SCORE,
                    ambiguous=False,
                )
            )

    return GrepRetrieveResult(
        hits=hits,
        whole_sentence_exact=whole_exact,
        whole_sentence_translate=whole_translate if whole_exact else None,
        ambiguous_words=ambiguous_words,
    )
