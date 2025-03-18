import common from "@/views/workbench/common.js";

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
export function setTableHeight(vm, buttonHeightBias = 8, tableHeightBias = 150) {
  vm.$nextTick(() => {
    // 设置列表父元素高度
    let box = vm.$refs.box.offsetHeight;
    let searchHeight = vm.$refs.search.$el.offsetHeight;
    try {
      let operationAreaHeight = vm.$refs.operationArea.$el.offsetHeight;
      vm.dataHeight = box - searchHeight - operationAreaHeight;
    } catch (error) {
      vm.dataHeight = box - searchHeight;
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
export function pageChange(vm, page, pageSize, fetchData) {
  vm.pagination.current = page;
  vm.pagination.pageSize = pageSize;
  // 调用传入的查询接口函数
  fetchData();
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
