import { cloneDeep } from 'lodash'; // 使用 lodash 的 cloneDeep
import { message } from "ant-design-vue";
// import common from "@/views/workbench/common.js";
// import tableParam from '@/views/entry/tableParam';
import { entryParams as tableParam } from "@/utils/commonParam.js";
import commonParam from "@/utils/commonParam.js";
import { cancelRequest, cancelAllRequests } from "@/http/request";
const requestDelId = [];// 存储删除请求的id，用于保留loading状态
// 每个函数都带有JSDoc注释，用于描述函数的功能、参数和返回值

/**
 * 从 filterSource 中移除 arr 中的数据(包括children)
 * @param {Array} filterSource - 需要过滤的源数组，每个对象必须有一个唯一的 id 属性
 * @param {Array} arr - 包含要移除的对象数组，每个对象必须有一个唯一的 id 属性
 * @returns {Array} - 过滤后的数组
 */
export function filter_arr_with_children(filterSource, arr) {
  filterSource = filterSource.filter((item) => {
    return !arr.some(
      (arrItem) => arrItem.id === item.id
    );
  });
  return filterSource;

  // this.selectedRows.forEach((item) => {
  //           if (item.entryState === 2) {
  //             // 词条审核未通过
  //             deleteCount++;

  //             deleteID.push(item.id); // 否则子词条还会重复加一遍

  //             // 若存在子词条  则删除子词条
  //             if (item.children && item.children.length > 0) {
  //               item.children.forEach((child) => {
  //                 deleteID.push(child.id);
  //               });
  //             }
  //           }
  //         });
}

/**
 * 从 filterSource 中移除 arr 中的数据
 * @param {Array} filterSource - 需要过滤的源数组，每个对象必须有一个唯一的 id 属性
 * @param {Array} arr - 包含要移除的对象数组，每个对象必须有一个唯一的 id 属性
 * @returns {Array} - 过滤后的数组
 */
export function filter_arr(filterSource, arr) {
  filterSource = filterSource.filter((item) => {
    return !arr.some(
      (arrItem) => arrItem.id === item.id
    );
  });
  return filterSource;
}

/**
 * 从 filterSource 中移除 arr 中的数据
 * @param {Array} filterSource - 需要过滤的源数组，每个元素是一个唯一的键值,代表id属性
 * @param {Array} arr - 包含要移除的对象数组，每个元素是一个唯一的键值,代表id属性
 * @returns {Array} - 过滤后的数组
 */
export function filter_arr_keys(filterSource, arr) {
  filterSource = filterSource.filter((key) => {
    return !arr.some(
      (arrItem) => arrItem.id === key
    );
  });
  return filterSource;
}

/**
 * 释义替换翻译
 * 将选中行的释义字段替换为对应语言的值，并更新数据源
 * @param {Object} vm - Vue 实例对象
 * @param {Array} vm.selectedRows - 选中的行数据数组
 * @param {Array} vm.dataSource - 表格数据源数组
 * @param {Array} vm.allData - 所有数据数组
 * @param {Object} commonParam - 公共参数对象，包含语言列表
 * @returns {void}
 */
export function interpretation2value(vm) {
  // 遍历选中的行
  vm.selectedRows.forEach((row) => {
    // 遍历语言列表
    commonParam.languageList.forEach((lang) => {
      const langValue = lang.value;
      const interpretationKey = lang.interpretation;
      if (row.hasOwnProperty(interpretationKey)) {
        // 替换释义为对应语言的值
        row[langValue] = row[interpretationKey];
      }
    });
  });

  // 更新 vm.dataSource
  if (vm.dataSource) {
    const newdataSource = vm.dataSource.map((item) => {
      const selectedRow = vm.selectedRows.find((row) => row.id === item.id);
      return selectedRow ? { ...item, ...selectedRow } : item;
    });
    vm.dataSource = newdataSource;
  }
  // 更新 vm.allData
  if (vm.allData) {
    const newallData = vm.allData.map((item) => {
      const selectedRow = vm.selectedRows.find((row) => row.id === item.id);
      return selectedRow ? { ...item, ...selectedRow } : item;
    });
    vm.allData = newallData;
  }
  // 强制更新表格
  vm.$nextTick(() => {
    if (vm.$refs.workTable && vm.$refs.workTable.reload) {
      vm.$refs.workTable.reload();
    }
  });
}

