/**
 * 将空值及序列化产生的空 ID 字符串规范为缺省值。
 * @param {string|number|null|undefined} value 待检查的 ID。
 * @returns {string|number|undefined} 有效 ID；缺省时返回 undefined。
 */
export function normalizeOptionalID(value) {
  if (value == null) return undefined;
  if (typeof value !== "string") return value;

  const normalizedValue = value.trim().toLowerCase();
  if (
    !normalizedValue ||
    normalizedValue === "null" ||
    normalizedValue === "undefined"
  ) {
    return undefined;
  }

  return value;
}
