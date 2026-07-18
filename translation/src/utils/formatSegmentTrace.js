/**
 * 切分轨迹展示 — segment_trace / segmentTrace → display 短串
 * @param {unknown} value - JSON 对象、JSON 字符串或已是 display
 * @returns {string}
 */
export function formatSegmentTraceDisplay(value) {
  if (value == null || value === "") return "-";
  if (typeof value === "object") {
    const display = value.display;
    return display != null && display !== "" ? String(display) : "-";
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) return "-";
    if (trimmed.startsWith("{")) {
      try {
        const parsed = JSON.parse(trimmed);
        if (parsed && typeof parsed === "object" && parsed.display != null) {
          return String(parsed.display) || "-";
        }
      } catch {
        /* 非 JSON，原样展示 */
      }
    }
    return trimmed;
  }
  return String(value);
}
