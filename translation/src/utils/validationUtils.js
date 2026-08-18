/**
 * 表单校验工具函数
 * 包含表单验证规则设置、词条校验等功能
 */
import { cloneDeep } from 'lodash';
import { checkSykEntryBeforeSave } from "@/http/api/glossary";
import { entryAllCols } from "@/constants/commonParam.js";
import { mapValueToLabel } from "@/utils/dataStructureUtils";

/**
 * 根据列 value 获取用户友好列名（label）
 * @param {string} value
 * @returns {string}
 */
export function getColumnLabelByValue(value) {
  if (!value) return "";
  const label = mapValueToLabel([value], entryAllCols)[0];
  return label || value;
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
 * 获取记录指定列（词条/翻译）的最大长度
 * @param {Object} record - 当前记录对象
 * @param {Object} vm - Vue 实例对象
 * @param {String} colName - 两种最大长度
 * 1. 词条："maxByte"；
 * 2. 翻译（具体语种english,chinese,...）:"foreignMaxByte"
 * @returns {number|null} - 最大长度，如果不存在则返回 null
 */
export function getMaxLength(record_, vm, colName = "foreignMaxByte") {
  let record = record_;
  if (vm.editableData[record.id]) {// 若处于编辑态，则使用编辑态数据
    record = vm.editableData[record.id];
  }
  if (!record.classfy1) {
    // console.log("无一级分类", record)
    return record.maxLength || null;
  }
  // console.log("长度的相关信息：", vm.classifyLimit, record.classfy1, colName)
  if (colName == "foreignMaxByte") {
    // console.log("获取最大长度:", vm.classifyLimit?.[record.classfy1]?.foreignMaxByte);
    return vm.classifyLimit?.[record.classfy1]?.foreignMaxByte || null;
  }
  else if (colName == "maxByte") {
    // console.log("获取最大长度:", vm.classifyLimit?.[record.classfy1]?.maxByte);
    return vm.classifyLimit?.[record.classfy1]?.maxByte || null;
  }
}

/**
 * 从 form ref 名解析 recordId（form{idNoDash}{columnKey}）
 */
export function resolveRecordIdFromFormRef(vm, refName, columnKey) {
  const prefix = "form";
  if (!refName?.startsWith(prefix) || !columnKey) return null;
  const idPart = refName.slice(prefix.length, refName.length - columnKey.length);
  for (const recordId of Object.keys(vm.editableData || {})) {
    if (recordId.replaceAll("-", "") === idPart) return recordId;
  }
  for (const row of vm.dataSource || []) {
    if (row.id?.replaceAll("-", "") === idPart) return row.id;
  }
  return null;
}

/**
 * 命令式校验单元格（不依赖单元格内 a-form）
 * @param {Object} vm
 * @param {string} recordId
 * @param {string} columnKey
 * @returns {Promise<void>}
 */
export async function validateEditableCell(vm, recordId, columnKey) {
  const rules = vm.rules?.[recordId]?.[columnKey];
  if (!rules?.length) return;

  const value = vm.editableData?.[recordId]?.[columnKey];

  const hasRequiredRule = rules.some((r) => r.required);
  if (!hasRequiredRule && isBlankTranslation(value)) return;

  const columnName = getColumnLabelByValue(columnKey);

  for (const rule of rules) {
    if (rule.required && (value == null || value === "")) {
      const errorMessage =
        typeof rule.message === "string" ? rule.message : "请输入!";
      return Promise.reject({ columnName, errorMessage });
    }
    if (rule.validator) {
      try {
        await rule.validator(rule, value);
      } catch (err) {
        const errorMessage =
          typeof err === "string"
            ? err
            : err?.message || String(err || "校验失败");
        return Promise.reject({ columnName, errorMessage });
      }
    }
  }
}

export function clearCellError(vm, recordId, columnKey) {
  if (vm.cellErrors?.[recordId]?.[columnKey]) {
    delete vm.cellErrors[recordId][columnKey];
    if (Object.keys(vm.cellErrors[recordId]).length === 0) {
      delete vm.cellErrors[recordId];
    }
  }
}

export function setCellError(vm, recordId, columnKey, errorMessage) {
  if (!vm.cellErrors) vm.cellErrors = {};
  if (!vm.cellErrors[recordId]) vm.cellErrors[recordId] = {};
  vm.cellErrors[recordId][columnKey] = errorMessage;
}

/**
 * 编辑态单元格输入 SSOT：写 editableData + 仅用户改值时清校验红字
 */
export function onEditableCellInput(vm, recordId, columnKey, value) {
  const row = vm.editableData?.[recordId];
  if (!row) return;
  const prev = row[columnKey];
  row[columnKey] = value;
  if (value !== prev) clearCellError(vm, recordId, columnKey);
}

/**
 * 批量清除指定行的单元格错误
 */
export function clearCellErrorsForRecords(vm, recordIds = []) {
  if (!vm.cellErrors) return;
  for (const recordId of recordIds) {
    delete vm.cellErrors[recordId];
  }
}

/**
 * 命令式校验并写入 cellErrors（不 throw，供 bulk 校验链路使用）
 */
export async function applyCellValidationAfterOpenEdit(vm, recordId, columnKey) {
  try {
    await validateEditableCell(vm, recordId, columnKey);
    clearCellError(vm, recordId, columnKey);
  } catch (err) {
    const errorMessage =
      err?.errorMessage || err?.message || String(err || "校验失败");
    setCellError(vm, recordId, columnKey, errorMessage);
  }
}

/**
 * 使用校验规则（命令式；保留 refName 签名以兼容既有调用）
 */
export async function useRefRules(refs, refName, columnValue, vm) {
  if (vm && columnValue) {
    const recordId = resolveRecordIdFromFormRef(vm, refName, columnValue);
    if (recordId) {
      return validateEditableCell(vm, recordId, columnValue);
    }
  }

  const formRef = refs?.[refName];
  if (formRef) {
    try {
      await formRef.validate();
      return Promise.resolve();
    } catch (err) {
      if (columnValue) {
        const columnName = getColumnLabelByValue(columnValue);
        const errors = err?.errorFields?.[0]?.errors || [];
        const errorMessage = Array.isArray(errors)
          ? errors.join("；")
          : String(errors || "");
        return Promise.reject({ columnName, errorMessage });
      }
      return Promise.reject(
        `编辑-保存校验失败:${err?.errorFields?.[0]?.errors}`
      );
    }
  }

  if (vm && columnValue) {
    const recordId = resolveRecordIdFromFormRef(vm, refName, columnValue);
    if (recordId) {
      return validateEditableCell(vm, recordId, columnValue);
    }
  }

  return Promise.reject(new Error(`未找到 ref 名称为 "${refName}" 的表单引用`));
}

/**
 * 设置校验规则
 * 为指定的数据记录设置表单校验规则。
 * 通常在打开编辑框时调用，编辑时实时校验（编辑时的使用校验规则是无须显式写出来的）
 *
 * @param {Object} vm - Vue 组件实例，需包含 rules 对象用于存储校验规则（如 this.rules = {}）
 * @param {Object} record - 当前正在编辑的数据记录，需包含唯一标识字段 id，以及其他待校验字段
 * @param {Array<string>} cols - 需要设置校验规则的字段名数组，如 ["entry", "english", "chinese"]
 * @returns {void}
 */
export function setRefRules(vm, record, cols) {
  vm.rules[record.id] = {};
  for (const col of cols) {
    if (col === "entry") {
      continue;// 不给词条列设置校验规则了
      vm.rules[record.id][col] = [
        { validator: validateRefRules(record, vm, "maxByte", "") },
        { required: true, message: "请输入!" },
      ];
    }
    else {
      const translateRules = buildTranslateRules(vm, record, col);
      vm.rules[record.id][col] = translateRules;
    }
  }
}

export function getEnabledVerifyMethods(vm) {
  const options = Array.isArray(vm?.rulesOptions) ? vm.rulesOptions : null;
  if (!options) return ["toLong", "special"];
  return options.filter((o) => o.checked).map((o) => o.key);
}

export function buildTranslateRules(vm, record, columnKey) {
  const verifyMethods = getEnabledVerifyMethods(vm);
  if (verifyMethods.length === 0) return [];
  return [
    {
      validator: validateRefRules(
        record,
        vm,
        "foreignMaxByte",
        columnKey,
        verifyMethods
      ),
    },
  ];
}

/**
 * 定义校验规则（通过.validate执行）
 * 校验器工厂函数，根据字段名返回一个异步校验函数。
 * 用于校验输入内容的字节长度和特殊字符是否翻译一致。
 *
 * @param {Object} vm - Vue 组件实例，需包含 rules 对象用于存储校验规则（如 this.rules = {}）
 * @param {Object} record - 当前数据记录，包含字段如 id, entry 等
 * @param {string} colName - 当前校验的字段类型，比如 "maxByte" 或 "foreignMaxByte"，用于区分校验策略
 * @param {string} language - 当前校验的语种类型，如english,chinese
 * @returns {(rule: any, value: any) => Promise<void>} - 返回一个异步校验函数，符合 Element Plus 的 validator 要求
 */
/**
 * 翻译列空白判定（null / "" / 纯空白字符视为空白）
 */
export function isBlankTranslation(value) {
  return value == null || String(value).trim() === "";
}

export function validateRefRules(record, vm, colName, language,
  verifyMethods = ["toLong", "special"]) {
  return async (rule, value) => {
    if (language && isBlankTranslation(value)) {
      return Promise.resolve();
    }
    if (verifyMethods.includes("toLong")) {
      const maxLength = getMaxLength(record, vm, colName);
      let length = byteLength(value);
      if (maxLength && length > maxLength) {
        // 表单项内仍使用字符串以保证控件正常显示错误；外层聚合展示由 useRefRules/editSave 负责
        return Promise.reject(`允许最大字符数为${maxLength}(1中文=2字符)`);
      }
    }

    if (language && verifyMethods.includes("special")) {// 需要拿翻译与词条进行比较，所以词条本身不需要进行特殊字符校验
      const datas = [
        {
          id: record.id,
          entry: record.entry,
          translate: vm.editableData[record.id]
            ? vm.editableData[record.id][language]
            : record[language],
        },
      ];
      // console.log("校验特殊字符", datas);
      let specialCharNum = 0;
      try {
        const res = await checkSykEntryBeforeSave(datas);//调用后端接口
        specialCharNum = res.data?.length ?? 0;
      } catch (err) { }
      if (specialCharNum > 0)
        // 只要 res.data 非空即视为失败（后端返回 data 表示不通过）
        return Promise.reject(`特殊字符不一致\r\n(如%1翻译成% 1)`);
    }

    return Promise.resolve();
  };
}

/**
 * 将指定记录设置为编辑状态，并为其配置校验规则
 * - 需要放到函数里使用，展开来用会报错找不到表单引用
 * - 原名：addEdit
 * @param {Object} record - 需要设置为编辑状态的记录对象
 * @param {Array<string>} cols - 需要设置校验规则的列名数组
 * 1. 工作台：翻译列为指定语种，如["english"]
 * 2. 词条管理：多列，如["entry", "english", "chinese"]
 * @param {Object} vm - Vue 实例对象
 * @returns {Promise} - 一个立即解决的 Promise 对象
 */
export function openSetEdit(record, cols, vm) {
  // 打开编辑态
  vm.editableData[record.id] = vm.editableData[record.id] || cloneDeep(record);
  // 设置校验规则
  setRefRules(vm, record, cols)
  return Promise.resolve();
}

/**
 * 校验词条数组（工作台场景）-当前页数据版
 * 1.-保存前（区分通过/不通过校验词条）
 * 2.-不通过的打开编辑态
 * @param {Object} pagination - 分页信息对象，包含 `current`（当前页码）和 `pageSize`（每页显示数量）属性
 * @param {string} language - 当前语种类型，用于指定要校验的翻译字段
 * @param {Object} vm - Vue 实例对象，包含 `dataSource`（数据源）等属性
 */
export async function verifyArray_workbench_page(pagination, language, vm,
  verifyMethods) {
  let data = vm.dataSource.slice(
    (pagination.current - 1) * pagination.pageSize,
    pagination.current * pagination.pageSize
  );
  // console.log("当前页校验", data)
  await verifyArray_workbench(vm, data, language, verifyMethods);
}

/**
 * 校验词条数组（工作台场景）
 * 1.-保存前（区分通过/不通过校验词条）
 * 2.-不通过的打开编辑态
 * @param {Object} vm - Vue 实例对象
 * @param {Array<Object>} array - 待校验的词条数组
 * @param {string} language - 编辑数据用任务语种存储了起来：english,chinese,...，
 *   - 如英文任务存储到：editableData[record.id].[english]
 * @param {Array<string>} verifyMethods - 需要执行的校验方法集合，可选值：
 *   - "toLong": 执行长度校验（检查翻译内容是否超过最大长度限制）
 *   - "special": 执行特殊字符校验（检查翻译与原文的特殊字符是否一致）
 * @returns {Promise<Object>} 校验结果对象，包含以下 Set 类型的属性：
 *   - acceptIds: 通过所有校验的记录ID集合
 *   - toLongIds: 长度超标的记录ID集合
 *   - specialIds: 特殊字符校验不通过的记录ID集合
 */
export async function verifyArray_workbench(vm, array, language,
  verifyMethods) {
  const methods = verifyMethods ?? getEnabledVerifyMethods(vm);
  let arr = {
    acceptIds: new Set(),// 所有校验通过
    errorIds: new Set(),// 所有校验不通过
    toLongIds: new Set(),// 校验长度
    specialIds: new Set(),// 校验特殊字符
  };
  const datas = [];
  for (const record of array) {
    const data = {
      id: record.id,
      entry: record.entry,
      translate: vm.editableData[record.id]?.[language] || record[language],
      maxLength: getMaxLength(record, vm),
    };
    if (isBlankTranslation(data.translate)) {
      arr.acceptIds.add(record.id);
      continue;
    }
    datas.push(data);
    if (methods.includes("toLong")) {
      if (data.maxLength && byteLength(data.translate) > data.maxLength) {
        arr.toLongIds.add(record.id);
      }
    }
  }

  if (methods.includes("special") && datas.length > 0) {
    try {
      const res = await checkSykEntryBeforeSave(datas);
      arr.specialIds = new Set(res.data?.map(item => item.id));
    } catch (err) { }
  }

  for (const record of array) {
    if (arr.acceptIds.has(record.id)) continue;
    if (!arr.toLongIds.has(record.id) && !arr.specialIds.has(record.id)) {
      arr.acceptIds.add(record.id);
    }
    else {
      // 注意：单条验证会遍历触发checkSykEntryBeforeSave，不要整个dataSource都遍历，一次遍历检查当前页的表单校验即可
      arr.errorIds.add(record.id);
      await openSetEdit(record, [language], vm);
      await applyCellValidationAfterOpenEdit(vm, record.id, language);
      if (typeof vm.showEditOperation === "function") {
        vm.showEditOperation();
      }
    }
  }
  // console.log("函数内", arr)
  return arr;
}

/**
 * 校验词条（词条管理场景）
 * 1.-保存前（是否通过）
 * 2.-不通过就打开编辑态
 * @param {Object} vm - Vue 实例对象
 * @param {Object} record - 待校验的词条（新增/修改/复制）
 * @param {Array<string>} colList - 新增/修改词条需要同时校验多列：[entry, english，chinese,...]
 * @param {Array<string>} verifyMethods - 需要执行的校验方法集合，可选值：
 *   - "toLong": 执行长度校验（检查翻译内容是否超过最大长度限制）
 *   - "special": 执行特殊字符校验（检查翻译与原文的特殊字符是否一致）
 * @returns {Promise<boolean>} 校验结果
 */
export async function verifyRecord_entry(vm, record, colList,
  verifyMethods) {
  const methods = verifyMethods ?? getEnabledVerifyMethods(vm);
  let flag = true;

  if (methods.includes("toLong")) {// 校验长度
    for (const col of colList) {
      if (col == "entry") {
        const maxLength = getMaxLength(record, vm, "maxByte");
        if (maxLength && byteLength(record[col]) > maxLength) {
          flag = false;// 词条长度超限
        }
      }
      else {
        const maxLength = getMaxLength(record, vm, "foreignMaxByte");
        if (maxLength && byteLength(record[col]) > maxLength) {
          flag = false;// xx翻译长度超限
        }
      }
    }
  }
  if (methods.includes("special")) {// 校验特殊字符
    const datas = [];
    for (const language of colList) {
      if (language == "entry") continue;
      const data = {
        id: record.id,
        entry: record.entry,
        translate: vm.editableData[record.id]?.[language] || record[language],
      };
      datas.push(data);
    }
    try {
      const res = await checkSykEntryBeforeSave(datas);//调用后端接口
      const specialCharNum = res.data?.length ?? 0;
      if (specialCharNum > 0)
        flag = false;// 存在特殊字符翻译不一致
    } catch (err) { }
  }

  if (flag) {
    return true;
  } else {
    await openSetEdit(record, colList, vm);
    for (const col of colList) {
      if (col === "entry") continue;
      await applyCellValidationAfterOpenEdit(vm, record.id, col);
    }
    return false;
  }
}
