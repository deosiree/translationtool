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
