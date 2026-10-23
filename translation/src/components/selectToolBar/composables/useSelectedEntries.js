/** @fileoverview 提供按唯一行 ID 合并、更新、移除和清空选择清单的操作。 */
/**
 * 按 ID 合并选择行，后续行覆盖相同 ID 的缓存对象。
 * @param {Array<Object>} existingRows - 当前已选行。
 * @param {Array<Object>} incomingRows - 新增或更新的行。
 * @returns {Array<Object>} 按 ID 去重后的行。
 */
export function mergeSelectedEntriesById(existingRows, incomingRows) {
  const rowsById = new Map();
  (existingRows || []).forEach((row) => {
    if (row && row.id != null) rowsById.set(row.id, row);
  });
  (incomingRows || []).forEach((row) => {
    if (row && row.id != null) rowsById.set(row.id, row);
  });
  return [...rowsById.values()];
}

/**
 * 从选择清单移除指定 ID 的行。
 * @param {Array<Object>} existingRows - 当前已选行。
 * @param {Array<Object|number|string>} removedRows - 要移除的行或 ID。
 * @returns {Array<Object>} 移除后的行。
 */
export function removeSelectedEntriesById(existingRows, removedRows) {
  const removedIds = new Set(
    (removedRows || []).map((row) =>
      row && typeof row === "object" ? row.id : row
    )
  );
  return (existingRows || []).filter((row) => !removedIds.has(row?.id));
}

/**
 * 对选择清单执行按 ID 的全量合并或移除。
 * @param {Array<Object>} selectedRows - 当前已选行。
 * @param {Array<Object>} resultRows - 本次操作覆盖的完整结果行。
 * @param {boolean} selected - true 合并，false 移除。
 * @returns {Array<Object>} 操作后的选择清单。
 */
export function applySelectionById(selectedRows, resultRows, selected) {
  return selected
    ? mergeSelectedEntriesById(selectedRows, resultRows)
    : removeSelectedEntriesById(selectedRows, resultRows);
}

/**
 * 提供选择清单共用的按 ID 数据操作。
 * @returns {{merge: Function, update: Function, remove: Function, apply: Function, clear: Function}} 选择数据操作集合。
 */
export function useSelectedEntries() {
  /**
   * 将一条最新行对象合并到现有清单。
   * @param {Array<Object>} rows - 当前已选行。
   * @param {Object} row - 最新行对象。
   * @returns {Array<Object>} 更新后的清单。
   */
  function update(rows, row) {
    return mergeSelectedEntriesById(rows, [row]);
  }

  /**
   * 创建空的选择清单。
   * @returns {Array<Object>} 空清单。
   */
  function clear() {
    return [];
  }

  return {
    merge: mergeSelectedEntriesById,
    update,
    remove: removeSelectedEntriesById,
    apply: applySelectionById,
    clear,
  };
}
