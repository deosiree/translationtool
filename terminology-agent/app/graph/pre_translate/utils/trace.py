"""PreTranslate trace 工具 — 测试与 devtools Demo 共用。"""

from __future__ import annotations

from typing import Any

from app.graph.pre_translate.runner import PreTranslateGraph


def build_pretranslate_trace_steps(
    final_state: dict,
    threshold: float,
) -> list[dict[str, Any]]:
    """根据 PreTranslateGraph 终态构建可读 trace 摘要。

    Args:
        final_state: 图执行完毕后的 state dict。
        threshold: 用户置信度阈值，用于推断 review_status。

    Returns:
        原 trace 步骤 + 末尾 Summary 步骤。
    """
    confidence = final_state.get("confidence") or 0.0
    review_status = final_state.get("review_status") or (
        "auto_approved" if confidence >= threshold else "needs_human"
    )
    steps: list[dict[str, Any]] = list(final_state.get("trace") or [])
    steps.append(
        {
            "stage": "Summary",
            "retrieval_method": final_state.get("retrieval_method"),
            "translation_source": final_state.get("translation_source"),
            "confidence": confidence,
            "threshold": threshold,
            "route": review_status,
            "suggested_translation": final_state.get("suggested_translation"),
            "llm_reasoning": final_state.get("llm_reasoning"),
        }
    )
    return steps


async def collect_pretranslate_trace(
    session,
    *,
    source_entry: str,
    target_lang: str | None,
    department: str | None,
    confidence_threshold: float,
) -> list[dict[str, Any]]:
    """对单条词条跑 PreTranslateGraph，返回 trace 步骤列表。

    Args:
        session: SQLAlchemy AsyncSession。
        source_entry: 源词条文本。
        target_lang: 目标语种。
        department: 部门/可视范围。
        confidence_threshold: 置信度阈值。

    Returns:
        build_pretranslate_trace_steps 产出的步骤列表。
    """
    final = await PreTranslateGraph().run(
        source_text=source_entry,
        target_lang=target_lang,
        department=department,
        confidence_threshold=confidence_threshold,
        session=session,
    )
    return build_pretranslate_trace_steps(final, confidence_threshold)


def format_astream_events(events: list[dict[str, Any]]) -> str:
    """将 LangGraph astream_events 事件列表格式化为可读文本。

    Args:
        events: astream_events 返回的事件 dict 列表。

    Returns:
        每行 ``[event] name`` 的多行文本；空列表返回 ``(no events)``。
    """
    lines: list[str] = []
    for event in events:
        kind = event.get("event", "unknown")
        name = event.get("name", "")
        lines.append(f"[{kind}] {name}")
    return "\n".join(lines) if lines else "(no events)"
