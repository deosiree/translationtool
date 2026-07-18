/**
 * 术语字典「正则-切片 / 正则-翻译」、术语词典「正则-词条 / 正则-翻译」共用预设
 *（label 中文，value 为 MySQL REGEXP）。
 */
export const TERM_WORD_REGEX_PRESETS = [
  { label: "全部中文", value: "^[\\x{4e00}-\\x{9fff}]+$" },
  { label: "包含英文", value: "[A-Za-z]" },
  { label: "包含特殊符号（如换行符）", value: "[[:cntrl:]]" },
  { label: "纯数字", value: "^[0-9]+$" },
];
