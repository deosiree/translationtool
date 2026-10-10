/**
 * 表格 UX 工具（筛选、高度、resize）；列 preset 见 @/components/ColumnFilter
 */
import { intersection } from "./dataStructureUtils";

/**
 * 筛选功能-列筛选
 */
export function handleSearch(selectedKeys, confirm, dataIndex, vm) {
  confirm();
  vm.state.searchText = selectedKeys[0];
  vm.state.searchedColumn = dataIndex;
}

/**
 * 筛选功能-重置
 */
export function handleReset(clearFilters, vm) {
  clearFilters({ confirm: true });
  vm.state.searchText = "";
}

/**
 * 筛选功能-清空表格筛选条件
 */
export function clearFilters(vm) {
  if (vm.filters) {
    for (let key in vm.filters) {
      vm.columns.forEach((col) => {
        if (col.dataIndex === key) {
          col.filteredValue = null;
        }
      });
    }
  }
}

/**
 * 筛选功能-表格 change 事件
 * 同步 filters / filteredValue；vm.filteredData 为历史字段（archive 仍写入，仓库内无读取方）
 */
export function handleTableChange(pagination, filters, vm) {
  vm.filters = filters;
  for (let key in filters) {
    vm.columns.forEach((col) => {
      if (col.dataIndex === key) {
        col.filteredValue = filters[key];
      }
    });
  }
  let isExistData = vm.dataSource.filter((item) => {
    return filters.isExist && filters.isExist.includes(item.isExist);
  });
  let sourceData = vm.dataSource.filter((item) => {
    return (
      filters.entrySource && item.entrySource.includes(filters.entrySource)
    );
  });
  vm.filteredData = intersection(isExistData, sourceData);
}

/**
 * 动态设置表格高度
 */
export function setTableHeight(
  vm,
  buttonHeightBias = 8,
  tableHeightBias = 158,
  dataHeightBias = 0,
  hasboxHeight = { ok: false, h: 0 }
) {
  vm.$nextTick(() => {
    let searchHeight = 0;
    if (vm.$refs.search?.$el) {
      searchHeight = vm.$refs.search.$el.offsetHeight;
    }
    let box = 0;
    if (!hasboxHeight.ok) box = vm.$refs.box?.offsetHeight ?? 0;
    else box = hasboxHeight.h;
    const operationAreaHeight =
      vm.$refs.operationArea?.$el?.offsetHeight ?? 0;
    vm.dataHeight = box - searchHeight - dataHeightBias - operationAreaHeight;

    let buttonHeight = 0;
    try {
      buttonHeight =
        vm.$refs.button?.offsetHeight + buttonHeightBias ?? buttonHeightBias;
    } catch (error) {}
    vm.tableHeight.y = vm.dataHeight - buttonHeight - tableHeightBias;
  });
}

/**
 * 表格列可伸缩
 */
export function handleResizeColumn(w, col) {
  col.width = w;
}

/**
 * 设置表格每一行的 class
 */
export function getRowClassName(record, index, selectedRowIndex) {
  let className = null;
  if (index % 2 === 1) {
    className = "table-striped";
    if (selectedRowIndex === record.id) {
      className = className + " highlighted-row";
    }
  } else {
    if (selectedRowIndex === record.id) {
      className = "highlighted-row";
    }
  }
  return className;
}
