/**
 * 表格字段列头的统一说明文案。
 */
const TABLE_HEADER_HINTS = Object.freeze({
  tag: "供开发人员定位词条",
  comment: "供翻译人员定位词条的使用场景",
});

/**
 * 获取表格列对应的列头提示。
 * @param {Object} column Ant Design Vue 列定义。
 * @returns {string} 匹配到的提示文案，未匹配时返回空字符串。
 */
export function getTableHeaderHint(column) {
  const key = column?.dataIndex ?? column?.key ?? column?.colValue;
  return TABLE_HEADER_HINTS[String(key)] || "";
}

export { TABLE_HEADER_HINTS };
