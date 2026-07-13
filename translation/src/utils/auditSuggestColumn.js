import commonParam from "@/constants/commonParam.js";

/**
 * 判断表格列是否为「审核意见」列（含动态语种字段与归档 auditSuggess）
 * @param {{ colValue?: string, dataIndex?: string }|null|undefined} column
 * @returns {boolean}
 */
export function isAuditSuggestColumn(column) {
  return (
    column?.colValue === "auditSuggest" ||
    commonParam.langAudSugList.includes(column?.dataIndex) ||
    column?.dataIndex === "auditSuggess"
  );
}
