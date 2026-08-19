/**
 * 表单校验工具函数
 * 包含表单验证规则设置、词条校验等功能
 *
 * 规则 SSOT：RulesDropdown → vm.rulesOptions → getMethods(vm)
 * 校验只在两处触发：行内 ✓（saveEdit）与底部保存（verifyArray_workbench / classifyArr）
 * 双击只 openSetEdit，不跑 applyCell（避免未勾选 special 仍调接口、或进编辑却无红字）
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
 * 按当前 rules 写/清该单元格 cellErrors（不 throw）
 * 行内 ✓ / 词条管理 verifyRecord_entry 用；bulk 失败行改走 openFailRows 按判定结果打红字
 */
export async function applyCell(vm, recordId, columnKey) {
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

/**
 * 当前已勾选的校验键（toLong / special）
 * 原名 getEnabledVerifyMethods。无 rulesOptions 时与历史默认一致：两项全开
 */
export function getMethods(vm) {
  const options = Array.isArray(vm?.rulesOptions) ? vm.rulesOptions : null;
  if (!options) return ["toLong", "special"];
  return options.filter((o) => o.checked).map((o) => o.key);
}

export function buildTranslateRules(vm, record, columnKey) {
  // 始终挂 validator，执行时再 getMethods；避免打开编辑时把勾选快照进闭包
  return [
    {
      validator: validateRefRules(
        record,
        vm,
        "foreignMaxByte",
        columnKey
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

/** 屏幕展示值：有编辑行则用其字段（含空串），否则浏览态 */
export function shownTranslate(vm, record, language) {
  const edit = vm.editableData?.[record.id];
  if (edit) return edit[language];
  return record[language];
}

/** 已加载词条一人一行：展平 dataSource 树；editableData 孤儿才补入，不把同一 id 塞两份 */
function flattenLoaded(vm) {
  const byId = new Map();
  const walk = (rows) => {
    for (const r of rows || []) {
      if (r?.id != null && !byId.has(r.id)) byId.set(r.id, r);
      if (r.children?.length) walk(r.children);
    }
  };
  walk(vm.dataSource);
  for (const row of Object.values(vm.editableData || {})) {
    if (row?.id != null && !byId.has(row.id)) byId.set(row.id, row);
  }
  return [...byId.values()];
}

/** 超长红字；validateRefRules 与 openFailRows 共用，避免文案漂移 */
function msgToLong(maxLength) {
  return `允许最大字符数为${maxLength}(1中文=2字符)`;
}

/** 特殊字符红字；validateRefRules 与 openFailRows 共用 */
const MSG_SPECIAL = "特殊字符不一致\r\n(如%1翻译成% 1)";

export function validateRefRules(record, vm, colName, language,
  verifyMethods) {
  return async (rule, value) => {
    const methods = verifyMethods ?? getMethods(vm);
    if (language && isBlankTranslation(value)) {
      return Promise.resolve();
    }
    if (methods.includes("toLong")) {
      const maxLength = getMaxLength(record, vm, colName);
      let length = byteLength(value);
      if (maxLength && length > maxLength) {
        // 表单项内仍使用字符串以保证控件正常显示错误；外层聚合展示由 useRefRules/editSave 负责
        return Promise.reject(msgToLong(maxLength));
      }
    }

    if (language && methods.includes("special")) {// 需要拿翻译与词条进行比较，所以词条本身不需要进行特殊字符校验
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
        return Promise.reject(MSG_SPECIAL);
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
 * 1. 保存前区分通过/不通过
 * 2. 不通过的打开编辑态（由 verifyArray_workbench → openFailRows 完成）
 * @param {Object} pagination - current / pageSize
 * @param {string} language - 当前任务翻译列，如 english
 * @param {Object} vm - 含 dataSource、rulesOptions
 * @param {string[]} [methods] - 未传时 verifyArray_workbench 内 getMethods(vm)（导入后/翻页依赖此默认）
 */
export async function verifyArray_workbench_page(pagination, language, vm,
  methods) {
  let data = vm.dataSource.slice(
    (pagination.current - 1) * pagination.pageSize,
    pagination.current * pagination.pageSize
  );
  await verifyArray_workbench(vm, data, language, methods);
}

/**
 * 纯判定：按 methods 分成 accept/error，无 UI 副作用
 * 与 openFailRows 拆开，避免「bulk 按全开规则失败、红字按勾选通过」的分裂态
 * 若行仍在编辑态，展示值取 editableData（含空串），否则 dataSource
 * @param {string[]} [methods] toLong / special；未传则 getMethods(vm)
 * @returns {Promise<{acceptIds:Set, errorIds:Set, toLongIds:Set, specialIds:Set}>}
 */
export async function classifyArr(vm, array, language, methods) {
  const m = methods ?? getMethods(vm);
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
      translate: shownTranslate(vm, record, language),
      maxLength: getMaxLength(record, vm),
    };
    if (isBlankTranslation(data.translate)) {
      arr.acceptIds.add(record.id);
      continue;
    }
    datas.push(data);
    if (m.includes("toLong")) {
      if (data.maxLength && byteLength(data.translate) > data.maxLength) {
        arr.toLongIds.add(record.id);
      }
    }
  }

  if (m.includes("special") && datas.length > 0) {
    // special 未勾选时不得请求 checkSykEntryBeforeSave
    try {
      const res = await checkSykEntryBeforeSave(datas);
      arr.specialIds = new Set(res.data?.map(item => item.id));
    } catch (err) { }
  }

  for (const record of array) {
    if (arr.acceptIds.has(record.id)) continue;
    if (!arr.toLongIds.has(record.id) && !arr.specialIds.has(record.id)) {
      arr.acceptIds.add(record.id);
    } else {
      arr.errorIds.add(record.id);
    }
  }
  return arr;
}

/**
 * 仅对 classifyArr 得到的 errorIds 开编辑态并写红字
 * 红字由 toLongIds / specialIds 直接 setCellError，不再 applyCell（避免 special 再打单条接口）
 * 两者都失败时优先超长文案，与 validateRefRules 先 toLong 再 special 一致
 */
export async function openFailRows(vm, array, arr, language) {
  for (const record of array) {
    if (!arr.errorIds.has(record.id)) continue;
    await openSetEdit(record, [language], vm);
    if (arr.toLongIds.has(record.id)) {
      setCellError(vm, record.id, language, msgToLong(getMaxLength(record, vm)));
    } else if (arr.specialIds.has(record.id)) {
      setCellError(vm, record.id, language, MSG_SPECIAL);
    }
    // 页面有同名实例方法时走页面（模板绑定）；单测 mock 也可接到这里
    if (typeof vm.showEditOperation === "function") {
      vm.showEditOperation();
    } else {
      showEditOperation(vm);
    }
  }
}

/**
 * 校验词条数组（工作台场景）
 * 1. 保存前区分通过/不通过（classifyArr）
 * 2. 不通过的打开编辑态（openFailRows）
 * 导入后/分页未传 methods 时内部 getMethods(vm)
 */
export async function verifyArray_workbench(vm, array, language, methods) {
  const arr = await classifyArr(vm, array, language, methods ?? getMethods(vm));
  await openFailRows(vm, array, arr, language);
  return arr;
}

/**
 * 勾选变化 / 导入后：按展示值对已加载表一人一行复检
 * 通过：编辑态退回浏览并写回展示译文；失败：浏览态进编辑打红字
 * methods 为空全部视为通过，不调 special
 */
export async function revalidateLoaded(vm, transCol) {
  const rows = flattenLoaded(vm);
  if (!rows.length) return;
  clearCellErrorsForRecords(vm, rows.map((r) => r.id));
  const methods = getMethods(vm);
  const acceptIds = new Set();
  if (methods.length === 0) {
    for (const r of rows) acceptIds.add(r.id);
  } else {
    const arr = await classifyArr(vm, rows, transCol, methods);
    await openFailRows(vm, rows, arr, transCol);
    for (const id of arr.acceptIds) acceptIds.add(id);
  }
  const byId = new Map(rows.map((r) => [r.id, r]));
  for (const id of acceptIds) {
    const edit = vm.editableData?.[id];
    if (!edit) continue;
    const rec = byId.get(id);
    if (rec) rec[transCol] = edit[transCol];
    delete vm.editableData[id];
    clearCellError(vm, id, transCol);
  }
  hideEditOperation(vm);
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
  const methods = verifyMethods ?? getMethods(vm);
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
        translate: shownTranslate(vm, record, language),
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
      await applyCell(vm, record.id, col);
    }
    return false;
  }
}

/**
 * 显示表格「编辑操作」列。四页逻辑相同，抽到此处避免再复制一份
 * 若列已存在则不再添加；index:101 保证在最右侧
 */
export function showEditOperation(vm) {
  if (!Array.isArray(vm.columns) || vm.columns.length === 0) return;
  if (vm.columns.at(-1).dataIndex === "editOperation") return; // 已有编辑操作列
  vm.columns.push({
    title: "编辑操作",
    dataIndex: "editOperation",
    align: "center",
    width: 100,
    resizable: true,
    fixed: "right",
    index: 101, // 确保该列在最右侧
  });
}

/** 删除操作列：editableData 空时收起「编辑操作」列 */
export function hideEditOperation(vm) {
  if (!Array.isArray(vm.columns)) return;
  if (Object.keys(vm.editableData || {}).length === 0) {
    vm.columns = vm.columns.filter((item) => item.dataIndex != "editOperation");
  }
}

/** 取消编辑：丢弃 editableData 并可能收起编辑列 */
export function cancelEdit(vm, recordId) {
  delete vm.editableData[recordId];
  hideEditOperation(vm);
}

/**
 * 行内 ✓：校验 → commit → 退出编辑态
 * commit 由各页传入（写回字段/状态策略不同）；失败则保留编辑态并 setCellError
 */
export async function saveEdit(vm, record, { transCol, commit }) {
  try {
    await validateEditableCell(vm, record.id, transCol);
  } catch (err) {
    setCellError(
      vm,
      record.id,
      transCol,
      err?.errorMessage || err?.message || String(err)
    );
    return false;
  }
  commit(record, vm.editableData[record.id]);
  delete vm.editableData[record.id];
  clearCellError(vm, record.id, transCol);
  hideEditOperation(vm);
  return true;
}
