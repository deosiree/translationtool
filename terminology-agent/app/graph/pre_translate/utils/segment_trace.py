"""切分轨迹组装 — 供 write_result 落库与 agent_meta 透出。"""

from __future__ import annotations

from typing import Any

from app.shared.term_word.segment import segment_source_text

_SEGMENT_METHODS = frozenset({"grep", "hybrid", "decomposed"})


def used_segmentation(state: dict[str, Any]) -> bool:
    """是否走过 jieba/对齐切分路径（需落 segment_trace）。"""
    spans = state.get("spans") or []
    if spans:
        return True
    method = (state.get("retrieval_method") or "").strip()
    return method in _SEGMENT_METHODS


def build_segment_trace(state: dict[str, Any]) -> dict[str, Any] | None:
    """从 PreTranslate state 组装 segment_trace；未走过切分则返回 None。

    契约::
        {
          "jieba": [...],
          "aligned": [{text, translate, ambiguous, jieba_parts?}, ...],
          "display": "文件 | 与 | 系统"
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

    if not jieba_tokens and not aligned:
        return None

    display = " | ".join(jieba_tokens) if jieba_tokens else ""
    return {
        "jieba": jieba_tokens,
        "aligned": aligned,
        "display": display,
    }
