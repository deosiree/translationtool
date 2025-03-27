import common from "@/views/workbench/common.js";
import { cancelRequest, cancelAllRequests } from "@/http/request";
const requestDelId = [];// 存储删除请求的id，用于保留loading状态


/**
 * 查询
 * 查询按钮共用多个接口，并维护loading状态
 * @param {Object} vm - Vue 实例，用于控制 loading 状态
 * @param {Object} params - 包含请求参数和数据的对象，结构为 { params: {...}, data: {...} }
 * @param {string} option - 用户选择的按钮名称，用于匹配 API 函数
 * @param {Object} apiFunctions - 包含按钮名称和对应 API 接口函数的对象，默认值为 { "按钮名称": "接口函数" }
 */
export function getSearch(vm, params, option, apiFunctions = { "按钮名称": "接口函数" }) {
  vm.loading = true; // 开始请求前设置 loading 为 true

  Object.entries(apiFunctions).forEach(([key, value]) => {
    if (option == key) {
      if (params.lastRequestId) {// 如果需要取消请求就会有上次请求的id属性lastRequestId，则可以取消上次的请求
        requestDelId.push(params.lastRequestId);
        cancelRequest(params.lastRequestId);
      }
      value(params.params, params.data).then((response) => {// 调用接口函数
        if (!requestDelId.includes(params.params.requestId)) {
          vm.loading = false;
          // console.log(`${key}请求完了,${params.params.requestId}`);
        }
        else {
          // console.log(`${key}请求被取消了,${params.params.requestId}`);
          requestDelId.splice(requestDelId.indexOf(params.params.requestId), 1); // 已经用过了保存loading状态的作用
        }
      }
      );
    }
  });
}

/**
 * 验证输入字段长度的函数，返回一个验证器函数，用于表单验证规则
 * @param {Object} limitMap - 包含分类限制信息的映射对象
 * @param {Object} record - 当前行的数据记录对象
 * @param {string} language - 语言类型，用于确定验证类型
 * @returns {Function} - 验证器函数，接受 rule 和 value 作为参数
 */
export function vilidFildLength(limitMap, record, language) {
  // 返回一个验证器函数，用于表单验证规则
  return (rule, value) => {
    // 初始化验证类型变量
    let type = "";
    // 根据语言类型确定验证类型
    if (language === "chinese") {
      // 中文使用 maxByte 验证
      type = "maxByte";
    } else {
      // 其他语言使用 foreignMaxByte 验证
      type = "foreignMaxByte";
    }
    // 初始化最大长度变量
    let maxLength = null;
    // 检查 limitMap 中是否存在当前分类的限制信息
    if (
      limitMap[record.classfy1] === undefined ||
      limitMap[record.classfy1] === null
    ) {
      // 如果 limitMap 中没有当前分类的限制信息
      if (record.maxLength != null && record.maxLength != "") {
        // 如果记录中有最大长度信息，则使用该信息
        maxLength = record.maxLength;
      } else {
        // 如果记录中也没有最大长度信息，则验证通过
        return Promise.resolve();
      }
    } else {
      // 如果 limitMap 中有当前分类的限制信息，则使用该信息
      maxLength = limitMap[record.classfy1][type];
    }
    // 检查最大长度是否有效
    if (
      maxLength === undefined ||
      maxLength === "" ||
      maxLength === null ||
      maxLength === 0
    ) {
      // 如果最大长度无效，则验证通过
      return Promise.resolve();
    }
    // 获取输入数据的字节长度
    let length = common.byteLength(value);
    // 检查输入数据的长度是否超过最大长度
    if (length > maxLength) {
      // 如果超过最大长度，则验证失败，返回错误信息
      return Promise.reject("允许最大字符数为" + maxLength + "！");
    }
    // 如果输入数据的长度未超过最大长度，则验证通过
    return Promise.resolve();
  };
}

/**
 * 表单单元格的点击事件处理函数
 * @param {VueInstance} vm - Vue 实例
 * @param {Event} event - 点击事件对象
 */
