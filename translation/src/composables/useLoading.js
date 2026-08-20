/**
 * 通用 loading 状态管理（全局单一引用计数）
 *
 * 本项目 loading 即各 UI 元素的局部遮罩（表格 :loading、按钮 :loading / :okLoading），
 * 且模态框互斥（同一时刻只有一个操作），因此用全局单例计数：
 * - startLoading() +1、endLoading() -1，归 0 才表示没有任务需要遮罩；
 * - 多个任务可叠加（嵌套校验/保存等），全部结束才恢复；
 * - 导出响应式 loading 供模板直接绑定（组件 setup() 返回即可），
 *   无 key、无 vm、无 UI 耦合，业务层不再散落 loading 变量。
 */
import { ref, computed } from 'vue';

// 全局唯一引用计数
const pending = ref(0);

/** 全局 loading 是否激活（响应式只读，模板绑定用） */
export const loading = computed(() => pending.value > 0);

/** 开始一个 loading 任务（计数 +1） */
export function startLoading() {
  pending.value++;
}

/** 结束一个 loading 任务（计数 -1，不低于 0） */
export function endLoading() {
  if (pending.value > 0) pending.value--;
}

/** 同步读取当前是否处于 loading（供逻辑判断；模板可直接用 loading） */
export function isLoading() {
  return pending.value > 0;
}

/** 兜底复位（异常/调试场景清空计数） */
export function resetLoading() {
  pending.value = 0;
}

/**
 * 包裹异步任务：start → task → finally end（保证计数不泄漏）
 * @param {Function} task - 异步任务（可为 async）
 * @returns {Promise<*>} 任务返回值
 */
export async function withLoading(task) {
  startLoading();
  try {
    return await task();
  } finally {
    endLoading();
  }
}
