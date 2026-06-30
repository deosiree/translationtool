/**
 * 列 preset 合并工具（展示列 SSOT 的运行时解析）
 */

/**
 * @typedef {Object} ColDef 列定义
 * @property {string} label 展示名（ColumnFilter、表头 title）
 * @property {string} value 逻辑列 ID，popover、localStorage 中使用
 * @property {number} index 排序
 * @property {boolean} [required] 是否必选，不可取消
 * @property {boolean} [visible] 是否可见，false 时默认不勾选
 * @property {boolean} [hidden] 是否隐藏，true 时该 preset 列列表不含此列；allCols 可标注默认 hidden，preset defaults/ovrd 可覆盖
 * @property {number} [width] 列宽度
 * @property {string} [fixed] 列固定
 * @property {string} [dataIndex] 列数据索引
 */

/**
 * @typedef {Object} ColOvrd 列覆盖
 * @property {string} [label] 列标签
 * @property {string} [value] 列值
 * @property {boolean} [required] 是否必选
 * @property {boolean} [visible] 是否可见
 * @property {boolean} [hidden] 是否隐藏
 * @property {number} [width] 列宽度
 * @property {string} [fixed] 列固定
 * @property {string} [dataIndex] 列数据索引
 */

/**
 * @typedef {Object} ColPreset 列预设
 * @property {ColOvrd[]} ovrd 页级覆盖项
 * @property {Partial<ColDef>|null} defaults 批量默认覆盖
 */

/**
 * 合并列全集与页级覆盖
 * @param {ColDef[]} allCols 列全集
 * @param {ColOvrd[]} [ovrd] 页级覆盖
 * @param {Partial<ColDef>|null} [defaults] 批量默认覆盖
 * @returns {ColDef[]} 合并后的列定义列表
 */
export function mergeColPresets(allCols, ovrd = [], defaults = null) {
  const byValue = new Map();
  const byLabel = new Map();
  for (const o of ovrd) {
    if (o.value != null) byValue.set(o.value, o);
    if (o.label != null) byLabel.set(o.label, o);
  }
  return allCols
    .map((base) => {
      const patch = byLabel.get(base.label) ?? byValue.get(base.value);
      let merged;
      if (!patch) {
        merged = { ...base, ...(defaults || {}) };
      } else {
        const { label: _label, value: _value, ...patchRest } = patch;
        merged = { ...base, ...(defaults || {}), ...patchRest };
      }
      if (merged.hidden === true) return null;
      return merged;
    })
    .filter(Boolean);
}

/**
 * 列定义 → 导出/字段选择用的 { label, value, index }
 * @param {ColDef[]} cols 列定义列表
 * @returns {{ label: string, value: string, index: number }[]} 导出/字段选择用的 { label, value, index }
 */
export function colsToFieldOptions(cols) {
  return (cols || []).map(({ label, value, index }) => ({ label, value, index }));
}

/**
 * 解析 preset 为完整列定义列表
 * @param {ColPreset} preset 列预设
 * @param {ColDef[]} allCols 列全集
 * @returns {ColDef[]} 解析后的列定义列表
 */
export function resolvePresetCols(preset, allCols) {
  return mergeColPresets(allCols, preset?.ovrd ?? [], preset?.defaults ?? null);
}

/**
 * 根据列定义计算默认勾选的 value 列表
 * @param {ColDef[]} cols 列定义列表
 * @returns {string[]} 默认勾选的 value 列表
 */
export function defaultSelectionFromCols(cols) {
  return (cols || [])
    .filter((c) => c.required || c.visible !== false)
    .map((c) => c.value);
}