/**
 * 从本地存储读取用户列偏好并更新组件状态
 * @param {string} colPrefName - 存储用户列偏好的 localStorage 键名
 * @param {Array} normalWidth - 表格列的默认宽度数组
 * @param {Object} vm - Vue 实例对象，包含 `checkedColumn` 和 `changeColumn` 方法
 * @param {boolean} needFilter - 是否需要过滤
 */
export function getColPref(colPrefName, normalWidth, vm, needFilter = false) {
  // 读取本地存储的用户偏好
  const storedPreferences = localStorage.getItem(colPrefName);
  // console.log("storedPreferences", storedPreferences)
  if (storedPreferences) {
    const colPref_strList = JSON.parse(storedPreferences).displayColumn.split(",");
    // console.log("colPref_strList", colPref_strList);
    // 调用 changeColumn 方法更新列显示
    changeColumn(colPrefName, normalWidth, colPref_strList, vm, needFilter = needFilter);
  }
}

/**
 * 根据用户勾选的列配置表格列展示，并将用户偏好保存到 localStorage
 * @param {string} colPrefName - 存储用户列偏好的 localStorage 键名
 * @param {Array} normalWidth - 表格列的默认宽度数组
 * @param {Array} checkedValue - 用户勾选的列值数组
 * @param {Object} vm - Vue 实例对象，包含 `checkedColumn`、`checkboxList`、`columns` 等属性
 * @param {boolean} needFilter - 是否需要过滤
 */
