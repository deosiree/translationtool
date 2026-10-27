/**
 * 表格选择/分页工具函数
 * 包含复选框选择、分页切换等功能
 */
import {
  applySelectionById,
  mergeSelectedEntriesById,
  removeSelectedEntriesById,
} from "@/components/selectToolBar/composables/useSelectedEntries";
import { useSelectionOperations } from "@/components/selectToolBar/composables/useSelectionOperations";

export {
  applySelectionById,
  mergeSelectedEntriesById,
  removeSelectedEntriesById,
};
export { useSelectionOperations as createSelectionOperationGuard };

/**
 * 用新查询行刷新已选对象，但不把新查询的未选 ID 加入清单。
 * @param {Array<{id: string|number}>} selectedRows - 当前已选行。
 * @param {Array<{id: string|number}>} incomingRows - 新查询返回行。
 * @returns {Array<{id: string|number}>} 已更新对象引用的已选行。
 */
export function refreshSelectedEntriesById(selectedRows, incomingRows) {
  const incomingById = new Map(
    flattenRowsById(incomingRows).map((row) => [row.id, row])
  );
  return (selectedRows || []).map((row) => incomingById.get(row.id) || row);
}

/**
 * 刷新表格已选行对象并同步其稳定 ID 键。
 * 行对象在异步查询期间缺失时，按已有行键从本次查询结果恢复；查询范围外的对象保留。
 * @param {Object} vm - 持有 selectedRows 和 selectedRowKeys 的组件实例。
 * @param {Array<Object>} incomingRows - 最新查询结果，可包含树形子行。
 * @returns {Array<Object>} 更新后的已选行。
 */
export function refreshTableSelectionById(vm, incomingRows) {
  const selectedRows = vm.selectedRows || [];
  const selectedIds = new Set([
    ...(vm.selectedRowKeys || []),
    ...selectedRows.map((row) => row.id),
  ]);
  const incomingById = new Map(
    flattenRowsById(incomingRows).map((row) => [row.id, row])
  );
  const selectedById = new Map(selectedRows.map((row) => [row.id, row]));
  vm.selectedRows = [...selectedIds]
    .map((id) => incomingById.get(id) || selectedById.get(id))
    .filter(Boolean);
  vm.selectedRowKeys = vm.selectedRows.map((row) => row.id);
  return vm.selectedRows;
}

/**
 * 将树形表格行及其后代按 ID 展平并去重。
 * @param {Array<Object>} rows - 表格当前完整结果。
 * @param {string} childKey - 子行属性名。
 * @returns {Array<Object>} 按遍历顺序去重后的行。
 */
export function flattenRowsById(rows, childKey = "children") {
  const result = [];
  const seen = new Set();
  const visit = (items) => {
    (items || []).forEach((row) => {
      if (!row || row.id == null || seen.has(row.id)) return;
      seen.add(row.id);
      result.push(row);
      visit(row[childKey] || row.children || row.child || []);
    });
  };
  visit(rows);
  return result;
}

/**
 * 按表格列筛选条件从完整行结果中取得当前可见选择范围。
 * @param {Array<Object>} rows - 表格完整结果。
 * @param {Object} filters - Ant Design Vue 表格筛选值。
 * @returns {Array<Object>} 符合所有有效筛选条件的行。
 */
export function getRowsMatchingFilters(rows, filters) {
  const flattenedRows = flattenRowsById(rows);
  const activeFilters = Object.entries(filters || {}).filter(
    ([, values]) => Array.isArray(values) && values.length > 0
  );
  if (activeFilters.length === 0) return flattenedRows;
  return flattenedRows.filter((row) =>
    activeFilters.every(([key, values]) => {
      const value = row[key];
      return values.some((filterValue) =>
        Array.isArray(value) ? value.includes(filterValue) : value === filterValue
      );
    })
  );
}

/**
 * 返回当前组件可用于全量选择的完整行缓存，兼容查询尚未建立缓存的初始状态。
 * @param {Object} vm - 持有 allData 与 dataSource 的表格组件实例。
 * @returns {Array<Object>} 当前查询范围的完整行列表。
 */
export function getSelectionScopeRows(vm) {
  return Array.isArray(vm?.allData) && vm.allData.length
    ? vm.allData
    : vm?.dataSource || [];
}

