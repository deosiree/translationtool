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

/**
 * 读取整份「完成后自动写库」开关 map
 *
 * 存储形态为单键 JSON map（`{ [classifyID]: true }`），避免按 classifyID 建键导致 key 膨胀。
 * 解析失败（人为篡改、旧格式残留）时降级为空 map，不抛错阻断右键菜单。
 * @returns {Object} classifyID 到 boolean 的映射；无缓存或解析失败时为 `{}`
 */
function readAutoWriteMap() {
  const key = entryParams.updateEntry.localStorageKey?.autoWrite;
  if (!key) {
    return {};
  }
  const raw = localStorage.getItem(key);
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    // 仅接受普通对象，数组与标量一律视为脏数据
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      return parsed;
    }
    return {};
  } catch (e) {
    return {};
  }
}

/**
 * 读取指定分类的「完成后自动写库」开关
 * @param {string} classifyID - 词条分类 ID（树节点 key）
 * @returns {boolean} 是否已勾选自动写库
 */
export function getAutoWrite(classifyID) {
  if (!classifyID) {
    return false;
  }
  return readAutoWriteMap()[classifyID] === true;
}

/**
 * 写入指定分类的「完成后自动写库」开关
 *
 * 传入假值时从 map 中删除该分类（而非存 false），map 清空后一并移除整个 localStorage 键，
 * 避免残留空对象。写库成功后的「清除记忆」即调用本函数传 false。
 * @param {string} classifyID - 词条分类 ID（树节点 key）
 * @param {boolean} enabled - 是否勾选自动写库
 */
export function setAutoWrite(classifyID, enabled) {
  const key = entryParams.updateEntry.localStorageKey?.autoWrite;
  if (!key || !classifyID) {
    return;
  }
  const map = readAutoWriteMap();
  if (enabled) {
    map[classifyID] = true;
  } else {
    delete map[classifyID];
  }
  if (Object.keys(map).length === 0) {
    localStorage.removeItem(key);
  } else {
    localStorage.setItem(key, JSON.stringify(map));
  }
}

