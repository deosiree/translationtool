/**
 * @file 按分页接口拉全量：与具体业务 API / Vue 解耦，仅依赖 fetchPage 约定。
 * @module utils/fetchAllByPaging
 */

import { mergeSelectedEntriesById } from "@/utils/selectionUtils";

/**
 * 由总数与页大小计算总页数（至少为 0）。
 *
 * @param {number} total - 总条数
 * @param {number} pageSize - 每页条数（须 > 0）
 * @returns {number} 总页数；total/pageSize 非法时返回 0
 */
export function calcPageCount(total, pageSize) {
  const t = Number(total);
  const ps = Number(pageSize);
  if (!Number.isFinite(t) || t <= 0 || !Number.isFinite(ps) || ps <= 0) {
    return 0;
  }
  return Math.ceil(t / ps);
}

/**
 * 按分页接口拉全量并按 id 去重合并。
 *
 * @description
 * 先请求第 1 页取得 `total`，再顺序请求剩余页；不绑定具体 HTTP 客户端。
 * `fetchPage` 须返回 `{ list, total }`（字段可缺省，缺省按空列表 / 0 处理）。
 * 不设条数上限，由调用方承担大数据量耗时与内存成本。
 *
 * @param {(page: number, pageSize: number) => Promise<{ list?: Array<{id?: string|number}>, total?: number }>} fetchPage
 *   分页拉取函数；`page` 从 1 起
 * @param {{ pageSize?: number }} [options]
 * @param {number} [options.pageSize=100] - 每页条数
 * @returns {Promise<Array<{id?: string|number}>>} 按 id 去重后的全量行
 * @throws {Error} `fetchPage` 非函数、pageSize 非法，或某一页请求失败（透传原错误）
 *
 * @example
 * const rows = await fetchAllByPaging(
 *   (page, pageSize) => api.list({ page, pageSize }).then((r) => ({
 *     list: r.data.list,
 *     total: r.data.total,
 *   }))
 * );
 */
export async function fetchAllByPaging(fetchPage, options = {}) {
  if (typeof fetchPage !== "function") {
    throw new Error("fetchAllByPaging: fetchPage 须为函数");
  }

  const pageSize = options.pageSize ?? 100;
  const ps = Number(pageSize);
  if (!Number.isFinite(ps) || ps <= 0) {
    throw new Error("fetchAllByPaging: pageSize 须为正数");
  }

  const first = (await fetchPage(1, ps)) || {};
  const firstList = Array.isArray(first.list) ? first.list : [];
  const total = Number(first.total) || 0;

  if (total <= 0) {
    return mergeSelectedEntriesById([], firstList);
  }

  let merged = mergeSelectedEntriesById([], firstList);
  const pages = calcPageCount(total, ps);
  for (let page = 2; page <= pages; page += 1) {
    const res = (await fetchPage(page, ps)) || {};
    const list = Array.isArray(res.list) ? res.list : [];
    merged = mergeSelectedEntriesById(merged, list);
  }
  return merged;
}
