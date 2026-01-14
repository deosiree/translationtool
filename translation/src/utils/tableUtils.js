/**
 * 表格相关工具函数
 * 包含表格列配置、筛选、高度设置等功能
 */
import { entryParams } from "@/utils/commonParam.js";
import { intersection } from "./dataStructureUtils";

/**
 * 从本地存储读取用户列偏好并更新组件状态
 * @param {string} colPrefName - 存储用户列偏好的 localStorage 键名
 * @param {Array} normalWidth - 表格列的默认宽度数组
 * @param {Object} vm - Vue 实例对象，包含 `checkedColumn` 和 `changeColumn` 方法
 * @param {boolean} needFilter - 是否需要过滤
 * @param {Object} tableParam - 表格参数对象checkboxList(默认使用entryParams.checkboxList，比如术语库时就要使用glossaryParams.checkboxList了)
 */
export function getColPref(colPrefName, normalWidth, vm, needFilter = false, norm_checkboxList = entryParams.checkboxList) {
  // 读取本地存储的用户偏好
  const storedPreferences = localStorage.getItem(colPrefName);
  // console.log(colPrefName, "偏好：", storedPreferences)
  if (storedPreferences) {
    const colPref_strList = JSON.parse(storedPreferences).displayColumn.split(",");
    // console.log("读取用户列偏好时，同时更新列显示", colPref_strList, vm.columns);
    // 读取用户列偏好时，调用 changeColumn 方法更新列显示
    changeColumn(colPrefName, normalWidth, colPref_strList, vm, needFilter = needFilter, norm_checkboxList = norm_checkboxList);
  }
}

/**
 * 根据用户勾选的列配置表格列展示，并将用户偏好保存到 localStorage
 * @param {string} colPrefName - 存储用户列偏好的 localStorage 键名
 * @param {Array} normalWidth - 表格列的默认宽度数组
 * @param {Array} checkedValue - 用户勾选的列值数组
 * @param {Object} vm - Vue 实例对象，包含 `checkedColumn`、`checkboxList`、`columns` 等属性
 * @param {boolean} needFilter - 是否需要过滤
 * @param {Object} tableParam - 表格参数对象checkboxList(默认使用entryParams.checkboxList，比如术语库时就要使用glossaryParams.checkboxList了)
 */
export function changeColumn(colPrefName, normalWidth, colPref_strList, vm, needFilter = false, norm_checkboxList = entryParams.checkboxList) {
  // 全部的展示列复选框vm.checkboxList
  // 勾选的展示列复选框vm.checkedColumn
  // 表格列的复选框vm.columns
  if (vm.checkedColumn)
    vm.checkedColumn = colPref_strList;

  if (vm.checkboxList && vm.checkboxList.length > 0 && vm.columns && vm.columns.length > 0) {// 如果有展示列，则要和列比较
    vm.checkboxList.forEach((value) => {
      // console.log("要比较的展示列：", value)
      // 查找当前勾选列表中是否存在该列
      let checkedIndex = vm.checkedColumn.findIndex(
        (item) => item === value.value
      );
      // 查找当前表格列中是否存在该列
      let nowColumnIndex = vm.columns.findIndex(
        (item) => item.dataIndex === value.value
      );
      // 若勾选状态和列存在状态一致，则跳过
      if (
        (nowColumnIndex !== -1 && checkedIndex !== -1) ||
        (nowColumnIndex === -1 && checkedIndex === -1)
      ) {
        return;
      }
      // 若勾选了但列不存在，则添加列
      if (nowColumnIndex === -1 && checkedIndex !== -1) {
        // 调用创建列配置对象的函数
        const newCol = createColumn(value, normalWidth, needFilter = needFilter);
        vm.columns.splice(-1, 0, newCol);
      }
      // 若未勾选但列存在，则移除列
      if (nowColumnIndex !== -1 && checkedIndex === -1) {
        vm.columns.splice(nowColumnIndex, 1);
      }
    });
  }
  else {
    // 如果没有展示列和列，则直接使用colPref_strList,经过这个函数的处理来生成列，把空白的columns填满
    for (let i = 0; i < colPref_strList.length; i++) {
      if (colPref_strList[i] == "")
        continue;
      if (vm.columns.some(col => col.dataIndex === colPref_strList[i])) {
        continue; // 如果列已经存在，则跳过
      }
      // console.log(colPrefName, "没有展示列：", colPref_strList, norm_checkboxList,"生成新列：", colPref_strList[i])
      // 使用 find 方法查找对应的 checkboxList 项
      const col = norm_checkboxList.find(item => item.value === colPref_strList[i]);
      // console.log("col:", col, "colPref_strList[i]", colPref_strList[i], colPref_strList, colPref_strList.length);
      const newCol = createColumn(col, normalWidth, needFilter = needFilter);
      vm.columns.splice(-1, 0, newCol);
    }
    // console.log("没有展示列，使用colPref_strList生成列", vm.columns);
  }

  vm.columns.sort((a, b) => a.index - b.index);

  // console.log("当前列配置", colPref_strList);
  // 记录
  let data = {
    displayColumn: colPref_strList.join(","),
  };
  localStorage.setItem(colPrefName, JSON.stringify(data)); // localStorage存储用户偏好
  // console.log("已保存列偏好设置!!!", data);
}

