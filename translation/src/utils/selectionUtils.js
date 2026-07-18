/**
 * 表格选择/分页工具函数
 * 包含复选框选择、分页切换等功能
 */

/**
 * 按 id 合并已选词条，后者覆盖同 id 前者
 * @description 用于「选择全部」等场景：保留跨产品已选，并对当前查询全量按 id 去重合并
 * @param {Array<{id: string|number}>} existingRows - 已有已选行
 * @param {Array<{id: string|number}>} incomingRows - 新并入的行（如同一次「选择全部」接口结果）
 * @returns {Array<{id: string|number}>} 按 id 唯一合并后的行数组
 * @example
 * mergeSelectedEntriesById(
 *   [{ id: 1 }, { id: 2 }],
 *   [{ id: 1, name: '更新' }, { id: 3 }]
 * )
 * // => [{ id: 1, name: '更新' }, { id: 2 }, { id: 3 }]
 */
export function mergeSelectedEntriesById(existingRows, incomingRows) {
  const map = new Map();
  (existingRows || []).forEach((row) => {
    if (row && row.id != null) map.set(row.id, row);
  });
  (incomingRows || []).forEach((row) => {
    if (row && row.id != null) map.set(row.id, row);
  });
  return Array.from(map.values());
}

/**
 * 将「选择全部」结果写回批量选择约定字段。
 *
 * @description 只赋值 `selectEntry` / `selectedRows` / `selectedRowKeys`，不发请求、不改 loading。
 * @param {{ selectEntry?: Array, selectedRows?: Array, selectedRowKeys?: Array }} target - 通常为页面 Vue 实例
 * @param {Array<{ id?: string|number }>} rows - 已按 id 去重的全量行
 * @returns {void}
 */
export function applyBatchSelectAll(target, rows) {
  if (!target || typeof target !== "object") return;
  const list = Array.isArray(rows) ? rows : [];
  target.selectEntry = list;
  target.selectedRows = [...list];
  target.selectedRowKeys = list
    .map((item) => item?.id)
    .filter((id) => id != null);
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
