/**
 * @module columnTable
 * @description 展示列单轨运行时：applyTable / localStorage 偏好 / changeColumn
 */
import { buildCol, buildTable } from "./columnBuilder.js";
import { defaultSelectionFromCols } from "./colPreset.js";

/** @typedef {Object} ColumnFilterPref applyTable 写入 vm 的列偏好配置 */
/** @property {string} colPrefName localStorage 键名 */
/** @property {number} normalWidth 默认列宽 */
/** @property {boolean} needFilter 是否启用列头筛选行为 */

const TABLE_HOST_MAX_DEPTH = 8;

/**
 * 从组件实例向上查找含 columnSettingsList 与 columns 的表格 host vm
 * @param {Object|null|undefined} startVm 起始 Vue 实例（通常为 ColumnFilter.$parent）
 * @param {number} [maxDepth=8] 最大向上层数
 * @returns {Object|null} 表格 host vm，未找到则 null
 */
export function findTableHost(startVm, maxDepth = TABLE_HOST_MAX_DEPTH) {
  let current = startVm;
  let depth = 0;
  while (current && depth < maxDepth) {
    if (
      Array.isArray(current.columnSettingsList) &&
      Array.isArray(current.columns)
    ) {
      return current;
    }
    current = current.$parent;
    depth += 1;
  }
  return null;
}

/**
 * 返回 columnSettingsList 中 required 列的 value 列表
 * @param {Array<{ value: string, required?: boolean }>} columnSettingsList
 * @returns {string[]}
 */
export function getRequiredColumnValues(columnSettingsList) {
  return columnSettingsList.filter((c) => c.required).map((c) => c.value);
}

/**
 * 合并必选列与用户勾选的可选列，并按 index 排序
 * @param {string[]} selected 用户勾选的 value 列表（可含可选列）
 * @param {Array<{ value: string, required?: boolean, index?: number }>} columnSettingsList
 * @returns {string[]}
 */
export function mergeColumnSelection(selected, columnSettingsList) {
  const required = getRequiredColumnValues(columnSettingsList);
  const optionalSelected = (selected || []).filter(
    (v) => !required.includes(v)
  );
  const merged = [...new Set([...required, ...optionalSelected])];
  const indexMap = new Map(
    columnSettingsList.map((c) => [c.value, c.index ?? 0])
  );
  return merged.sort(
    (a, b) => (indexMap.get(a) ?? 0) - (indexMap.get(b) ?? 0)
  );
}

/**
 * 从 merged 勾选列表中剥离必选列，得到仅存 localStorage 的可选部分
 * @param {string[]} selected
 * @param {Array<{ value: string, required?: boolean }>} columnSettingsList
 * @returns {string[]}
 */
export function getOptionalColumnSelection(selected, columnSettingsList) {
  const required = new Set(getRequiredColumnValues(columnSettingsList));
  return (selected || []).filter((v) => !required.has(v));
}

/**
 * 仅持久化勾选偏好（不改表格 columns），用于展示条件等场景
 * @param {string} colPrefName localStorage 键名
 * @param {string[]} mergedSelection 合并后的勾选 value 列表
 * @param {Array<{ value: string, required?: boolean }>} columnSettingsList
 */
export function persistSelectionPref(
  colPrefName,
  mergedSelection,
  columnSettingsList
) {
  const optional = getOptionalColumnSelection(
    mergedSelection,
    columnSettingsList
  );
  localStorage.setItem(
    colPrefName,
    JSON.stringify({ displayColumn: optional.join(",") })
  );
}

export { defaultSelectionFromCols as getDefaultColumnSelection } from "./colPreset.js";

/**
 * 裁剪 vm.columns，保留 mergedSelection 中的列及所有必选列
 * @param {Object} vm 表格 Vue 实例
 * @param {string[]} mergedSelection 合并后的勾选 value 列表
 * @param {Array<{ value: string, required?: boolean }>} columnSettingsList
 */
export function pruneColumnsToSelection(vm, mergedSelection, columnSettingsList) {
  const requiredValues = new Set(getRequiredColumnValues(columnSettingsList));
  const allowed = new Set(mergedSelection);
  vm.columns = vm.columns.filter(
    (col) => requiredValues.has(col.colValue) || allowed.has(col.colValue)
  );
}

/**
 * 根据用户勾选更新 columns、checkedColumn，并写入 localStorage
 * @param {string} colPrefName localStorage 键名
 * @param {number} normalWidth 动态增列时的默认宽度
 * @param {string[]} colPref_strList 用户勾选的 value 列表
 * @param {Object} vm 表格 Vue 实例（含 columns、checkedColumn、colBuildCtx）
 * @param {boolean} [needFilter=false] 是否启用列头筛选
 * @param {Array} [columnSettingsList] 列定义；缺省用 vm.columnSettingsList
 */
