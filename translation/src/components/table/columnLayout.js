/**
 * @fileoverview 表格列的语义宽度档位与自适应列识别。
 *
 * 页面只需声明字段语义，具体宽度由这里统一维护，避免不同表格对同类字段使用不同尺寸。
 */

const BASE_LAYOUTS = {
  cell: { width: 100, minWidth: 50, align: "center", resizable: true },
  selection: { width: 48, minWidth: 48, align: "center", resizable: false },
  index: { width: 56, minWidth: 50, align: "center", resizable: true },
  status: { width: 96, minWidth: 50, align: "center", resizable: true },
  language: { width: 88, minWidth: 50, align: "center", resizable: true },
  type: { width: 96, minWidth: 50, align: "center", resizable: true },
  tag: { width: 128, minWidth: 50, align: "center", resizable: true },
  abbr: { width: 100, minWidth: 50, align: "left", resizable: true },
  person: { width: 96, minWidth: 50, align: "center", resizable: true },
  version: { width: 120, minWidth: 50, align: "center", resizable: true },
  date: { width: 104, minWidth: 50, align: "center", resizable: true },
  dateTime: { width: 168, minWidth: 50, align: "center", resizable: true },
  shortText: { width: 120, minWidth: 50, align: "left", resizable: true },
  entry: {
    width: 220,
    minWidth: 50,
    align: "left",
    resizable: true,
    adaptive: true,
  },
  translation: {
    width: 260,
    minWidth: 50,
    align: "left",
    resizable: true,
    adaptive: true,
  },
  note: {
    width: 180,
    minWidth: 50,
    align: "left",
    resizable: true,
    adaptive: true,
  },
  comment: {
    width: 180,
    minWidth: 50,
    align: "center",
    resizable: true,
    adaptive: true,
  },
  source: {
    width: 200,
    minWidth: 50,
    align: "left",
    resizable: true,
    adaptive: true,
  },
  operation: { width: 96, minWidth: 50, align: "center", resizable: true },
};

const LAYOUT_ALIASES = {
  index: "index",
  entry: "entry",
  source: "source",
  entrySource: "source",
  sourceFile: "source",
  diFileName: "source",
  translate: "translation",
  translation: "translation",
  chinese: "translation",
  english: "translation",
  russian: "translation",
  spanish: "translation",
  french: "translation",
  interpretation: "note",
  meaning: "note",
  comment: "comment",
  remark: "note",
  auditSuggest: "note",
  auditSuggess: "note",
  description: "shortText",
  taskDescription: "note",
  taskName: "shortText",
  product: "shortText",
  productName: "shortText",
  classfy1: "shortText",
  classfy2: "shortText",
  tag: "tag",
  abbr: "abbr",
  audit: "person",
  operation: "operation",
  translateState: "status",
  entryState: "status",
  isExist: "status",
  type: "type",
  translateType: "language",
  language: "language",
  creator: "person",
  operator: "person",
  auditor: "person",
  entryAuditor: "person",
  productVersion: "version",
  versionName: "version",
  branch: "shortText",
  createTime: "dateTime",
  updateTime: "dateTime",
  operateTime: "dateTime",
  deliveryTime: "date",
};

/**
 * 根据列语义返回统一的宽度配置。
 * @param {string} kind 列语义或字段标识。
 * @param {Object} [overrides={}] 页面级覆盖配置。
 * @returns {Object} 可直接合并到 Ant Design Vue 列定义的配置。
 */
export function getColumnLayout(kind, overrides = {}) {
  const layoutName = LAYOUT_ALIASES[kind] || kind;
  return { ...(BASE_LAYOUTS[layoutName] || BASE_LAYOUTS.cell), ...overrides };
}

/**
 * 为手写列定义附加统一宽度配置。
 * @param {Object} column 原始列定义。
 * @param {string} kind 列语义或字段标识。
 * @param {Object} [overrides={}] 页面级覆盖配置。
 * @returns {Object} 带有统一宽度、对齐与拖拽属性的列定义。
 */
export function applyColumnLayout(column, kind, overrides = {}) {
  return { ...column, ...getColumnLayout(kind, overrides) };
}

/**
 * 提取显式声明为自适应的叶子列，供 TableViewport 分配剩余空间。
 * @param {Array} columns Ant Design Vue 列定义。
 * @returns {string[]} 自适应列的稳定标识列表。
 */
export function getAdaptiveColumnKeys(columns) {
  const result = [];
  const visit = (items) => {
    (items || []).forEach((column) => {
      if (column.children?.length) {
        visit(column.children);
        return;
      }
      if (!column.adaptive) return;
      const key = column.colValue ?? column.dataIndex ?? column.key;
      if (key != null) result.push(String(key));
    });
  };
  visit(columns);
  return [...new Set(result)];
}

export { BASE_LAYOUTS as TABLE_COLUMN_LAYOUTS };