export function changeColumn(colPrefName, normalWidth, colPref_strList, vm, needFilter = false) {
  // 全部的展示列复选框vm.checkboxList
  // 勾选的展示列复选框vm.checkedColumn
  // 表格列的复选框vm.columns
  if (vm.checkedColumn)
    vm.checkedColumn = colPref_strList;

  if (vm.checkboxList && vm.checkboxList.length > 0 && vm.columns && vm.columns.length > 0) {// 如果有展示列，则要和列比较
    vm.checkboxList.forEach((value) => {
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
      // console.log("没有展示列：", colPref_strList)
      // 使用 find 方法查找对应的 checkboxList 项
      const col = tableParam.checkboxList.find(item => item.value === colPref_strList[i]);
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
  // 初始化列配置对象
  let newCol = {
    title: value.label,
    dataIndex: value.value,
    align: "center",
    width: normalWidth,
    ellipsis: true,
    resizable: true,
    index: value.index,
  };

  // 根据列数据索引设置固定位置
  if (["isExist", "translateState", "entry"].includes(newCol.dataIndex)) {
    newCol.fixed = "left";
  }
  if (["auditSuggess", "entryState"].includes(newCol.dataIndex)) {
    newCol.fixed = "right";
  }

  if (needFilter) {
    // 若列数据索引为 "entrySource"，添加筛选功能
    /**
     * entrySource 列：用于筛选 entrySource 字段的数据，通常该字段可能存储着词条来源等文本信息。
     * 筛选逻辑：模糊匹配，将 record.entrySource 转换为字符串并转为小写，检查是否包含用户输入的筛选值（同样转为小写）
     * 应用场景：适用于文本类型的数据筛选，用户可以输入部分关键字来筛选出包含该关键字的所有记录。
     */
    if (newCol.dataIndex === "entrySource") {
      newCol.customFilterDropdown = true; // 使用自定义筛选下拉框
      newCol.filteredValue = null; // 初始状态下没有筛选条件
      newCol.onFilter = (filterValue, record) =>
        record.entrySource
          .toString()
          .toLowerCase()
          .includes(filterValue.toLowerCase());
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
 * 两个数组取并集
 * @param {Array} nums1 - 第一个数组
 * @param {Array} nums2 - 第二个数组
 * @return {Array} - 并集数组
 */
export function intersection(nums1, nums2) {
  if (nums1.length === 0) {
    return nums2;
  }
  if (nums2.length === 0) {
    return nums1;
  }
  let a = new Set(nums1);
  let b = new Set(nums2);
  let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
  return arr;
}

/**
 * 校验当前页数据的翻译长度
 * @param {Object} pagination - 分页信息对象，包含 `current`（当前页码）和 `pageSize`（每页显示数量）属性
 * @param {string} language - 当前语言类型，用于指定要校验的翻译字段
 * @param {Object} vm - Vue 实例对象，包含 `dataSource`（数据源）等属性
 */
export function verifyCurrentPageData(pagination, language, vm) {
  let data = vm.dataSource.slice(
    (pagination.current - 1) * pagination.pageSize,
    pagination.current * pagination.pageSize
  );
  verifyTranslationLength(data, language, vm);
}

/**
 * 校验翻译长度，统计超长记录数量，并将超长记录设为编辑状态进行表单校验
 * @param {Array} array - 待校验的记录数组
 * @param {Object} editData - 编辑状态的数据对象
 * @param {string} language - 当前语言类型
 * @param {Object} vm - Vue 实例对象
 * @returns {number} - 超长记录的数量
 */
export async function verifyTranslationLength(array, language, vm) {
  let flag = 0;
  const promises = [];

  for (const record of array) {
    const maxLength = getMaxLength(record, vm);
    if (maxLength === null) continue;

    const text = vm.editableData[record.id]?.[language] || record[language];
    if (byteLength(text) > maxLength) {
      // console.log("超长记录:", record);
      flag++;
      promises.push(handleExceedLength(record, language, vm));
    }
    // else {
    //   console.log("不长记录:", record);
    // }
  }

  await Promise.all(promises);
  return flag;
}

/**
 * 获取记录的最大长度
 * @param {Object} record - 当前记录对象
 * @param {Object} vm - Vue 实例对象
 * @returns {number|null} - 最大长度，如果不存在则返回 null
 */
export function getMaxLength(record, vm) {
  if (!record.classfy1) {
    return record.maxLength || null;
  }
  // console.log("获取最大长度:", vm.classifyLimit?.[record.classfy1]?.foreignMaxByte);
  return vm.classifyLimit?.[record.classfy1]?.foreignMaxByte || null;
}

/**
 * 处理翻译长度超过限制的记录，将其设为编辑状态并进行表单校验
 * @param {Object} record - 当前记录对象
 * @param {string} language - 当前语言类型
 * @param {Object} vm - Vue 实例对象
 * @returns {Promise<void>}
 */
export async function handleExceedLength(record, language, vm) {
  await addEdit(record, language, vm);
  const formRef = vm.$refs[`form${record.id.replaceAll('-', '')}${language}`];
  if (formRef) {
    try {
      await formRef.validate();
    } catch (err) {
      // 可根据实际需求添加错误处理逻辑
    }
  }
}

/**
 * 将指定记录设置为编辑状态，并为其配置校验规则
 * @param {Object} record - 需要设置为编辑状态的记录对象
 * @param {string} language - 当前语言类型
 * @param {Object} vm - Vue 实例对象
 * @returns {Promise} - 一个立即解决的 Promise 对象
 */
export function addEdit(record, language, vm) {
  vm.editableData[record.id] = vm.editableData[record.id] || cloneDeep(record);// 确保 vm.editableData 中有对应的记录（变成编辑态了）
  // console.log("设置编辑状态:", vm.editableData);
  // 设置校验规则
  vm.rules[record.id] = {
    entry: [
      { validator: validFieldLength(record, "chinese", vm) },
      { required: true, message: "请输入!" },
    ],
  };
  vm.rules[record.id][language] = [
    { validator: validFieldLength(record, language, vm) },
  ];
  return Promise.resolve();
}

/**
 * 校验翻译长度的函数，返回一个验证器函数，用于表单验证规则
 * @param {Object} record - 当前行的数据记录对象
 * @param {string} language - 语言类型，用于确定验证类型
 * @param {Object} vm - Vue 实例，包含分类限制信息
 * @returns {Function} - 验证器函数，接受 rule 和 value 作为参数
 */
export function validFieldLength(record, language, vm) {
  return (rule, value) => {
    const type = language === "chinese" ? "maxByte" : "foreignMaxByte";
    const maxLength = getFieldMaxLength(record, vm, type);

    if (maxLength === null) {
      return Promise.resolve();
    }

    const length = byteLength(value);
    return length > maxLength
      ? Promise.reject(`允许最大字符数为${maxLength}！`)
      : Promise.resolve();
  };
}

/**
 * 获取字段的最大长度
 * @param {Object} record - 当前记录对象
 * @param {Object} vm - Vue 实例对象
 * @param {string} type - 长度类型，如 'maxByte' 或 'foreignMaxByte'
 * @returns {number|null} - 最大长度，如果不存在则返回 null
 */
export function getFieldMaxLength(record, vm, type) {
  if (!vm.classifyLimit?.[record.classfy1]) {
    return record.maxLength || null;
  }
  return vm.classifyLimit[record.classfy1][type];
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
    // let length = common.byteLength(value);
    let length = byteLength(value);
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
 * 计算字符串的字节长度，中文及部分中文符号按 2 字节计算，其他字符按 1 字节计算。
 * @param {string|null|undefined} str - 待计算字节长度的字符串，允许传入 null 或 undefined。
 * @returns {number} - 返回字符串的字节长度，若传入 null 或 undefined 则返回 0。
 */
export function byteLength(str) {
  if (str === null || str === undefined) {
    return 0
  }
  // 去除首尾空格
  str = ("" + str).trim()
  let strlen = 0;
  for (let i = 0; i < str.length; i++) {
    if (str.charCodeAt(i) >= 0x4E00 && str.charCodeAt(i) <= 0x9FA5) {
      // 如果是汉字，则字符串长度加2
      strlen += 2;
    } else {
      strlen++;
    }
  }
  return strlen
}

/**
 * 获取当前时间并格式化为 "YYYY-MM-DD HH:mm:ss" 格式
 * @returns {string} - 格式化后的当前时间字符串
 */
export function getCurrentFormattedTime() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  const seconds = String(now.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * 获取当前时间并格式化为 "YYYYMMDDHHmmss" 格式
 * @returns {string} - 格式化后的当前时间字符串
 */
export function getCurrentStringTime() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  const seconds = String(now.getSeconds()).padStart(2, '0');
  return `${year}${month}${day}${hours}${minutes}${seconds}`;
}

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

/**
 * 设置模态框的 aria-hidden 属性为 false
 * @param {Object} vm - Vue 实例，用于调用 $nextTick 方法
 * @param {Document} _document - 文档对象，用于获取 DOM 元素
 */
// 定义一个函数，接受 _document 作为参数
export function setModalAriaHidden(vm, _document) {
  // 等待 Vue 实例的 DOM 更新完成后执行回调函数
  vm.$nextTick(() => {
    // 通过传入的文档对象获取所有类名为 'ant-modal' 的 DOM 元素
    const domArr = _document.getElementsByClassName("ant-modal");
    // 检查是否存在类名为 'ant-modal' 的元素
    if (domArr && domArr.length > 0) {
      Array.from(domArr).forEach((item) => {
        // 检查当前 'ant-modal' 元素是否存在子节点
        if (item.childNodes && item.childNodes.length > 0) {
          Array.from(item.childNodes).forEach((child) => {
            // 检查子节点是否具有 setAttribute 方法
            if (child.setAttribute) {
              // 设置子节点的 aria-hidden 属性为 'false'，表示该元素可见
              child.setAttribute("aria-hidden", "false");
            }
          });
        }
      });
    }
  });
}