export function changeColumn(
  colPrefName,
  normalWidth,
  colPref_strList,
  vm,
  needFilter = false,
  columnSettingsList
) {
  const defs = columnSettingsList || vm.columnSettingsList;
  if (!defs?.length) return;

  const mergedSelection = mergeColumnSelection(colPref_strList, defs);
  const optionalSelection = getOptionalColumnSelection(mergedSelection, defs);

  if (vm.checkedColumn) vm.checkedColumn = mergedSelection;

  defs.forEach((def) => {
    if (def.required) return;
    const checkedIndex = mergedSelection.indexOf(def.value);
    const nowColumnIndex = vm.columns.findIndex(
      (item) => item.colValue === def.value
    );
    if (
      (nowColumnIndex !== -1 && checkedIndex !== -1) ||
      (nowColumnIndex === -1 && checkedIndex === -1)
    ) {
      return;
    }
    if (nowColumnIndex === -1 && checkedIndex !== -1) {
      const newCol = buildCol(
        def,
        vm.colBuildCtx,
        normalWidth,
        needFilter
      );
      vm.columns.splice(-1, 0, newCol);
    }
    if (nowColumnIndex !== -1 && checkedIndex === -1) {
      vm.columns.splice(nowColumnIndex, 1);
    }
  });

  pruneColumnsToSelection(vm, mergedSelection, defs);
  vm.columns.sort((a, b) => a.index - b.index);

  localStorage.setItem(
    colPrefName,
    JSON.stringify({ displayColumn: optionalSelection.join(",") })
  );
}

/**
 * 从 localStorage 读取列偏好并应用到 vm
 * @param {string} colPrefName localStorage 键名
 * @param {number} normalWidth 列宽
 * @param {Object} vm 表格 Vue 实例
 * @param {boolean} [needFilter=false] 是否启用列头筛选
 * @param {Array} [columnSettingsList] 列定义；缺省用 vm.columnSettingsList
 */
export function getColPref(
  colPrefName,
  normalWidth,
  vm,
  needFilter = false,
  columnSettingsList
) {
  const defs = columnSettingsList || vm.columnSettingsList;
  if (!defs?.length) return;

  const storedPreferences = localStorage.getItem(colPrefName);
  const validValues = new Set(defs.map((item) => item.value));
  let colPref_strList;

  if (storedPreferences === null) {
    colPref_strList = defaultSelectionFromCols(defs);
  } else {
    colPref_strList = JSON.parse(storedPreferences).displayColumn
      .split(",")
      .filter(Boolean)
      .filter((val) => validValues.has(val));
  }

  changeColumn(
    colPrefName,
    normalWidth,
    colPref_strList,
    vm,
    needFilter,
    defs
  );
}

/**
 * 表格列单轨初始化：buildTable + 读取 localStorage 偏好
 * @param {Object} vm Vue 实例
 * @param {Object} options
 * @param {import('./colPreset.js').ColDef[]} options.allCols 列全集
 * @param {import('./colPreset.js').ColPreset} options.preset 页级 preset
 * @param {Object} options.ctx buildCol 上下文（pagination、task 等）
 * @param {string} options.colPrefName localStorage 键名
 * @param {number} [options.normalWidth=100] 默认列宽
 * @param {boolean} [options.needFilter=false] 是否启用列头筛选
 * @param {Function} [options.filterCols] 二次过滤 columnSettingsList 的函数
 */
export function applyTable(vm, options) {
  const {
    allCols,
    preset,
    ctx,
    colPrefName,
    normalWidth = 100,
    needFilter = false,
    filterCols = null,
  } = options;

  vm.colBuildCtx = ctx;
  const { columnSettingsList, columns } = buildTable(
    allCols,
    preset,
    ctx,
    normalWidth,
    needFilter,
    filterCols
  );
  vm.columnSettingsList = columnSettingsList;
  vm.columns = columns;
  vm.$columnFilterPref = { colPrefName, normalWidth, needFilter };
  getColPref(colPrefName, normalWidth, vm, needFilter, columnSettingsList);
}

/**
 * 从 localStorage 读展示列偏好并投影到 vm.columns / checkedColumn（页面 @change 时调用）
 * @param {Object} vm 含 $columnFilterPref、columnSettingsList、columns 的表格页实例
 */
export function syncColumnsFromPref(vm) {
  const pref = vm?.$columnFilterPref;
  if (!pref?.colPrefName || !vm?.columnSettingsList?.length) return;
  getColPref(
    pref.colPrefName,
    pref.normalWidth,
    vm,
    pref.needFilter,
    vm.columnSettingsList,
  );
}
