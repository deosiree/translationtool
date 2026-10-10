/**
 * 翻译语种列表模块级缓存。
 *
 * 语种列表在登录会话期间不变，但同屏多组件各自 fetch 会产生重复请求
 * （如工作台 mounted 与悬浮工具箱）。当前接入方：workbench/index.vue、
 * FloatingToolBox/index.vue；其余页面仍直连 getLanguage，可按需逐步迁移。
 * 统一走本缓存共享同一 Promise；请求失败时清空缓存句柄，下一次调用可重试。
 */
import { getLanguage } from "@/http/api/translate";

let languagePromise = null;

/** 获取语种列表（同会话内共享同一请求） */
export function fetchLanguages() {
  if (!languagePromise) {
    languagePromise = getLanguage({}).catch((err) => {
      languagePromise = null; // 失败不缓存，下次调用重新发起
      throw err;
    });
  }
  return languagePromise;
}

/** 仅测试用：清空语种缓存句柄，保证用例间隔离 */
export function __resetLanguageCacheForTest() {
  languagePromise = null;
}