/**
 * 创建表格列配置对象
 * @param {Object} value - 包含列配置信息的对象，包含 `label`、`value`、`index` 属性
 * @param {number} normalWidth - 列的正常宽度
 * @param {boolean} needFilter - 是否需要筛选功能，默认为 false
 * @returns {Object} - 表格列配置对象
 */
export function createColumn(value, normalWidth, needFilter = false) {
  // console.log("创建列value", value)
  // 初始化列配置对象
  let newCol = {
    title: value.label,
    dataIndex: value.value,
    align: "center",
    width: normalWidth,// 默认宽度
    ellipsis: true,// 超出显示省略号
    resizable: true,// 可调整列宽
    index: value.index,
  };

  // 根据列数据索引设置固定位置
  // ("isExist", "translateState","auditSuggess","entryState"：存在状态、翻译状态，审核意见，词条状态，也不固定了)
  if (["entry"].includes(newCol.dataIndex)) {
    newCol.fixed = "left";
  }
  if (["operation"].includes(newCol.dataIndex)) {
    newCol.fixed = "right";
  }

  if (needFilter) {
    // 若列数据索引为 "entrySource"，添加筛选功能
    /**
     * entrySource 列：用于筛选 entrySource 字段的数据，通常该字段可能存储着词条来源等文本信息。
     * 筛选逻辑：模糊匹配，将 record.entrySource 转换为字符串并转为小写，检查是否包含用户输入的筛选值（同样转为小写）
     * 应用场景：适用于文本类型的数据筛选，用户可以输入部分关键字来筛选出包含该关键字的所有记录。
     */
    if (["entrySource"].includes(newCol.dataIndex)) {
      newCol.customFilterDropdown = true; // 使用自定义筛选下拉框
      newCol.filteredValue = null; // 初始状态下没有筛选条件
      newCol.onFilter = (filterValue, record) => {
        const cellValue = record[newCol.dataIndex];
        // 处理空值情况
        if (cellValue === null || cellValue === undefined) {
          return false;
        }
        return cellValue.toString().toLowerCase().includes(filterValue.toLowerCase());
      };
    }

    // 添加筛选功能（全量筛选）
    if (["entry"].includes(newCol.dataIndex)) {
      newCol.customFilterDropdown = true; // 使用自定义筛选下拉框
      newCol.filteredValue = null; // 初始状态下没有筛选条件
      newCol.onFilter = (filterValue, record) => {
        const cellValue = record[newCol.dataIndex];
        // 处理空值情况
        if (cellValue === null || cellValue === undefined) {
          return false;
        }
        // 精确匹配，不忽略大小写
        return cellValue.toString() === filterValue;
      };
    }

    // 若列数据索引为 "isExist"，添加筛选功能
    /**
     * isExist 列：用于筛选 isExist 字段的数据，该字段一般存储布尔值或表示存在状态的枚举值。
     * 筛选逻辑：精确匹配，会判断 record.isExist 是否等于筛选值。
     * 应用场景：适用于枚举类型或布尔类型的数据筛选，用户可以精确选择某个状态值来筛选出符合该状态的记录。
     */
    if (newCol.dataIndex === "isExist") {
      newCol.customFilterDropdown = true; // 使用自定义筛选下拉框
      newCol.filteredValue = null; // 初始状态下没有筛选条件
      newCol.filters = [
        { text: "已存在", value: 1 },
        { text: "新建", value: 0 },
      ];
      newCol.onFilter = (filterValue, record) =>
        record.isExist === filterValue;
    }
  }

  return newCol;
}

