/** @fileoverview 管理全量选择操作的即时投影、操作顺序、结果应用与失效。 */
import {
  mergeSelectedEntriesById,
  removeSelectedEntriesById,
} from "./useSelectedEntries";

/**
 * 管理待处理全量操作的即时投影、用户操作顺序与结果回滚。
 * @returns {Object} 开始、投影、完成、失败和失效操作的接口。
 */
export function useSelectionOperations() {
  let sequence = 0;
  let generation = 0;
  const pendingOperations = new Map();
  const latestById = new Map();
  const refreshedRowsById = new Map();

  const guard = {
    revision: 0,
    /**
     * 登记一次异步全量选择或取勾。
     * @param {string} scope - 查询范围稳定标识。
     * @param {boolean} selected - 是否选择该范围。
     * @returns {Object} 待完成的操作令牌。
     */
    begin(scope, selected) {
      this.revision += 1;
      const operation = { id: ++sequence, generation, scope, selected };
      pendingOperations.set(operation.id, operation);
      return operation;
    },
    /**
     * 记录用户对单行或父子行的后续操作。
     * @param {string|number} id - 稳定行 ID。
     * @param {boolean} selected - 行的最终选择状态。
     * @param {string|null} scope - 行所属查询范围；不传时兼容不分范围的调用。
     * @returns {number} 操作顺序号。
     */
    record(id, selected, scope = null) {
      this.revision += 1;
      const idSequence = ++sequence;
      if (id != null && pendingOperations.size > 0) {
        latestById.set(id, { id: idSequence, selected, scope });
      }
      return idSequence;
    },
    /**
     * 缓存异步全量请求开始后的最新行对象，避免旧查询结果覆盖保存值。
     * @param {Object} row - 最新词条对象。
     * @returns {void} 无返回值。
     */
    refresh(row) {
      if (row?.id == null || pendingOperations.size === 0) return;
      refreshedRowsById.set(row.id, { id: ++sequence, row });
      this.revision += 1;
    },
    /**
     * 计算某行在当前范围下的待处理选择显示状态。
     * @param {string|number} id - 稳定行 ID。
     * @param {string} scope - 当前查询范围。
     * @param {boolean} committed - 已提交选择状态。
     * @returns {boolean} 表格应显示的选择状态。
     */
    project(id, scope, committed) {
      const storedItemOperation = latestById.get(id);
      const itemOperation = storedItemOperation &&
        (storedItemOperation.scope == null || storedItemOperation.scope === scope)
        ? storedItemOperation
        : null;
      let latestOperation = null;
      for (const operation of pendingOperations.values()) {
        if (
          operation.scope === scope &&
          (!latestOperation || operation.id > latestOperation.id)
        ) {
          latestOperation = operation;
        }
      }
      if (itemOperation && (!latestOperation || itemOperation.id > latestOperation.id)) {
        return itemOperation.selected;
      }
      return latestOperation ? latestOperation.selected : committed;
    },
    /**
     * 判断操作令牌是否仍属于当前代次和查询范围。
     * @param {Object} operation - begin 返回的操作令牌。
     * @param {string} currentScope - 当前查询范围。
     * @returns {boolean} 操作是否有效。
     */
    isCurrent(operation, currentScope) {
      return pendingOperations.get(operation?.id) === operation &&
        operation.generation === generation &&
        operation.scope === currentScope;
    },
    /**
     * 将有效全量结果应用到清单，并保留被后续行操作覆盖的 ID。
     * @param {Object} operation - begin 返回的操作令牌。
     * @param {Array<Object>} rows - 完整查询结果。
     * @param {Array<Object>} selectedRows - 当前已选行。
     * @param {string} currentScope - 完成时的查询范围。
     * @returns {Array<Object>} 更新后的清单。
     */
    resolve(operation, rows, selectedRows, currentScope) {
      this.revision += 1;
      if (pendingOperations.get(operation?.id) !== operation) return selectedRows;
      pendingOperations.delete(operation.id);
      if (operation.generation !== generation || operation.scope !== currentScope) {
        clearLatestWhenIdle();
        return selectedRows;
      }

      const applicableRows = (rows || []).filter((row) => {
        if (!row || row.id == null) return false;
        const latestOperation = latestById.get(row.id);
        return !latestOperation || latestOperation.id <= operation.id;
      });
      const resolvedRows = applicableRows.map((row) => {
        const refreshed = refreshedRowsById.get(row.id);
        return refreshed && refreshed.id > operation.id ? refreshed.row : row;
      });
      resolvedRows.forEach((row) => {
        latestById.set(row.id, {
          id: operation.id,
          selected: operation.selected,
          scope: operation.scope,
        });
      });
      const nextRows = operation.selected
        ? mergeSelectedEntriesById(selectedRows, resolvedRows)
        : removeSelectedEntriesById(selectedRows, resolvedRows);
      clearLatestWhenIdle();
      return nextRows;
    },
    /**
     * 移除失败操作，使未被提交的临时投影回滚。
     * @param {Object} operation - begin 返回的操作令牌。
     * @returns {void} 无返回值。
     */
    reject(operation) {
      this.revision += 1;
      if (pendingOperations.get(operation?.id) === operation) {
        pendingOperations.delete(operation.id);
      }
      clearLatestWhenIdle();
    },
    /**
     * 使当前查询范围内所有待处理全量操作失效。
     * @returns {number} 新操作代次。
     */
    invalidate() {
      this.revision += 1;
      generation += 1;
      sequence += 1;
      pendingOperations.clear();
      latestById.clear();
      refreshedRowsById.clear();
      return generation;
    },
    /**
     * 返回仍在请求中的全量操作数量。
     * @returns {number} 待处理操作数。
     */
    get pendingCount() {
      return pendingOperations.size;
    },
  };
  return guard;

  /**
   * 所有请求结束后清理仅用于竞态判定的单行操作记录。
   * @returns {void} 无返回值。
   */
  function clearLatestWhenIdle() {
    if (pendingOperations.size === 0) {
      latestById.clear();
      refreshedRowsById.clear();
    }
  }
}
