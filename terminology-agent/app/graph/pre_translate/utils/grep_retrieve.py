"""Grep 线检索纯函数 — Trie 拆词 + term_word lookup。

不含 IO；``lookup`` 由调用方注入（单测 mock / 节点内 WordRepository）。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable

from app.graph.pre_translate.constants import GREP_WORD_SCORE
from app.shared.term_word.extract import unique_words
from app.shared.term_word.trie import Trie


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
    """从 lookup 结果选取唯一译法；多行则视为 ambiguous。

    Args:
        rows: ``find_by_word`` 返回的行或 dict 列表。

    Returns:
        ``(translate, ambiguous)``；无命中时 ``(None, False)``。
    """
    if not rows:
        return None, False
    if len(rows) > 1:
        return None, True
    translate = getattr(rows[0], "translate", None) or rows[0].get("translate")
    return (str(translate).strip() if translate else None), False


def grep_retrieve_candidates(
    *,
    source_text: str,
    trie: Trie,
    lookup: Callable[[str], list[Any]],
) -> GrepRetrieveResult:
    """对 source_text 做整句 + Trie 词级 Grep lookup。

    Args:
        source_text: 待译词条原文。
        trie: 由 ``term_word`` approved 词表构建的 Trie。
        lookup: ``word -> rows`` 同步查表函数（已含 comment/department 过滤）。

    Returns:
        命中列表、是否整句 exact、整句译文及 ambiguous 词列表。
    """
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

    for word in unique_words(text, trie):
        if word == text:
            continue
        rows = lookup(word)
        translate, ambiguous = _pick_translate(rows)
        if ambiguous:
            ambiguous_words.append(word)
            hits.append(GrepHit(word=word, translate="", score=0.0, ambiguous=True))
        elif translate:
            hits.append(
                GrepHit(
                    word=word,
                    translate=translate,
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
