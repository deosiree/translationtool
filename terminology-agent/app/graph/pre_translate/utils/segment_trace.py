"""切分轨迹组装 — 供 write_result 落库与 agent_meta 透出。"""

from __future__ import annotations

from typing import Any

from app.shared.term_word.segment import segment_source_text
from app.shared.term_word.stopwords import normalize_cn_lexemes

_SEGMENT_METHODS = frozenset({"grep", "hybrid", "decomposed"})


def used_segmentation(state: dict[str, Any]) -> bool:
    """是否走过 jieba/对齐切分路径（需落 segment_trace）。"""
    spans = state.get("spans") or []
    if spans:
        return True
    method = (state.get("retrieval_method") or "").strip()
    return method in _SEGMENT_METHODS


def finalize_jieba_tokens(tokens: list[str]) -> tuple[list[str], str]:
    """写入 segment_trace 前的词片门禁：normalize → (jieba, display)。"""
    jieba = normalize_cn_lexemes(tokens)
    display = " | ".join(jieba) if jieba else ""
    return jieba, display


def normalize_segment_trace_dict(trace: dict[str, Any] | None) -> dict[str, Any] | None:
    """对已有 segment_trace 的 jieba/display 走同一门禁；其它键原样保留。"""
    if trace is None:
        return None
    if not isinstance(trace, dict):
        return trace
    jieba, display = finalize_jieba_tokens(
        [str(t) for t in (trace.get("jieba") or [])]
    )
    out = dict(trace)
    out["jieba"] = jieba
    out["display"] = display
    return out


def build_segment_trace(state: dict[str, Any]) -> dict[str, Any] | None:
    """从 PreTranslate state 组装 segment_trace；未走过切分则返回 None。

    契约::
        {
          "jieba": [...],
          "aligned": [{text, translate, ambiguous, jieba_parts?}, ...],
          "display": "文件 | 系统"
        }
    """
    if not used_segmentation(state):
        return None

    spans = state.get("spans") or []
    jieba_tokens: list[str] = []
    aligned: list[dict[str, Any]] = []

    if spans:
        for span in spans:
            if not isinstance(span, dict):
                continue
            parts = span.get("jieba_parts")
            if isinstance(parts, (list, tuple)) and parts:
                jieba_tokens.extend(str(p) for p in parts)
            else:
                text = span.get("text")
                if text:
                    jieba_tokens.append(str(text))
            item: dict[str, Any] = {
                "text": span.get("text"),
                "translate": span.get("translate"),
                "ambiguous": bool(span.get("ambiguous")),
            }
            if isinstance(parts, (list, tuple)) and parts:
                item["jieba_parts"] = list(parts)
            aligned.append(item)
    else:
        source = (state.get("source_text") or "").strip()
        if source:
            jieba_tokens = [tok for tok, _s, _e in segment_source_text(source)]

    jieba_tokens, display = finalize_jieba_tokens(jieba_tokens)

    if not jieba_tokens and not aligned:
        return None

    return {
        "jieba": jieba_tokens,
        "aligned": aligned,
        "display": display,
    }
