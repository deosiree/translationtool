"""compose_suggest — LLM 受约束拼装 prompt。"""

from __future__ import annotations

from app.graph.pre_translate.utils.decompose import Span
from app.shared.field_limits import LLM_REASONING_DETAIL_MAX, TRANSLATE_MAX


def build_compose_suggest_system_prompt(target_lang: str | None) -> str:
    """构造受约束拼装 system prompt。"""
    lang = target_lang or "目标语种"
    return f"""您是工业软件 i18n 专家。任务：将中文词条译为{lang}，
在「词片术语表」约束下输出自然、符合目标语习惯的短语/短句。

规则：
1. 词片术语表中每个译法必须出现（允许合理屈折/大小写，不可替换同义词）。
2. 目标语为空格分写语言（英文/俄文/法文/西文）：词与词之间加空格；按需补介词（of/for/in 等）。
3. 禁止简单拼接词片术语译法（如 File+System→FileSystem），除非术语表明确为品牌/ProductName。
4. 保留 %1、%2 等占位符位置与数量。
5. 未在术语表中的连接字（与/的/…）由你按语法翻译，勿保留中文。
6. 只输出 JSON，无 markdown。

输出：{{"translation":"...","reasoning":"...","terms_used":["..."]}}

字段约束（必须遵守）：
- translation：≤{TRANSLATE_MAX} 字符，目标语译文
- reasoning：≤{LLM_REASONING_DETAIL_MAX} 字符，简体中文一句话，说明用了哪些词片术语及连接词处理；禁止英文长段解释
- terms_used：可选，最多 20 项，每项 ≤64 字符
"""


def build_compose_suggest_user_message(
    *,
    source_text: str,
    target_lang: str | None,
    coverage: float,
    spans: list[dict],
    decomposed_translation: str | None,
) -> str:
    """构造 compose_suggest 用户消息。"""
    table_lines = ["| source_span | required_translation | status | use_llm |", "|---|---|---|---|"]
    notes_blocks: list[str] = []
    for raw in spans:
        text = str(raw.get("text") or "")
        translate = raw.get("translate")
        ambiguous = bool(raw.get("ambiguous"))
        use_llm = bool(raw.get("use_llm"))
        if ambiguous:
            status = "ambiguous"
            req = "(ambiguous — do not pick)"
        elif translate:
            status = "hit"
            req = str(translate)
        elif len(text) == 1 and text in "与的地和及":
            status = "glue"
            req = "(no glossary — translate as connector)"
        else:
            status = "oov"
            req = "(translate if possible)"
        table_lines.append(f"| {text} | {req} | {status} | {str(use_llm).lower()} |")
        desc = (raw.get("usage_notes") or "").strip()
        if translate and (desc or use_llm):
            domain = (raw.get("category") or "").strip()
            abbr = (raw.get("abbr") or "").strip()
            parts = [f"- 词片「{text}」→ {translate}"]
            if domain:
                parts.append(f"  领域: {domain}")
            if abbr:
                parts.append(f"  缩写: {abbr}")
            if use_llm:
                parts.append(
                    "  走LLM=true：不可纯直译替换，须结合指代/词性/分场景判断"
                )
            if desc:
                parts.append(f"  注意事项: {desc}")
            notes_blocks.append("\n".join(parts))

    span_term_table = "\n".join(table_lines)
    draft = decomposed_translation or ""
    notes_section = (
        "\n\n词片注意事项（必须遵守禁用译法与示例）:\n" + "\n".join(notes_blocks)
        if notes_blocks
        else ""
    )
    return (
        f"Chinese term: {source_text}\n"
        f"Target language: {target_lang or 'unknown'}\n"
        f"Coverage: {coverage:.0%}（词片命中比例，供你判断可信度）\n\n"
        f"词片术语表（必须全部使用）:\n{span_term_table}"
        f"{notes_section}\n\n"
        f"Naive draft（仅供参考，禁止照搬）: {draft}\n\n"
        f"Produce the best natural translation."
    )


def spans_from_state(spans_raw: list[dict] | None) -> list[Span]:
    """state spans dict → Span 列表（校验用）。"""
    result: list[Span] = []
    for raw in spans_raw or []:
        result.append(
            Span(
                text=str(raw.get("text") or ""),
                start=int(raw.get("start") or 0),
                end=int(raw.get("end") or 0),
                translate=raw.get("translate"),
                ambiguous=bool(raw.get("ambiguous")),
            )
        )
    return result
