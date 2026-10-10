/**
 * 工作台流水线专用：关弹窗清列头筛选（Ant Design Table 原生 clearFilters 回调）
 * 落点 views/workbench/composables
 */

/**
 * 保存 Ant Table 列头筛选注入的 clearFilters 回调
 * @param {Object} vm Options API 实例，需含 antClearFilter 字段
 * @param {Function} fn Ant 注入的 clearFilters
 * @returns {void}
 */
export function saveClearFn(vm, fn) {
  vm.antClearFilter = fn;
}

/**
 * 列头自定义筛选：确认搜索并保存 clearFilters 供关弹窗复位
 * @param {string[]} selectedKeys 用户输入的筛选关键字
 * @param {Function} confirm Ant 确认回调
 * @param {string} dataIndex 当前列 dataIndex
 * @param {Function} clearFilters Ant 注入的清筛选回调
 * @param {Object} vm 含 state.searchText / state.searchedColumn / antClearFilter
 * @returns {void}
 */
export function handleFilterSearch(
  selectedKeys,
  confirm,
  dataIndex,
  clearFilters,
  vm
) {
  confirm();
  vm.state.searchText = selectedKeys[0];
  vm.state.searchedColumn = dataIndex;
  saveClearFn(vm, clearFilters);
}

/**
 * 内置 filters 列（如 isExist）受控 filteredValue 复位
 * @param {Object} vm 含 filters / columns
 * @returns {void}
 */
export function resetBuiltinColumnFilters(vm) {
  if (vm.filters) {
    for (const key in vm.filters) {
      vm.columns.forEach((col) => {
        if (col.dataIndex === key) {
          col.filteredValue = null;
        }
      });
    }
  }
  vm.filters = null;
}

/**
 * 关弹窗：Ant 原生清列头筛选 + 内置 filters 与搜索态复位
 * @param {Object} vm 含 antClearFilter / filters / columns / state
 * @returns {void}
 */
export function resetOnClose(vm) {
  if (typeof vm.antClearFilter === "function") {
    vm.antClearFilter({ confirm: true });
  }
  resetBuiltinColumnFilters(vm);
  if (vm.state) {
    vm.state.searchText = "";
    vm.state.searchedColumn = "";
  }
  vm.antClearFilter = null;
}