/**
 * 取得表头全选对应的当前完整结果范围。
 * 本地关键词筛选会把 dataSource 缩成 allData 的子集；此时恢复完整对象后
 * 再按当前可见 ID 限制范围。远程分页没有完整 allData，不会错误扩展到其他页。
 * @param {Object} vm - 持有 dataSource、allData 与 pagination 的表格组件实例。
 * @returns {Array<Object>} 当前查询和本地筛选条件对应的完整行列表。
 */
export function getCurrentSelectionRows(vm) {
  const scopeRows = getSelectionScopeRows(vm);
  const scopeFlatRows = flattenRowsById(scopeRows);
  const visibleRows = flattenRowsById(vm?.dataSource || []);
  const visibleIds = new Set(visibleRows.map((row) => row.id));
  const isLocalSubset =
    scopeFlatRows.length > visibleIds.size &&
    scopeRows !== vm?.dataSource;
  const rows = isLocalSubset
    ? scopeFlatRows.filter((row) => visibleIds.has(row.id))
    : scopeRows;
  return getRowsMatchingFilters(rows, vm?.filters);
}

/**
 * 应用一次完整结果集选择并同步表格的行键和行对象。
 * @param {Object} vm - 持有 selectedRows 和 selectedRowKeys 的组件实例。
 * @param {Array<Object>} resultRows - 本次全选或取勾的完整结果。
 * @param {boolean} selected - true 合并，false 移除。
 * @returns {Array<Object>} 更新后的已选行。
 */
export function applyTableSelectionById(vm, resultRows, selected) {
  vm.selectedRows = applySelectionById(vm.selectedRows, resultRows, selected);
  vm.selectedRowKeys = vm.selectedRows.map((row) => row.id);
  return vm.selectedRows;
}

/**
 * 清空表格的已选行和行键。
 * @param {Object} vm - 持有 selectedRows 与 selectedRowKeys 的组件实例。
 * @returns {void} 无返回值。
 */
export function clearTableSelection(vm) {
  vm.selectedRows = [];
  vm.selectedRowKeys = [];
}

/**
 * 清空当前列表查询上下文中的行、选择和编辑缓存。
 * @param {Object} vm - 持有表格数据和选择状态的组件实例。
 * @returns {void} 无返回值。
 */
export function clearTableQueryContext(vm) {
  vm.dataSource = [];
  if (Array.isArray(vm.allData)) vm.allData = [];
  if (Array.isArray(vm.dataSourceAll)) vm.dataSourceAll = [];
  clearTableSelection(vm);
  if (vm.editableData) vm.editableData = {};
  if (vm.cellErrors) vm.cellErrors = {};
  if ("selectedRowIndex" in vm) vm.selectedRowIndex = null;
}

/**
 * 失效查询会话并清空列表、选择和编辑上下文。
 * @param {Object} vm - 持有查询状态与表格上下文的组件实例。
 * @param {Array<{invalidate: Function}>} [guards=[vm.queryGuard]] - 本次会话关联的请求守卫。
 * @returns {void} 无返回值。
 */
export function resetTableQuerySession(vm, guards = [vm.queryGuard]) {
  guards.forEach((guard) => guard.invalidate());
  vm.queryLoading = false;
  vm.queryScope = "";
  clearTableQueryContext(vm);
}

/**
 * 清除旧查询行与编辑缓存，但保留跨查询已提交选择。
 * @param {Object} vm - 持有表格行、编辑状态和已选清单的组件实例。
 * @returns {void} 无返回值。
 */
export function clearTableRowsContext(vm) {
  vm.dataSource = [];
  if (Array.isArray(vm.allData)) vm.allData = [];
  if (Array.isArray(vm.dataSourceAll)) vm.dataSourceAll = [];
  if (vm.editableData) vm.editableData = {};
  if (vm.cellErrors) vm.cellErrors = {};
  if ("selectedRowIndex" in vm) vm.selectedRowIndex = null;
}

