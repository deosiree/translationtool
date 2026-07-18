"""术语字典 Excel 列契约 — 导入/导出/模板共用。"""

from __future__ import annotations

from typing import Final

USE_LLM_TIP: Final[str] = (
    "是否需要走LLM判断，非直译，而是含有指代、有词性、需分场景判断等"
)

# 表头顺序固定；勿改列名（导入按表头匹配）
HEADERS: Final[tuple[str, ...]] = (
    "词片",
    "翻译",
    "翻译类型",
    "可见范围",
    "comment",
    "领域",
    "缩写",
    "走LLM",
    "使用场景与注意事项",
    "翻译状态",
)

HEADER_TO_FIELD: Final[dict[str, str]] = {
    "词片": "word",
    "翻译": "translate",
    "翻译类型": "target_lang",
    "可见范围": "department",
    "comment": "comment",
    "领域": "category",
    "缩写": "abbr",
    "走LLM": "use_llm",
    "使用场景与注意事项": "usage_notes",
    "翻译状态": "status",
}

STATUS_LABEL_TO_CODE: Final[dict[str, str]] = {
    "0": "0",
    "1": "1",
    "2": "2",
    "3": "3",
    "未翻译": "0",
    "待审核": "1",
    "审核不通过": "2",
    "已审核": "3",
}

STATUS_CODE_TO_LABEL: Final[dict[str, str]] = {
    "0": "未翻译",
    "1": "待审核",
    "2": "审核不通过",
    "3": "已审核",
}

# 源「通用术语」sheet 表头 → 标准字段
NOTES_SHEET_NAME: Final[str] = "通用术语"
NOTES_HEADER_MAP: Final[dict[str, str]] = {
    "类型": "category",
    "中文": "word",
    "英文（若需缩写，全称作为Tips）": "translate",
    "英文": "translate",
    "英文缩写": "abbr",
    "伪代码术语(有指代、有词性、需分场景判断等)": "use_llm",
    "备注（使用场景、中文解释、长度限制、易错翻译、使用示例）": "usage_notes",
    "备注": "usage_notes",
}
