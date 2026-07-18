/**
 * 可搜索下拉：预设可 label≠value；手写追加必须 label===value。
 */

/**
 * @typedef {{ label: string, value: string }} SelectOption
 */

/**
 * 深拷贝预设 options（基座列表）。
 * @param {SelectOption[]} presets
 * @returns {SelectOption[]}
 */
export function clonePresetOptions(presets) {
  return (presets || []).map((o) => ({
    label: o.label,
    value: o.value,
  }));
}

/**
 * 在基座 options 上追加手写项：label 与 value 均为 typed。
 * 若 typed 为空、或已作为 value 存在，则返回基座副本（不追加）。
 * @param {SelectOption[]} baseOptions
 * @param {string} typed
 * @returns {SelectOption[]}
 */
export function appendTypedOption(baseOptions, typed) {
  const base = clonePresetOptions(baseOptions);
  const text = typed == null ? "" : String(typed);
  if (!text) {
    return base;
  }
  if (base.some((o) => o.value === text)) {
    return base;
  }
  return base.concat([{ label: text, value: text }]);
}