/**
 * 按表格当前选择结果同步当前查询范围，并保留范围外的已选行。
 * @param {Object} vm - 持有 selectedRows 与 selectedRowKeys 的组件实例。
 * @param {Array<Object>} tableSelectedRows - 表格回调返回的全部已选行。
 * @param {Array<Object>} scopeRows - 当前完整查询范围的行，包括树形子行。
 * @returns {Array<Object>} 按 ID 去重后的完整已选行。
 */
export function syncTableSelectionById(vm, tableSelectedRows, scopeRows) {
  const scopedRows = flattenRowsById(scopeRows);
  const scopedIds = new Set(scopedRows.map((row) => row.id));
  const retainedRows = (vm.selectedRows || []).filter((row) => !scopedIds.has(row?.id));
  const selectedInScope = flattenRowsById(tableSelectedRows).filter((row) =>
    scopedIds.has(row.id)
  );
  vm.selectedRows = mergeSelectedEntriesById(retainedRows, selectedInScope);
  vm.selectedRowKeys = vm.selectedRows.map((row) => row.id);
  return vm.selectedRows;
}

/**
 * 只更新表格回调涉及的行，保留同一查询范围其他页和范围外的已选行。
 * @param {Object} vm - 持有 selectedRows 与 selectedRowKeys 的组件实例。
 * @param {Array<Object>} changedRows - 本次回调实际改变的行，可包含树形子行。
 * @param {boolean} selected - 本次行操作的最终选择状态。
 * @returns {Array<Object>} 按 ID 去重后的完整已选行。
 */
export function updateTableSelectionById(vm, changedRows, selected) {
  vm.selectedRows = applySelectionById(
    vm.selectedRows,
    flattenRowsById(changedRows),
    selected,
  );
  vm.selectedRowKeys = vm.selectedRows.map((row) => row.id);
  return vm.selectedRows;
}

/**
 * 列筛选范围变化时使待处理操作失效，但保留已提交的跨筛选选择。
 * @param {Object} vm - 持有筛选值和可选操作守卫的组件实例。
 * @param {Object} nextFilters - 即将应用的筛选条件。
 * @returns {boolean} 筛选范围是否变化。
 */
export function invalidateSelectionWhenFiltersChange(vm, nextFilters) {
  if (JSON.stringify(vm.filters || {}) === JSON.stringify(nextFilters || {})) {
    return false;
  }
  vm.selectionOperations?.invalidate();
  return true;
}

/**
 * 使用现有分页接口逐页收集一组查询的完整结果。
 * @param {(pageIndex: number, pageSize: number) => Promise<Object>} fetchPage - 当前查询范围的分页请求。
 * @param {number} pageSize - 单页请求数量。
 * @param {() => boolean} shouldContinue - 判断当前收集操作是否仍有效。
 * @returns {Promise<Array>} 收集完成的行列表；操作失效时返回空数组。
 */
export async function collectAllPages(fetchPage, pageSize = 100, shouldContinue = () => true) {
  if (!shouldContinue()) return [];

  const firstResponse = await fetchPage(1, pageSize);
  if (!shouldContinue()) return [];

  const firstRows = Array.isArray(firstResponse?.data?.list)
    ? firstResponse.data.list
    : [];
  const rows = [...firstRows];
  let total = Number(firstResponse?.data?.totalNum);
  let hasTotal = Number.isFinite(total) && total >= 0;
  const effectivePageSize = firstRows.length || pageSize;

  for (
    let pageIndex = 2;
    hasTotal ? rows.length < total : firstRows.length > 0;
    pageIndex += 1
  ) {
    if (!shouldContinue()) return [];

    const response = await fetchPage(pageIndex, effectivePageSize);
    if (!shouldContinue()) return [];

    const pageRows = Array.isArray(response?.data?.list) ? response.data.list : [];
    const nextTotal = Number(response?.data?.totalNum);
    if (Number.isFinite(nextTotal) && nextTotal >= 0) {
      total = nextTotal;
      hasTotal = true;
    }
    if (pageRows.length === 0) {
      if (hasTotal && rows.length < total) {
        throw new Error("分页查询未返回完整结果");
      }
      break;
    }
    rows.push(...pageRows);
    if (!hasTotal && pageRows.length < effectivePageSize) break;
  }
  return rows;
}

/**
 * 分页切换函数
 * @param {VueInstance} vm - Vue 实例
 * @param {number} page - 当前页码
 * @param {number} pageSize - 每页显示数量
 * @param {function} fetchData - 数据查询回调函数
 */
