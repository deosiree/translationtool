/**
 * @module ColumnFilter
 * @description 展示列 SSOT 模块：preset 合并、表格列构建、localStorage 偏好、ColumnFilter UI
 *
 * 公共 API：
 * - {@link ColumnFilter} 展示列 popover 组件
 * - {@link applyTable} / {@link changeColumn} / {@link getColPref} 表格列运行时
 * - {@link mergeColPresets} / {@link resolvePresetCols} preset 解析
 * - {@link buildCol} / {@link buildTable} 列定义 → Ant Table columns
 */
export { columnFilterOverlayStyle } from "./overlayStyle.js";
export { default as ColumnFilter } from "./ColumnFilter.vue";
export {
  applyTable,
  changeColumn,
  getColPref,
  findTableHost,
  persistSelectionPref,
} from "./columnTable.js";
export {
  mergeColPresets,
  resolvePresetCols,
  defaultSelectionFromCols,
  colsToFieldOptions,
} from "./colPreset.js";
export { buildCol, buildTable, filterWbColsForCtx } from "./columnBuilder.js";
export {
  mergeColumnSelection,
  getDefaultColumnSelection,
} from "./columnTable.js";
