/**
 * 统一解析 segment_trace 值：兼容 string/object 两种入参
 * @param {unknown} value - segment_trace 字段值
 * @returns {object|null} 解析后的对象，失败/空时返回 null
 */
export function parseSegmentTrace(value) {
  if (value == null) return null;

  if (typeof value === "object") {
    return value;
  }

  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) return null;
    if (trimmed.startsWith("{")) {
      try {
        const parsed = JSON.parse(trimmed);
        if (parsed && typeof parsed === "object") return parsed;
      } catch {
        /* 非 JSON 字符串 */
      }
    }
    return null;
  }

  return null;
}
