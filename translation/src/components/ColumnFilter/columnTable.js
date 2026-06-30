/**
 * 展示列单轨运行时：applyTable / localStorage 偏好 / changeColumn
 */
import { buildCol, buildTable } from "./columnBuilder.js";
import { defaultSelectionFromCols } from "./colPreset.js";

export function getRequiredColumnValues(columnSettingsList) {
  return columnSettingsList.filter((c) => c.required).map((c) => c.value);
}

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

export function getOptionalColumnSelection(selected, columnSettingsList) {
  const required = new Set(getRequiredColumnValues(columnSettingsList));
  return (selected || []).filter((v) => !required.has(v));
}

export { defaultSelectionFromCols as getDefaultColumnSelection } from "./colPreset.js";

/**
 * 裁剪表格列，确保 columns 与 mergedSelection 一致
 */
export function pruneColumnsToSelection(vm, mergedSelection, columnSettingsList) {
  const requiredValues = new Set(getRequiredColumnValues(columnSettingsList));
  const allowed = new Set(mergedSelection);
  vm.columns = vm.columns.filter(
    (col) => requiredValues.has(col.colValue) || allowed.has(col.colValue)
  );
}

/**
 * 根据用户勾选更新 columns 并保存 localStorage
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
 * 从 localStorage 读取列偏好
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

  if (storedPreferences) {
    colPref_strList = JSON.parse(storedPreferences).displayColumn
      .split(",")
      .filter(Boolean)
      .filter((val) => validValues.has(val));
  }

  if (!colPref_strList?.length) {
    colPref_strList = defaultSelectionFromCols(defs);
  }

  if (colPref_strList.length === 0) return;
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
 * 表格列单轨初始化
 * @param {Object} vm - Vue 实例
 * @param {Object} options
 * @param {import('./colPreset.js').ColDef[]} options.allCols
 * @param {import('./colPreset.js').ColPreset} options.preset
 * @param {Object} options.ctx
 * @param {string} options.colPrefName
 * @param {number} [options.normalWidth]
 * @param {boolean} [options.needFilter]
 * @param {Function} [options.filterCols]
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
  getColPref(colPrefName, normalWidth, vm, needFilter, columnSettingsList);
}
