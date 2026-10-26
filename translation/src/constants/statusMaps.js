/**
 * 从状态映射表中读取原始状态定义。
 * @param {Record<string, {label: string, color: string}>} statusMap - 状态映射表。
 * @param {number|string|null|undefined} value - 原始状态码。
 * @returns {{label: string, color: string}|undefined} 匹配的状态定义。
 */
function getStatusDefinition(statusMap, value) {
  return statusMap[value == null ? "" : String(value)];
}

/**
 * 将指定状态码转换为下拉选项。
 * @param {Record<string, {label: string, color: string}>} statusMap - 状态映射表。
 * @param {string[]} codes - 需展示的状态码顺序。
 * @returns {{label: string, value: string}[]} 下拉选项。
 */
function createStatusOptions(statusMap, codes) {
  return codes.map((value) => ({
    label: statusMap[value].label,
    value,
  }));
}

/** 存在状态定义。 */
export const IS_EXIST_STATUS_MAP = Object.freeze({
  0: Object.freeze({ label: "新建", color: "#6BB8FF" }),
  1: Object.freeze({ label: "已存在", color: "#FBB31F" }),
});

/** 词条审核状态定义。 */
export const ENTRY_STATE_STATUS_MAP = Object.freeze({
  "-1": Object.freeze({ label: "禁用", color: "#9a9a9a" }),
  0: Object.freeze({ label: "新建", color: "#6BB8FF" }),
  1: Object.freeze({ label: "审核中", color: "#FBB31F" }),
  2: Object.freeze({ label: "审核不通过", color: "#ff0000" }),
  3: Object.freeze({ label: "已审核", color: "#36BF7D" }),
});

/** 翻译审核状态定义。 */
export const TRANSLATE_STATE_STATUS_MAP = Object.freeze({
  0: Object.freeze({ label: "未翻译", color: "#6BB8FF" }),
  1: Object.freeze({ label: "待审核", color: "#FBB31F" }),
  2: Object.freeze({ label: "审核不通过", color: "#ff0000" }),
  3: Object.freeze({ label: "已审核", color: "#36BF7D" }),
});

/**
 * 获取存在状态定义。
 * @param {number|string|null|undefined} value - 原始存在状态码。
 * @returns {{label: string, color: string}|undefined} 匹配的存在状态定义。
 */
export function getIsExistStatus(value) {
  return getStatusDefinition(IS_EXIST_STATUS_MAP, value);
}

/**
 * 获取词条审核状态定义。
 * @param {number|string|null|undefined} value - 原始词条审核状态码。
 * @returns {{label: string, color: string}|undefined} 匹配的词条审核状态定义。
 */
export function getEntryStateStatus(value) {
  return getStatusDefinition(ENTRY_STATE_STATUS_MAP, value);
}

/**
 * 获取翻译审核状态定义。
 * @param {number|string|null|undefined} value - 原始翻译审核状态码。
 * @returns {{label: string, color: string}|undefined} 匹配的翻译审核状态定义。
 */
export function getTranslateStateStatus(value) {
  return getStatusDefinition(TRANSLATE_STATE_STATUS_MAP, value);
}

/**
 * 获取存在状态标签。
 * @param {number|string|null|undefined} value - 原始存在状态码。
 * @returns {string} 状态标签；未知状态返回空字符串。
 */
export function getIsExistLabel(value) {
  return getIsExistStatus(value)?.label ?? "";
}

/**
 * 获取词条审核状态标签。
 * @param {number|string|null|undefined} value - 原始词条审核状态码。
 * @returns {string} 状态标签；未知状态返回空字符串。
 */
export function getEntryStateLabel(value) {
  return getEntryStateStatus(value)?.label ?? "";
}

/**
 * 获取翻译审核状态标签。
 * @param {number|string|null|undefined} value - 原始翻译审核状态码。
 * @returns {string} 状态标签；未知状态回退为“未翻译”。
 */
export function getTranslateStateLabel(value) {
  return (
    getTranslateStateStatus(value)?.label ??
    TRANSLATE_STATE_STATUS_MAP["0"].label
  );
}

/**
 * 获取词条审核状态下拉选项。
 * @param {boolean} [includeForbidden=false] - 是否包含禁用状态。
 * @returns {{label: string, value: string}[]} 保持字符串值的下拉选项。
 */
export function getEntryStateOptions(includeForbidden = false) {
  const codes = includeForbidden
    ? ["0", "1", "2", "3", "-1"]
    : ["0", "1", "2", "3"];
  return createStatusOptions(ENTRY_STATE_STATUS_MAP, codes);
}

/**
 * 获取翻译审核状态下拉选项。
 * @returns {{label: string, value: string}[]} 保持字符串值的下拉选项。
 */
export function getTranslateStateOptions() {
  return createStatusOptions(TRANSLATE_STATE_STATUS_MAP, ["0", "1", "2", "3"]);
}