export function clickInput(vm, event) {
  event.stopPropagation();
  // // 这里可以添加更多的交互逻辑，例如聚焦输入框、记录点击信息等
  // const inputElement = event.target;
  // inputElement.focus();
  // const inputName = inputElement.name;
  // console.log(`点击了输入框: ${inputName}`);
}

/**
 * 动态设置表格高度
 * @param {VueInstance} vm - Vue 实例
 * @param {number} buttonHeightBias - 按钮高度的偏移量，默认值为 8
 * @param {number} tableHeightBias - 表格高度的偏移量，默认值为 150
 */
export function setTableHeight(vm, buttonHeightBias = 8, tableHeightBias = 150, dataHeightBias = 0) {
  vm.$nextTick(() => {
    // 设置列表父元素高度
    let box = vm.$refs.box.offsetHeight;
    let searchHeight = vm.$refs.search.$el.offsetHeight;
    try {
      let operationAreaHeight = vm.$refs.operationArea.$el.offsetHeight;
      vm.dataHeight = box - searchHeight - operationAreaHeight - dataHeightBias;
    } catch (error) {
      vm.dataHeight = box - searchHeight - dataHeightBias;
    }

    // 设置表格高度
    let buttonHeight = 0;
    try {
      buttonHeight = vm.$refs.button.offsetHeight + buttonHeightBias;
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

/**
 * 分页切换函数
 * @param {VueInstance} vm - Vue 实例
 * @param {number} page - 当前页码
 * @param {number} pageSize - 每页显示数量
 * @param {function} fetchData - 数据查询回调函数
 */
export function pageChange(vm, page, pageSize, fetchData, selectEntry = "selectEntry") {
  vm.pagination.current = page;
  vm.pagination.pageSize = pageSize;

  // 调用传入的查询接口函数
  if (typeof fetchData === 'function') {// 有可能不传，就是null
    fetchData();
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
  // console.log("selectAll", selected);
  // if (selected) {
  //   vm.selectedRows = vm.selectedRows.concat(changeRows);
  //   if (selectEntry in vm) {
  //     vm[selectEntry] = vm[selectEntry].concat(changeRows);
  //   }
  // }
  // else {
  //   changeRows.forEach((item) => {
  //     vm.selectedRows = vm.selectedRows.filter((entry) => {
  //       return entry !== item;
  //     });
  //     if (selectEntry in vm) {
  //       vm[selectEntry] = vm[selectEntry].filter((entry) => {
  //         return entry !== item;
  //       });
  //     }
  //   });
  // }
  if (selected) {
    const idSet1 = new Set();
    vm.selectedRows = vm.selectedRows.concat(changeRows).filter((item) => {
      if (idSet1.has(item.id)) {
        return false;
      }
      idSet1.add(item.id);
      return true;
    });
    if (selectEntry in vm) {
      const idSet2 = new Set();
      vm[selectEntry] = vm[selectEntry].concat(changeRows).filter((item) => {
        if (idSet2.has(item.id)) {
          return false;
        }
        idSet2.add(item.id);
        return true;
      });
    }
  } else {
    changeRows.forEach((item) => {
      vm.selectedRows = vm.selectedRows.filter((entry) => {
        return entry.id !== item.id;
      });
      if (selectEntry in vm) {
        vm[selectEntry] = vm[selectEntry].filter((entry) => {
          return entry.id !== item.id;
        });
      }
    });
  }
}

/**
 * 复选框全选事件处理函数
 * @param {VueInstance} vm - Vue 实例
 */
export function selectAllEntry(vm) {
  // console.log("全选");
  vm.selectedRowKeys = [];
  vm.selectedRows = [];
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
  });
  // console.log("全选后的行", vm.selectedRows);
}

/**
 * 复选框反选事件处理函数
 * @param {VueInstance} vm - Vue 实例
 */
export function clearAllEntry(vm) {
  // console.log("反选");
  vm.selectedRowKeys = [];
  vm.selectedRows = [];
}