/**
 * 筛选功能-列筛选
 * @param {Array} selectedKeys - 选中词条的key集合
 * @param {Function} confirm - 确认筛选的回调函数
 * @param {string} dataIndex - 当前筛选的列的 dataIndex
 * @param {Object} vm - Vue 实例对象
 */
export function handleSearch(selectedKeys, confirm, dataIndex, vm) {
  confirm();
  vm.state.searchText = selectedKeys[0];
  vm.state.searchedColumn = dataIndex;
}

/**
 * 筛选功能-重置
 * @param {Function} clearFilters - 重置筛选条件的回调函数
 * @param {Object} vm - Vue 实例对象
 */
export function handleReset(clearFilters, vm) {
  clearFilters({ confirm: true });
  vm.state.searchText = "";
}

/**
 * 筛选功能-清空表格筛选条件
 * @param {Object} vm - Vue 实例对象
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
 * 筛选功能-表格change事件
 * @param {Object} pagination - 分页信息对象，包含 `current`（当前页码）和 `pageSize`（每页显示数量）属性
 * @param {Object} filters - 筛选条件对象，包含 `isExist`（存在状态筛选）和 `entrySource`（来源筛选）属性
 * @param {Object} vm - Vue 实例对象
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
  // 获取筛选后的数据
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
 * @param {VueInstance} vm - Vue 实例
 * @param {number} buttonHeightBias - 按钮高度的偏移量，默认值为 8
 * @param {number} tableHeightBias - 表格高度的偏移量，默认值为 150(滚动条变宽了，+8)
 */
export function setTableHeight(vm, buttonHeightBias = 8, tableHeightBias = 158, dataHeightBias = 0, hasboxHeight = { ok: false, h: 0 }) {
  vm.$nextTick(() => {
    let searchHeight = 0;
    if (vm.$refs.search?.$el) {
      searchHeight = vm.$refs.search.$el.offsetHeight;
    }
    let box = 0;
    if (!hasboxHeight.ok)
      box = vm.$refs.box?.offsetHeight ?? 0;
    else
      box = hasboxHeight.h;
    const operationAreaHeight = vm.$refs.operationArea?.$el?.offsetHeight ?? 0;
    vm.dataHeight = box - searchHeight - dataHeightBias - operationAreaHeight;

    let buttonHeight = 0;
    try {
      buttonHeight = vm.$refs.button?.offsetHeight + buttonHeightBias ?? buttonHeightBias;
    } catch (error) { }
    vm.tableHeight.y = vm.dataHeight - buttonHeight - tableHeightBias;

    // console.log(vm.tableHeight.y)
  });
}

/**
 * 表格列可伸缩
 * @param {number} w - 新的列宽度
 * @param {object} col - 列对象
 */
export function handleResizeColumn(w, col) {
  col.width = w;
}

/**
 * 设置表格每一行的 class
 * @param {object} record - 行数据记录
 * @param {number} index - 行索引
 * @param {number} selectedRowIndex - 选中行的索引
 * @returns {string} - 行的 class 名称
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
