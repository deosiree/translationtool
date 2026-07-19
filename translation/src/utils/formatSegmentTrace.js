import { parseSegmentTrace } from "./parseSegmentTrace.js";

/**
 * 切分轨迹展示 — segment_trace / segmentTrace → display 短串
 * @param {unknown} value - JSON 对象、JSON 字符串或已是 display
 * @returns {string}
 */
export function formatSegmentTraceDisplay(value) {
  const parsed = parseSegmentTrace(value);
  if (parsed) {
    const display = parsed.display;
    return display != null && display !== "" ? String(display) : "-";
  }
  if (value == null || value === "") return "-";
  return String(value);
}
