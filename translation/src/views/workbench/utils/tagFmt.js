/**
 * 工作台流水线专用：表格 tag 列浏览态格式化
 * 落点 views/workbench/utils
 */

/**
 * 将 tag 字符串按中英文分号拆分为数组
 * @param {string|null} message 原始 tag 文本
 * @returns {string[]} 非空片段列表；空值返回 []
 */
export function companyCut(message) {
  if (message === null || message === "") {
    return [];
  }
  const regex = /[;；]/;
  return message
    .split(regex)
    .filter((item) => item !== "");
}

/**
 * tag 列展示：分号片段合并为「; 」分隔的单行文本
 * @param {string|null} text 原始 tag 文本
 * @returns {string} 格式化后的展示文本
 */
export function formatTagText(text) {
  return companyCut(text).join("; ");
}
