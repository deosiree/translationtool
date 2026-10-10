/**
 * 工作台流水线专用：行内编辑列判定（TableCellTextArea）
 * 落点 views/workbench/utils
 */

/**
 * 返回使用 TableCellTextArea 的列 dataIndex 列表（排除专用输入列）
 * @param {Object} vm 含 editList_needValidate / editList
 * @param {{ withEditList?: boolean }} [opts] translate 阶段传 withEditList: false，不含 editList
 * @returns {string[]} 需 TextArea 编辑的列名
 */
export function editTextCols(vm, { withEditList = true } = {}) {
  const dedicatedInputCols = ["diFileName", "tag", "maxLength"];
  const cols = [...(vm.editList_needValidate || [])];
  if (withEditList) {
    cols.push(...(vm.editList || []));
  }
  return cols.filter((col) => !dedicatedInputCols.includes(col));
}