export function pageChange(vm, page, pageSize, fetchData, selectEntry = "selectEntry", ...fetchDataParams) {
  vm.pagination.current = page;
  vm.pagination.pageSize = pageSize;
  // console.log("更新了page信息", page, pageSize);
  // 调用传入的查询接口函数
  if (typeof fetchData === 'function') {// 有可能不传，就是null
    if (fetchDataParams.length === 0)
      fetchData();
    else
      fetchData(...fetchDataParams);
  }

  // 如果vm中有这个参数（默认为vm[selectEntry]）,就在分页后  保留之前勾选的词条
  if (selectEntry in vm) {
    vm.selectedRows = vm[selectEntry];
    vm[selectEntry].forEach((item) => {
      vm.selectedRowKeys.push(item.id);
    });
    vm.selectedRowKeys = [...new Set(vm.selectedRowKeys)];// 去重
  }
}

/**
 * 复选框选择事件处理函数
 * @param {VueInstance} vm - Vue 实例
 * @param {Array} selectedRowKeys - 当前选中行的键数组
 * @param {Array} selectedRows - 当前选中行的数据数组
 */
export function onSelectChange(vm, selectedRowKeys, selectedRows) {
  // console.log("onSelectChange")
  // v1：未去重
  // vm.selectedRowKeys = selectedRowKeys;// 将当前选中行的键数组赋值给 Vue 实例中的 selectedRowKeys
  // vm.selectedRows = selectedRows;// 将当前选中行的数据数组赋值给 Vue 实例中的 selectedRows
  // v2：去重
  vm.selectedRowKeys = Array.from(new Set(selectedRowKeys));
  const idSet = new Set();
  vm.selectedRows = selectedRows.filter((item) => {
    if (idSet.has(item.id)) {
      return false;
    }
    idSet.add(item.id);
    return true;
  });
}

/**
 * 复选框点击事件处理函数
 * @param {Object} vm - Vue 实例，用于访问和修改数据
 * @param {Object} record - 当前点击复选框对应的记录数据
 * @param {boolean} selected - 复选框的选中状态，true 表示选中，false 表示未选中
 * @param {boolean} condition - 执行操作的条件，默认为 true
 * @param {string} selectEntry - 存储选中词条的属性名，默认为 "selectEntry"
 */
export function onSelect(vm, record, selected, condition = true, selectEntry = "selectEntry") {
  if (!condition) return; // 如果条件不满足，直接返回
  // console.log("select", record, )
  // if (selected) {
  //   vm.selectedRows.push(record);
  //   if (selectEntry in vm)
  //     vm[selectEntry].push(record);
  // } else {
  //   vm.selectedRows = vm.selectedRows.filter((item) => { return item !== record; });
  //   if (selectEntry in vm)
  //     vm[selectEntry] = vm[selectEntry].filter((item) => { return item !== record; });
  // }
  if (selected) {
    const isExists1 = vm.selectedRows.some(
      (item) => item.id === record.id
    );
    if (!isExists1) {
      vm.selectedRows.push(record);// 添加选择
    }
    // console.log("是否有保存？",selectEntry in vm)
    if (selectEntry in vm) {
      const isExists2 = vm[selectEntry].some(
        (item) => item.id === record.id
      );
      if (!isExists2) {
        vm[selectEntry].push(record);// 添加到已选词条
      }
    }
  } else {
    vm.selectedRows = vm.selectedRows.filter((item) => {
      return item.id !== record.id;
    });// 取消选择
    vm.selectedRowKeys = vm.selectedRowKeys.filter((item) => {
      return item.id !== record.id;
    });
    if (selectEntry in vm) {
      vm[selectEntry] = vm[selectEntry].filter((item) => {
        return item.id !== record.id;
      });// 从已选词条中去除
    }
  }
}

/**
 * 复选框当前页全选/反选框点击事件处理函数
 * @param {Object} vm - Vue 实例，用于访问和修改数据
 * @param {boolean} selected - 全选/反选当前页的多选框的选中状态，true 表示全选，false 表示反选
 * @param {Array} selectedRows - 当前页所有选中的记录数组
 * @param {Array} changeRows - 状态发生改变的记录数组
 * @param {boolean} condition - 执行操作的条件，默认为 true
 * @param {string} selectEntry - 存储选中词条的属性名，默认为 "selectEntry"
 */
