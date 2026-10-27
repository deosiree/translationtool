/**
 * 解析 /entryInfo/updateEntryInfoList 的 per-row 响应。
 * 契约：data.list[{ id, success, message }] + data.totalNum
 *
 * @param {{ code?: number, data?: { list?: Array<{ id: string, success?: boolean, message?: string }>, totalNum?: number } }} res
 * @returns {{
 *   successIds: string[],
 *   failed: Array<{ id: string, message: string }>,
 *   successCount: number,
 *   failedCount: number,
 *   totalNum: number,
 * }}
 */
export function partitionBatchUpdateResults(res) {
  const list = Array.isArray(res?.data?.list) ? res.data.list : [];
  const successIds = [];
  const failed = [];

  for (const item of list) {
    if (!item || item.id == null) continue;
    if (item.success === true) {
      successIds.push(item.id);
    } else {
      failed.push({
        id: item.id,
        message: item.message || "保存失败",
      });
    }
  }

  const totalNum =
    typeof res?.data?.totalNum === "number" ? res.data.totalNum : list.length;

  return {
    successIds,
    failed,
    successCount: successIds.length,
    failedCount: failed.length,
    totalNum,
  };
}

/**
 * 将批量保存响应映射回本次提交的行，兼容逐条结果与旧版失败行列表。
 * @param {Array<Object>} submittedRows - 本次提交的行快照。
 * @param {Object} res - 批量保存接口响应。
 * @returns {{successRows: Array<Object>, failedRows: Array<Object>, successCount: number, failedCount: number}}
 *   成功行、失败行及其数量。
 */
export function partitionSubmittedRows(submittedRows, res) {
  // 先将调用方快照和接口列表规整为数组，避免异常响应影响保存后的界面反馈。
  const rows = Array.isArray(submittedRows) ? submittedRows : [];
  const resultList = Array.isArray(res?.data?.list) ? res.data.list : [];
  const hasPerRowResult = resultList.some(
    (item) => typeof item?.success === "boolean",
  );
  const failedCountFromResponse = Number(res?.data?.totalNum);

  if (hasPerRowResult) {
    // 新接口逐行给出成功标记：页面仅移除已确认成功的行，失败行继续留在原处修正。
    const { successIds, failed, failedCount } = partitionBatchUpdateResults(res);
    const successIdSet = new Set(successIds);
    const failedIdSet = new Set(failed.map((item) => item.id));
    const successRows = rows.filter((row) => successIdSet.has(row?.id));
    const failedRows = rows.filter((row) => failedIdSet.has(row?.id));
    return {
      successRows,
      failedRows,
      successCount: successRows.length,
      failedCount: Math.max(failedCount, failedRows.length),
    };
  }

  // 旧接口只返回失败行列表，未落入失败集合的提交快照视为保存成功。
  const failedIdSet = new Set(
    resultList.map((row) => row?.id).filter((id) => id != null),
  );
  const failedRows = rows.filter((row) => failedIdSet.has(row?.id));
  const failedCount = Number.isFinite(failedCountFromResponse)
    ? Math.max(failedCountFromResponse, failedRows.length)
    : failedRows.length;
  const successRows = rows.filter((row) => !failedIdSet.has(row?.id));
  return {
    successRows,
    failedRows,
    successCount: successRows.length,
    failedCount,
  };
}
