"""PreTranslate — LLM 机翻 prompt 模板。"""

from app.graph.pre_translate.state import PreTranslateState
from app.shared.field_limits import LLM_REASONING_DETAIL_MAX, TRANSLATE_MAX


def build_pre_translate_system_prompt(target_lang: str | None) -> str:
    """构造面向目标语种的 LLM 系统 prompt。

    Args:
        target_lang: 目标语种名称；None 时使用「目标语种」占位。

    Returns:
        含 Guidelines 与 JSON 输出格式的系统 prompt 字符串。
    """
    lang = target_lang or "目标语种"
    return f"""您是工业自动化软件的专业国际化翻译专家（中文→{lang}）。

任务：将中文词条翻译为{lang}，输出简洁、准确的技术译文。

Guidelines:
1. 保留 %1、%2 等占位符不变。
2. 使用标准工业软件/UI 术语，不要意译过度。
3. 只输出 JSON，不要 markdown 代码块。

Output format (JSON):
{{"translation": "...", "reasoning": "..."}}

字段约束（必须遵守）：
- translation：≤{TRANSLATE_MAX} 字符，目标语译文
- reasoning：≤{LLM_REASONING_DETAIL_MAX} 字符，简体中文一句话，说明检索/译法依据；禁止英文长段解释
"""


def build_pre_translate_user_message(state: PreTranslateState) -> str:
    """根据 state 构造 LLM 用户消息。

    Args:
        state: 当前 PreTranslateState，使用 source_text / target_lang / similar_terms。

    Returns:
        含源词条、目标语与术语参考的多行用户消息。
    """
    similar = state.get("similar_terms") or []
    refs = "\n".join(
        f"- {t.get('entry', '')} → {t.get('translate', '')}"
        for t in similar[:3]
    )
    ref_block = refs if refs else "（术语库无参考）"
    return (
        f"Chinese term: {state['source_text']}\n"
        f"Target language: {state.get('target_lang') or 'unknown'}\n"
        f"Reference terms from glossary:\n{ref_block}\n"
        f"Provide the best translation."
    )
