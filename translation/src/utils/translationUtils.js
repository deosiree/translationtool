/**
 * 翻译相关工具函数
 * 包含释义替换翻译等功能
 */
import commonParam from "@/utils/commonParam.js";
import { verifyArray_workbench } from "./validationUtils";

/**
 * 释义替换翻译（对应语种）（暂时不重要，没改完）
 * 将选中行的释义字段替换为对应语种的值，并更新数据源
 * @param {Object} vm - Vue 实例对象
 * @param {Array} vm.selectedRows - 选中的行数据数组
 * @param {Array} vm.dataSource - 表格数据源数组
 * @param {Array} vm.allData - 所有数据数组
 * @param {Object} commonParam - 公共参数对象，包含语种列表
 * @returns {void}
 */
export async function interpretation2value_(vm, langMap, verifyMethods = ["toLong", "special"]) {
  const langValue = langMap.value;// english
  const langInter = langMap.interpretation;// englishInterpretation
  let arr = {
    acceptIds: new Set(), // 所有校验通过
    errorIds: new Set(), // 所有校验不通过
    toLongIds: new Set(), // 校验长度
    specialIds: new Set(), // 校验特殊字符
  };
  // 修改选中行(不保存就关闭窗口，就不会写入库)
  vm.selectedRows.forEach((record) => {
    // 修改编辑态内容
    if (record.hasOwnProperty(langInter)) {
      // 替换释义为对应语种的值
      record[langValue] = record[langInter];
    }
  });
  // 修改后校验（校验失败的会打开编辑框，提示用户修改）(改到这儿了)
  arr = await verifyArray_workbench(
    vm,
    vm.selectedRows,
    langValue,
    verifyMethods
  );

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
 * 释义替换翻译(兼容性)
 * 将选中行的释义字段替换为对应语种的值，并更新数据源
 * @param {Object} vm - Vue 实例对象
 * @param {Array} vm.selectedRows - 选中的行数据数组
 * @param {Array} vm.dataSource - 表格数据源数组
 * @param {Array} vm.allData - 所有数据数组
 * @param {Object} commonParam - 公共参数对象，包含语种列表
 * @returns {void}
 */
export function interpretation2value(vm) {
  // 遍历选中的行
  vm.selectedRows.forEach((row) => {
    // 遍历语种列表
    commonParam.languageList.forEach((lang) => {
      const langValue = lang.value;
      const interpretationKey = lang.interpretation;
      if (row.hasOwnProperty(interpretationKey)) {
        // 替换释义为对应语种的值
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

