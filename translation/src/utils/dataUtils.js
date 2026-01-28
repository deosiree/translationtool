/**
 * 数据存储工具函数
 * 包含 localStorage 操作等功能
 */
import { entryParams } from "@/constants/commonParam";

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
