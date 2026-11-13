/**
 * @module columnBuilder
 * @description 列定义 → Ant Design Table columns（单轨 buildCol）
 */
import { resolvePresetCols, defaultSelectionFromCols } from "./colPreset.js";
import { getColumnLayout } from "@/components/table/columnLayout.js";
import { getIsExistLabel } from "@/constants/statusMaps.js";

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
 * 构建序号列（含分页偏移的 customRender）
 * @param {import('./colPreset.js').ColDef} def 列定义
 * @param {Object} ctx 含 pagination 的上下文
 * @returns {Object} Ant Table 列配置
 */
function buildIndexCol(def, ctx) {
  const pagination = ctx?.pagination || { pageSize: 20, current: 1 };
  const col = {
    ...getColumnLayout("index"),
    title: def.label,
    dataIndex: "index",
    align: "center",
    width: def.width ?? getColumnLayout("index").width,
    fixed: "left",
    resizable: def.resizable ?? getColumnLayout("index").resizable,
    adaptive: def.adaptive ?? getColumnLayout("index").adaptive,
    index: def.index,
    colValue: def.value,
    customRender: (text) =>
      text.index + 1 + pagination.pageSize * (pagination.current - 1),
  };
  return col;
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
 * 比较两个翻译值，并保持空值在升序时置前。
 * @param {*} leftValue 左侧翻译值
 * @param {*} rightValue 右侧翻译值
 * @returns {number} 字典序比较结果
 */
function compareTranslationValues(leftValue, rightValue) {
  const leftEmpty = leftValue == null || leftValue === "";
  const rightEmpty = rightValue == null || rightValue === "";

  if (leftEmpty) return rightEmpty ? 0 : -1;
  if (rightEmpty) return 1;
  return String(leftValue).localeCompare(String(rightValue));
}

/**
 * 构建词条管理翻译列排序器，编辑态在升降序下都保持置顶。
 * @param {string} dataIndex 翻译字段名
 * @param {Object} editableData 当前编辑行草稿映射
 * @returns {Function} Ant Design Vue 表格排序比较器
 */
function createEntryTranslationSorter(dataIndex, editableData) {
  /**
   * 先比较编辑状态，再比较当前有效翻译值。
   * @param {Object} left 左侧词条
   * @param {Object} right 右侧词条
   * @param {string} sortOrder 表格排序方向
   * @returns {number} Ant Design Vue 使用的比较结果
   */
  return function compareEntryTranslationRows(left, right, sortOrder) {
    const leftDraft = editableData?.[left?.id];
    const rightDraft = editableData?.[right?.id];
    const leftEditing = Boolean(leftDraft);
    const rightEditing = Boolean(rightDraft);

    if (leftEditing !== rightEditing) {
      const editPriority = leftEditing ? -1 : 1;
      return sortOrder === "descend" ? -editPriority : editPriority;
    }

    const leftValue = leftEditing
      ? leftDraft?.[dataIndex]
      : left?.[dataIndex];
    const rightValue = rightEditing
      ? rightDraft?.[dataIndex]
      : right?.[dataIndex];
    return compareTranslationValues(leftValue, rightValue);
  };
}

/**
 * 按列 value 附加 fixed、sorter、filters 等业务行为
 * @param {Object} col Ant Table 列配置
 * @param {import('./colPreset.js').ColDef} def 列定义
 * @param {boolean} needFilter 是否启用列头筛选
 * @param {Object} ctx buildCol 上下文
 */
function applyValueBehaviors(col, def, needFilter, ctx) {
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
  const translationSort = ctx?.translationSort;
  const isEntryTranslationColumn = translationSort?.fields?.includes(v);
  if (v === "translate" || isEntryTranslationColumn) {
    col.sorter = isEntryTranslationColumn
      ? createEntryTranslationSorter(
          col.dataIndex,
          translationSort.editableData
        )
      : (a, b) =>
          compareTranslationValues(a?.[col.dataIndex], b?.[col.dataIndex]);
    col.sortDirections = ["ascend", "descend"];
  }
  if (v === "isExist") {
    col.filteredValue = null;
    col.filters = [
      { text: getIsExistLabel(1), value: 1 },
      { text: getIsExistLabel(0), value: 0 },
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
 * @param {Object} ctx buildCol 上下文；词条翻译排序可含 translationSort.fields 与 editableData
 * @param {number} [normalWidth=100] 默认列宽
 * @param {boolean} [needFilter=false] 是否启用列头筛选
 * @returns {Object} Ant Table 列配置（含 colValue）
 */
export function buildCol(
  def,
  ctx,
  normalWidth = 100,
  needFilter = false
) {
  if (def.value === "index") {
    return buildIndexCol(def, ctx);
  }

  const layout = getColumnLayout(def.layout || def.value);
  const col = {
    ...layout,
    title: def.label,
    dataIndex: resolveDataIndex(def, ctx),
    align: def.align ?? layout.align ?? "center",
    width: def.width ?? layout.width ?? normalWidth,
    minWidth: def.minWidth ?? layout.minWidth,
    ellipsis: true,
    resizable: def.resizable ?? layout.resizable ?? true,
    adaptive: def.adaptive ?? layout.adaptive,
    index: def.index,
    colValue: def.value,
  };

  applyValueBehaviors(col, def, needFilter, ctx);
  return col;
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
 * @returns {{ columnSettingsList: Array, columns: Array }}
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
    minWidth: c.minWidth,
    align: c.align,
    resizable: c.resizable,
    adaptive: c.adaptive,
    layout: c.layout,
    fixed: c.fixed,
    ...(c.dataIndex ? { dataIndex: c.dataIndex } : {}),
  }));

  const defaultValues = defaultSelectionFromCols(columnSettingsList);
  const defMap = new Map(filtered.map((c) => [c.value, c]));
  const columns = defaultValues
    .map((v) => defMap.get(v))
    .filter(Boolean)
    .map((def) =>
      buildCol(def, ctx, normalWidth, needFilter)
    )
    .sort((a, b) => a.index - b.index);

  return { columnSettingsList, columns };
}
