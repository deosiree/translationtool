/**
 * 表格单元格展示文本（浏览态省略 / Tooltip 内容）
 */

/**
 * 词条列：把真实换行转成可见的 \\n
 * @param {*} text
 * @returns {string}
 */
export function formatEntryText(text) {
  if (text == null || text === "") return "";
  return String(text).replace(/\n/g, "\\n");
}

/**
 * 普通单元格：统一成字符串
 * @param {*} text
 * @returns {string}
 */
export function formatCellText(text) {
  if (text == null || text === "") return "";
  return String(text);
}

/**
 * 翻译最大长度：0 / "0" / 空视为无限制，浏览态留空
 * @param {*} text
 * @returns {string}
 */
export function formatMaxLengthText(text) {
  if (text == null || text === "") return "";
  const n = Number(text);
  if (!Number.isFinite(n) || n <= 0) return "";
  return String(text);
}
