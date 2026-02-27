/**
 * 数据存储工具函数
 * 包含 localStorage 操作、行数据规范化等
 */
import { entryParams } from "@/constants/commonParam";

/**
 * 将行数据中可能绑定到 Input/TextArea 的原始值转为字符串，避免 value 类型告警（Expected String, got Number 等）
 * @param {Object} row - 行对象（浅拷贝后仅处理顶层）
 * @returns {Object} 新对象，number/boolean 已转为 string，其余保持
 */
export function normalizeEditableRow(row) {
  if (!row || typeof row !== "object") return row;
  const out = { ...row };
  for (const key of Object.keys(out)) {
    const v = out[key];
    if (v != null && typeof v === "object" && typeof v !== "function") continue;
    if (typeof v === "number" || typeof v === "boolean") out[key] = String(v);
  }
  return out;
}

/**
 * 获取缓存的 i18nUrl
 * @returns {string|null} 缓存的 i18nUrl，如果不存在则返回 null
 */
export function getCachedI18nUrl() {
  const key = entryParams.updateEntry.localStorageKey?.i18nUrl;
  if (!key) {
    return null;
  }
  const i18nUrl = localStorage.getItem(key);
  // console.log("getCachedI18nUrl", i18nUrl);
  return i18nUrl;
}

/**
 * 设置缓存的 i18nUrl
 * @param {string} i18nUrl - 要缓存的 i18nUrl
 */
export function setCachedI18nUrl(i18nUrl) {
  const key = entryParams.updateEntry.localStorageKey?.i18nUrl;
  if (!key) {
    return;
  }
  if (i18nUrl) {
    localStorage.setItem(key, i18nUrl);
  } else {
    localStorage.removeItem(key);
  }
}
