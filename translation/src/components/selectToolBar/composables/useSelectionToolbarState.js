/** @fileoverview 计算已选数量、徽标显示数量和清空按钮状态。 */
/**
 * 计算选择工具栏的数量、徽标与清空按钮状态。
 * @param {Array<Object>} selectedEntries - 当前选择清单。
 * @param {boolean} loading - 是否有全量选择操作进行中。
 * @returns {{count: number, badgeCount: number, loading: boolean, clearDisabled: boolean}} 工具栏状态快照。
 */
export function useSelectionToolbarState(selectedEntries, loading = false) {
  const count = Array.isArray(selectedEntries) ? selectedEntries.length : 0;
  const isLoading = Boolean(loading);

  return {
    count,
    badgeCount: isLoading ? 0 : count,
    loading: isLoading,
    clearDisabled: count === 0 && !isLoading,
  };
}
