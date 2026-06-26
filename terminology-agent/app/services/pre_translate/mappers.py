"""PreTranslate 响应映射 — graph state 与 API agent_meta 互转。"""

from __future__ import annotations

from typing import Any


def map_graph_state_to_agent_meta(final: dict[str, Any]) -> dict[str, Any]:
    """将 PreTranslateGraph 终态映射为 API 契约的 agent_meta 六字段。

    Args:
        final: ``PreTranslateGraph.run()`` 返回的 state dict。

    Returns:
        含 confidence / review_status / suggested_translation / similar_terms /
        retrieval_method / reasoning 的 dict。
    """
    reasoning = final.get("llm_reasoning") or ""
    return {
        "confidence": final.get("confidence"),
        "review_status": final.get("review_status"),
        "suggested_translation": final.get("suggested_translation"),
        "similar_terms": final.get("similar_terms") or [],
        "retrieval_method": final.get("retrieval_method"),
        "reasoning": reasoning,
    }


def guess_lang_field(entry: dict, target_lang: str | None) -> str | None:
    """从词条 dict 推断目标语字段名（如 russian、english）。

    Args:
        entry: 工作台词条 dict。
        target_lang: 目标语种（当前未用于匹配，保留扩展）。

    Returns:
        目标语字段 key；无法推断时返回 None。
    """
    _ = target_lang
    skip_keys = {
        "id",
        "entry",
        "parentID",
        "translate",
        "agent_meta",
        "chineseInterpretation",
        "englishInterpretation",
        "comment",
        "abbr",
        "tag",
        "translateState",
    }
    for key in entry:
        if key in skip_keys:
            continue
        if isinstance(entry.get(key), str):
            return key
    return None


def apply_auto_approved_translation(
    result_item: dict,
    *,
    suggested: str | None,
    lang_key: str | None,
) -> dict:
    """auto_approved 时将建议译文回填到词条 dict。

    Args:
        result_item: 含 agent_meta 的词条结果 dict（会被原地修改）。
        suggested: 建议译文。
        lang_key: 目标语字段名。

    Returns:
        回填后的 result_item。
    """
    if lang_key and suggested:
        result_item[lang_key] = suggested
    if suggested:
        result_item["translate"] = suggested
    return result_item
