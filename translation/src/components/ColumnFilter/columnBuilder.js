/**
 * @module columnBuilder
 * @description 列定义 → Ant Design Table columns（单轨 buildCol）
 */
import { resolvePresetCols, defaultSelectionFromCols } from "./colPreset.js";

/**
 * 从 ctx 解析当前语种 transMap（工作台动态列）
 * @param {Object} [ctx] buildCol 上下文
 * @returns {Object|null}
 */
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

/**
 * 解析列的 dataIndex（支持工作台语种动态映射）
 * @param {import('./colPreset.js').ColDef} def 列定义
 * @param {Object} ctx buildCol 上下文
 * @returns {string}
 */
function resolveDataIndex(def, ctx) {
  if (def.dataIndex) return def.dataIndex;
  const resolver = wbDataIndexResolvers[def.value];
  return resolver ? resolver(ctx) : def.value;
}

/**
 * 将列宽转为 CSS 尺寸（数字补 px）
 * @param {number|string|undefined} width
 * @returns {string|undefined}
 */
function toCssSize(width) {
  if (width == null || width === "") return undefined;
  return typeof width === "number" ? `${width}px` : String(width);
}

/**
 * 锁定单元格 width/min-width/max-width，并关闭原生 title
 * @param {Object} col Ant Table 列配置
 * @returns {Object}
 */
export function applyLockCellSize(col) {
  if (!col) return col;
  col.ellipsis = { showTitle: false };
  col.customCell = () => {
    const w = toCssSize(col.width);
    return { style: { width: w, minWidth: w, maxWidth: w } };
  };
  col.customHeaderCell = () => {
    const w = toCssSize(col.width);
    return { style: { width: w, minWidth: w, maxWidth: w } };
  };
  return col;
}

/**
 * 构建序号列（含分页偏移的 customRender）
 * @param {import('./colPreset.js').ColDef} def 列定义
 * @param {Object} ctx 含 pagination 的上下文
 * @param {boolean} [lockCellSize=false] 是否锁定单元格宽
 * @returns {Object} Ant Table 列配置
 */
function buildIndexCol(def, ctx, lockCellSize = false) {
  const pagination = ctx?.pagination || { pageSize: 20, current: 1 };
  const col = {
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
  return lockCellSize ? applyLockCellSize(col) : col;
}

/**
 * 为列附加 customFilterDropdown / filters（needFilter 为 true 时）
 * @param {Object} col Ant Table 列配置
 * @param {boolean} needFilter 是否启用筛选
 */
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
}

/**
 * 按列 value 附加 fixed、sorter、filters 等业务行为
 * @param {Object} col Ant Table 列配置
 * @param {import('./colPreset.js').ColDef} def 列定义
 * @param {boolean} needFilter 是否启用列头筛选
 */
function applyValueBehaviors(col, def, needFilter) {
  const v = def.value;

  if (["entry", "entryState", "index"].includes(v)) {
    col.fixed = col.fixed || "left";
  }
  if (v === "operation") {
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
  if (v === "isExist") {
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
 * 将单列定义转为 Ant Table 列配置
 * @param {import('./colPreset.js').ColDef} def 列定义
 * @param {Object} ctx buildCol 上下文
 * @param {number} [normalWidth=100] 默认列宽
 * @param {boolean} [needFilter=false] 是否启用列头筛选
 * @param {boolean} [lockCellSize=false] 是否锁定单元格宽并关闭原生 title
 * @returns {Object} Ant Table 列配置（含 colValue）
 */
export function buildCol(
  def,
  ctx,
  normalWidth = 100,
  needFilter = false,
  lockCellSize = false
) {
  if (def.value === "index") {
    return buildIndexCol(def, ctx, lockCellSize);
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
  return lockCellSize ? applyLockCellSize(col) : col;
}

/**
 * 工作台：按当前语种排除与 translate/interpretation 重复的 optional 列
 * @param {import('./colPreset.js').ColDef[]} cols 解析后的列定义
 * @param {Object} ctx 含 task.transMap 的上下文
 * @returns {import('./colPreset.js').ColDef[]}
 */
export function filterWbColsForCtx(cols, ctx) {
  const tm = getTransMap(ctx);
  const dynExcl = new Set([tm?.value, tm?.interpretation].filter(Boolean));
  return cols.filter((c) => c.required || !dynExcl.has(c.value));
}

/**
 * 由 allCols + preset 生成 columnSettingsList 与默认 columns
 * @param {import('./colPreset.js').ColDef[]} allCols 列全集
 * @param {import('./colPreset.js').ColPreset} preset 页级 preset
 * @param {Object} ctx buildCol 上下文
 * @param {number} [normalWidth=100] 默认列宽
 * @param {boolean} [needFilter=false] 是否启用列头筛选
 * @param {Function|null} [filterCols] 二次过滤函数 (resolvedCols, ctx) => cols
 * @param {boolean} [lockCellSize=false] 是否锁定单元格宽并关闭原生 title
 * @returns {{ columnSettingsList: Array, columns: Array }}
 */
export function buildTable(
  allCols,
  preset,
  ctx,
  normalWidth = 100,
  needFilter = false,
  filterCols = null,
  lockCellSize = false
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
    .map((def) => buildCol(def, ctx, normalWidth, needFilter, lockCellSize))
    .sort((a, b) => a.index - b.index);

  return { columnSettingsList, columns };
}