export function onSelectAll(vm, selected, selectedRows, changeRows, condition = true, selectEntry = "selectEntry") {
  if (!condition) return; // 如果条件不满足，直接返回

  let dataToSelect;
  // 检查是否存在筛选条件
  if (vm.filters && (vm.filters.isExist || vm.filters.entrySource)) {
    // 若存在筛选条件，根据筛选条件过滤数据源
    dataToSelect = vm.dataSource.filter((item) => {
      // 检查 isExist 字段是否匹配筛选条件
      const isExistMatch =
        !vm.filters.isExist ||
        vm.filters.isExist.includes(item.isExist);
      // 检查 entrySource 字段是否匹配筛选条件
      const entrySourceMatch =
        !vm.filters.entrySource ||
        item.entrySource.includes(vm.filters.entrySource);
      // 只有当两个条件都匹配时，才将该项加入待选择数据
      return isExistMatch && entrySourceMatch;
    });
  } else {
    // 若不存在筛选条件，直接使用数据源
    dataToSelect = vm.dataSource;
  }

  if (selected) {
    const idSet1 = new Set(vm.selectedRows.map(item => item.id));
    dataToSelect.forEach((item) => {
      if (!idSet1.has(item.id)) {
        vm.selectedRows.push(item);
        vm.selectedRowKeys.push(item.id);
        idSet1.add(item.id);
      }
    });

    if (selectEntry in vm) {
      const idSet2 = new Set(vm[selectEntry].map(item => item.id));
      dataToSelect.forEach((item) => {
        if (!idSet2.has(item.id)) {
          vm[selectEntry].push(item);
          idSet2.add(item.id);
        }
      });
    }
  } else {
    const changeIds = new Set(dataToSelect.map(item => item.id));
    vm.selectedRows = vm.selectedRows.filter((entry) => {
      return !changeIds.has(entry.id);
    });
    vm.selectedRowKeys = vm.selectedRowKeys.filter((key) => {
      return !changeIds.has(key);
    });

    if (selectEntry in vm) {
      vm[selectEntry] = vm[selectEntry].filter((entry) => {
        return !changeIds.has(entry.id);
      });
    }
  }
}

/**
 * 复选框全选事件处理函数
 * @param {VueInstance} vm - Vue 实例
 */
export function selectAllEntry(vm, selectEntry = "selectEntry") {
  // console.log("全选");
  vm.selectedRowKeys = [];
  vm.selectedRows = [];
  if (selectEntry in vm) {
    vm[selectEntry] = [];
  }
  let dataToSelect;
  // 检查是否存在筛选条件
  if (vm.filters && (vm.filters.isExist || vm.filters.entrySource)) {
    // 若存在筛选条件，根据筛选条件过滤数据源
    dataToSelect = vm.dataSource.filter((item) => {
      // 检查 isExist 字段是否匹配筛选条件
      const isExistMatch =
        !vm.filters.isExist ||
        vm.filters.isExist.includes(item.isExist);
      // 检查 entrySource 字段是否匹配筛选条件
      const entrySourceMatch =
        !vm.filters.entrySource ||
        item.entrySource.includes(vm.filters.entrySource);
      // 只有当两个条件都匹配时，才将该项加入待选择数据
      return isExistMatch && entrySourceMatch;
    });
  } else {
    // 若不存在筛选条件，直接使用数据源
    dataToSelect = vm.dataSource;
  }
  dataToSelect.forEach((item) => {
    vm.selectedRowKeys.push(item.id);
    vm.selectedRows.push(item);
    if (selectEntry in vm) {
      vm[selectEntry].push(item);
    }
  });
  // console.log("全选后的行", vm.selectedRows);
}

/**
 * 复选框反选事件处理函数
 * @param {VueInstance} vm - Vue 实例
 */
export function clearAllEntry(vm, selectEntry = "selectEntry") {
  // console.log("反选");
  vm.selectedRowKeys = [];
  vm.selectedRows = [];
  if (selectEntry in vm) {
    vm[selectEntry] = [];
  }
}
