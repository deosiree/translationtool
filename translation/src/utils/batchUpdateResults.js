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
