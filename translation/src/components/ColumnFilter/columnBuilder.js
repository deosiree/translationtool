/**
 * 列定义 → Ant Design Table columns（单轨 buildCol）
 */
import { resolvePresetCols, defaultSelectionFromCols } from "./colPreset.js";

function getTransMap(ctx) {
  return ctx?.transMap || ctx?.task?.transMap || ctx?.language || null;
}

/** 逻辑 value → 动态 dataIndex（工作台语种列） */
const wbDataIndexResolvers = {
  translateState: (ctx) => getTransMap(ctx)?.state || "translateState",
  translate: (ctx) => getTransMap(ctx)?.value || "translate",
  interpretation: (ctx) => getTransMap(ctx)?.interpretation || "interpretation",
  auditSuggest: (ctx) => getTransMap(ctx)?.auditSuggest || "auditSuggest",
};

function resolveDataIndex(def, ctx) {
  if (def.dataIndex) return def.dataIndex;
  const resolver = wbDataIndexResolvers[def.value];
  return resolver ? resolver(ctx) : def.value;
}

function buildIndexCol(def, ctx) {
  const pagination = ctx?.pagination || { pageSize: 20, current: 1 };
  return {
    title: def.label,
    dataIndex: "index",
    align: "center",
    width: def.width ?? 50,
    fixed: "left",
    index: def.index,
    colValue: def.value,
    customRender: (text) =>
      text.index + 1 + pagination.pageSize * (pagination.current - 1),
  };
}

function applyFilterBehaviors(col, needFilter) {
  if (!needFilter) return;

  if (col.dataIndex === "entrySource") {
    col.customFilterDropdown = true;
    col.filteredValue = null;
    col.onFilter = (filterValue, record) => {
      const cellValue = record[col.dataIndex];
      if (cellValue === null || cellValue === undefined) return false;
      return cellValue
        .toString()
        .toLowerCase()
        .includes(filterValue.toLowerCase());
    };
  }

  if (col.dataIndex === "entry") {
    col.customFilterDropdown = true;
    col.filteredValue = null;
    col.onFilter = (filterValue, record) => {
      const cellValue = record[col.dataIndex];
      if (cellValue === null || cellValue === undefined) return false;
      return cellValue.toString() === filterValue;
    };
  }

  if (col.dataIndex === "isExist") {
    col.customFilterDropdown = true;
    col.filteredValue = null;
    col.filters = [
      { text: "已存在", value: 1 },
      { text: "新建", value: 0 },
    ];
    col.onFilter = (filterValue, record) => record.isExist === filterValue;
  }
}

function applyValueBehaviors(col, def, needFilter) {
  const v = def.value;

  if (["entry", "entryState", "index"].includes(v)) {
    col.fixed = col.fixed || "left";
  }
  if (["operation", "abbr"].includes(v)) {
    col.fixed = col.fixed || "right";
  }

  if (v === "entry") {
    col.fixed = "left";
    if (col.sorter == null) {
      col.sorter = (a, b) => a.entry.localeCompare(b.entry);
      col.sortDirections = ["ascend", "descend"];
    }
  }
  if (v === "translate") {
    col.sorter = (a, b) => a.entry.localeCompare(b.entry);
    col.sortDirections = ["ascend", "descend"];
  }
  if (v === "isExist" && !needFilter) {
    col.filteredValue = null;
    col.filters = [
      { text: "已存在", value: 1 },
      { text: "新建", value: 0 },
    ];
    col.onFilter = (value, record) => record.isExist === value;
  }
  if (v === "operation") {
    col.fixed = "right";
    col.width = def.width ?? col.width ?? 130;
  }
  if (def.fixed) col.fixed = def.fixed;

  applyFilterBehaviors(col, needFilter);
}

/**
 * 将列定义转为 Ant Table 列配置
 * @param {import('./colPreset.js').ColDef} def
 * @param {Object} ctx
 * @param {number} [normalWidth]
 * @param {boolean} [needFilter]
 */
export function buildCol(def, ctx, normalWidth = 100, needFilter = false) {
  if (def.value === "index") {
    return buildIndexCol(def, ctx);
  }

  const col = {
    title: def.label,
    dataIndex: resolveDataIndex(def, ctx),
    align: "center",
    width: def.width ?? normalWidth,
    ellipsis: true,
    resizable: true,
    index: def.index,
    colValue: def.value,
  };

  applyValueBehaviors(col, def, needFilter);
  return col;
}

/**
 * 工作台：按当前语种排除与 translate/interpretation 重复的 optional 列
 * @param {import('./colPreset.js').ColDef[]} cols
 * @param {Object} ctx
 */
export function filterWbColsForCtx(cols, ctx) {
  const tm = getTransMap(ctx);
  const dynExcl = new Set([tm?.value, tm?.interpretation].filter(Boolean));
  return cols.filter((c) => c.required || !dynExcl.has(c.value));
}

/**
 * 由 allCols + preset 生成 columnSettingsList 与默认 columns
 * @param {import('./colPreset.js').ColDef[]} allCols
 * @param {import('./colPreset.js').ColPreset} preset
 * @param {Object} ctx
 * @param {number} [normalWidth]
 * @param {boolean} [needFilter]
 * @param {Function} [filterCols]
 */
export function buildTable(
  allCols,
  preset,
  ctx,
  normalWidth = 100,
  needFilter = false,
  filterCols = null
) {
  const resolved = resolvePresetCols(preset, allCols);
  const filtered = filterCols ? filterCols(resolved, ctx) : resolved;

  const columnSettingsList = filtered.map((c) => ({
    label: c.label,
    value: c.value,
    index: c.index,
    required: !!c.required,
    visible: c.visible,
    width: c.width,
    fixed: c.fixed,
    ...(c.dataIndex ? { dataIndex: c.dataIndex } : {}),
  }));

  const defaultValues = defaultSelectionFromCols(columnSettingsList);
  const defMap = new Map(filtered.map((c) => [c.value, c]));
  const columns = defaultValues
    .map((v) => defMap.get(v))
    .filter(Boolean)
    .map((def) => buildCol(def, ctx, normalWidth, needFilter))
    .sort((a, b) => a.index - b.index);

  return { columnSettingsList, columns };
}
