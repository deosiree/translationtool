/**
 * 1. 导入默认导出
 * import commonParam from "@/utils/commonParam.js";
 * 2. 导入命名导出
 * import { entryParams } from "@/utils/commonParam.js";
 * 2.1 导入命名导出并改名
 * import { entryParams as tableParam } from "@/utils/commonParam.js";
 * 3. 同时导入默认导出和命名导出
 * import commonParam, { workbenchCommon } from "@/utils/commonParam.js";
 * 4. 使用 * as 导入所有导出
 * import * as commonModule from "@/utils/commonParam.js";
 * const commonParam = commonModule.default;
 * const namedExport = commonModule.namedExport;
 */

import { columnFilterOverlayStyle } from "@/components/ColumnFilter/overlayStyle.js";
import {
  resolvePresetCols,
  colsToFieldOptions,
} from "@/components/ColumnFilter/colPreset.js";

// 1.commonParam如下：
const default_languageList = [
  {
    name: "英文", // XX语种的中文
    value: "english", // XX语种
    state: "englishTranslateState", // XX翻译状态
    chineseState: "englishChineseState", // XX翻译状态的中文描述
    // id: "englishId",// XX语种id
    publicState: "englishPublicState", // XX发布状态
    checked: "englishChecked", // XX是否已选
    auditSuggest: "englishAuditSuggest", // XX审核意见
    transIdName: "engTransId", // XX语种的翻译id（工作台-翻译：保存编辑数据时使用了）
    interpretation: "englishInterpretation", // XX释义
  },
  {
    name: "俄文",
    value: "russian",
    state: "russianTranslateState",
    chineseState: "russianChineseState",
    // id: "russianId",
    publicState: "russianPublicState",
    checked: "russianChecked",
    auditSuggest: "russianAuditSuggest",
    transIdName: "rusTransId",
    interpretation: "russianInterpretation",
  },
  {
    name: "西文",
    value: "spanish",
    state: "spanishTranslateState",
    chineseState: "spanishChineseState",
    // id: "spanishId",
    publicState: "spanishPublicState",
    checked: "spanishChecked",
    auditSuggest: "spanishAuditSuggest",
    transIdName: "spaTransId",
    interpretation: "spanishInterpretation",
  },
  {
    name: "法文",
    value: "french",
    state: "frenchTranslateState",
    chineseState: "frenchChineseState",
    // id: "frenchId",
    publicState: "frenchPublicState",
    checked: "frenchChecked",
    auditSuggest: "frenchAuditSuggest",
    transIdName: "fraTransId",
    interpretation: "frenchInterpretation",
  },
  {
    name: "中文",
    value: "chinese",
    state: "chineseTranslateState",
    chineseState: "chineseChineseState", // （词条管理-翻译状态）中文翻译状态过于拗口
    // id: "chineseId",
    publicState: "chinesePublicState",
    checked: "chineseChecked",
    auditSuggest: "chineseAuditSuggest",
    transIdName: "zhTransId",
    interpretation: "chineseInterpretation",
  },
];
const default_languageMap = default_languageList.reduce((acc, lang) => {
  acc[lang.name] = lang;
  return acc;
}, {});
export default {
  languageList: default_languageList,
  langNameList: default_languageList.map((item) => item.name),
  langValList: default_languageList.map((item) => item.value),
  langTranslateStateList: default_languageList.map((item) => item.state),
  langCNStateList: default_languageList.map((item) => item.chineseState),
  langPublicStateList: default_languageList.map((item) => item.publicState),
  langAudSugList: default_languageList.map((item) => item.auditSuggest),
  langInterList: default_languageList.map((item) => item.interpretation),
  languageMap: default_languageMap,
  // ==================== 回写相关常量 ====================
  /**
   * 回写类型（与后端/现有接口保持兼容：仍使用历史枚举值）
   * - 注意：当前项目历史上使用了 "DEFAUT"（拼写错误）作为默认类型值，不能直接改成 "DEFAULT" 否则可能影响接口。
   */
  writeBackTypeList: [
    { label: "默认", value: "DEFAUT" },
    { label: "TS文件", value: "TS" },
    { label: "辞典", value: "DI" },
  ],
  checkboxList: [
    // 工作台-展示列
    { label: "存在状态", value: "isExist", index: 1 },
    // {label:'翻译状态',value:'translateState',index:2},
    // {label:'审核状态',value:'state',index:3},// (前端以前写的是state,我觉得写的不严谨，查了后端，后端表里没定义审核状态)
    // {label:'词条',value:'entry',index:4},
    // {label:'翻译',value:'translate',index:5},
    // {label:'释义',value:'interpretation',index:6},
    { label: "tag", value: "tag", index: 7 },
    { label: "comment", value: "comment", index: 8 },
    { label: "中文释义", value: "chineseInterpretation", index: 9 },
    { label: "中文翻译", value: "chinese", index: 10 },
    { label: "英文释义", value: "englishInterpretation", index: 11 },
    { label: "英文翻译", value: "english", index: 12 },
    { label: "俄文释义", value: "russianInterpretation", index: 13 },
    { label: "俄文翻译", value: "russian", index: 14 },
    { label: "西文释义", value: "spanishInterpretation", index: 15 },
    { label: "西文翻译", value: "spanish", index: 16 },
    { label: "法文释义", value: "frenchInterpretation", index: 17 },
    { label: "法文翻译", value: "french", index: 18 },
    { label: "一级分类", value: "classfy1", index: 19 },
    { label: "二级分类", value: "classfy2", index: 20 },
    { label: "词条来源", value: "entrySource", index: 21 },
    { label: "回写辞典", value: "diFileName", index: 22 },
    { label: "abbr", value: "abbr", index: 23 },
    { label: "外文字符上限", value: "foreignMaxByte", index: 24 },
    { label: "来源表名", value: "srcTabName", index: 25 },
    { label: "数据库记录ID", value: "dbRID", index: 26 },
    // {label:'审核意见',value:'auditSuggess',index:18},// 归档：后端传来的是auditSuggess，与翻译审核中有值的效果不一样；翻译审核和翻译处的值是前端根据翻译语种动态锁定的
    // {label:'词条状态',value:'entryState',index:19},
  ],
  departmentMap: {
    通用平台部: {
      label: "通用平台部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"], // 导入类型
      value: "common",
      classfyID: "1",
      ops: new Set([
        "needIP",
        "needExamine",
        "needDelete",
        "needBranch",
        "toolBox",
        "fileUpdate",
        "dev",
      ]),
      // -needIP是否需要IP地址(凡是包含ip的都同理）
      // -----1.工作台-导入：IP的显示与获取、回写辞典
      // -----2.工作台-归档，归档；
      // -----3.词条管理-已选词条：回写；
      // -----4.词条管理-右键-更新；
      // -----5.git推送（词条管理）;
      // -----6.配置管理-辞典管理；
      // -----7.词条管理-右键-拷贝所有（用于版本归档）（该功能为分支新建前身，已删）；
      // -needExamine是否需要提交词条审核（词条管理-已选词条）
      // -needDelete是否需要删除词条（词条管理-已选词条）
      // -needBranch是否需要分支新建（用于管理产品的版本）
      // -----1.词条管理-右键-分支新建
      // -----2.词条管理-右键-冗余校验（该功能为校验类型-冗余校验前身，已删）；
      // -----3.词条管理-classifyModal:分类的编辑-批量修改归档分支；产品的编辑-修改归档分支
      // -----4.工作台-平铺展示，分支列
      // -----5.工作台-批量选择，
      // --------5.1.工作台-批量选择，勾选后才显示多选按钮（平铺有下三角可全部选择/反选；层级无全部选择）
      // -----6.词条管理-查询条件-校验类型，（其他部门没有，默认为条件查询）
      // -toolBox是否需要全局工具栏（悬浮按钮）
      // -fileUpdate是否能通过excel文件批量写库更新
      // -----1.词条管理-右键-更新翻译(并且是管理员)；
      // -----2.词条管理-右键-去重回填(并且是管理员)；
      // -dev正在开发中的功能，暂时只放开给平台部
    },
    监控系统部: {
      label: "监控系统部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"], // 导入类型
      value: "jk",
      classfyID: "6",
      ops: new Set([
        "needIP",
        "needExamine",
        "needDelete",
        "needBranch",
        "fileUpdate",
      ]),
    },
    装置开发部: {
      label: "装置开发部",
      importTypes: ["file"], // 导入类型
      xml_temp: false, // 临时修改一下xml的样式(现已置false，改回去了)
      templateType: [
        { label: "excel新模板", value: "新模板" },
        { label: "excel旧模板", value: "旧模板" },
        { label: "excel通用模板", value: "通用模板" },
        { label: "xml可视化词条", value: "可视化词条" },
        { label: "xml装置辞典", value: "装置辞典" },
      ],
      value: "zz",
      classfyID: "2",
      ops: new Set([
        "needExamine",
        "entryState3",
        "needForbidden",
        "fileUpdate",
      ]),
      // -entryState3默认词条审核状态为“已审核”
      // -needForbidden是否需要禁用词条（词条管理-已选词条，词条管理-显示禁用/隐藏禁用）
    },
    人工智能部: {
      label: "人工智能部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"], // 导入类型
      value: "zn", // 瞎写的
      ops: new Set([]),
    },
    柔性输电系统部: {
      label: "柔性输电系统部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"], // 导入类型
      value: "rx", // 瞎写的
      classfyID: "101",
      ops: new Set([]),
    },
    default: {
      label: "公共库",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"], // 导入类型
      value: "default",
      classfyID: "3",
      ops: new Set([]),
    },
  },
  departmentList: [
    {
      label: "通用平台部",
      value: "common",
    },
    {
      label: "监控系统部",
      value: "jk",
    },
    {
      label: "装置开发部",
      value: "zz",
    },
    {
      label: "人工智能部",
      value: "zn",
    },
    {
      label: "柔性输电系统部",
      value: "rx",
    },
    {
      label: "公共库",
      value: "default",
    },
  ],
  treeScoped: [
    {
      title: "思源宏瑞",
      children: [
        {
          title: "通用平台部",
        },
        {
          title: "监控系统部",
        },
        {
          title: "装置开发部",
        },
        {
          title: "人工智能部",
        },
      ],
    },
    {
      title: "中研院",
      children: [
        {
          title: "柔性输电系统部",
        },
      ],
    },
    {
      title: "清能",
    },
    {
      title: "公共库",
    },
  ], // 公司->部门的状态树分布
  rulesOptions: [
    { key: "toLong", label: "校验字符长度", checked: true },
    { key: "special", label: "校验特殊字符", checked: true }, // %1翻成% 1
  ], // 表单校验规则
};

// 2.tableParam如下：

// ==================== 词条列设置（entry，单轨）====================
// 列逻辑见 @/components/ColumnFilter；此处仅 SSOT 数据

/**
 * 词条列全集（SSOT）。
 * hidden 规则：默认 false；在 allCols 标注 hidden:true 的列在导出等场景自动排除；
 * 仅 productEntry 用 defaults:{hidden:false} 展示全量（含 CSV 后端不支持但词条管理可看的列）。
 * visible:false 表示可选列默认不勾选，与 hidden 独立。
 * @type {import('@/components/ColumnFilter/colPreset.js').ColDef[]}
 */
export const entryAllCols = [
  { label: "序号", value: "index", index: 0, required: true, hidden: true },
  { label: "词条状态", value: "entryState", index: 1, visible: true, hidden: true },
  { label: "词条", value: "entry", index: 2, required: true },
  { label: "tag", value: "tag", index: 3, visible: true },
  { label: "comment", value: "comment", index: 4, visible: true },
  { label: "词条版本", value: "entryVersion", index: 5, visible: true, hidden: true },
  { label: "词条字符数", value: "entryLength", index: 6, visible: false },
  { label: "词条来源", value: "entrySource", index: 7, visible: false },// 可以看到.qt文件的相对路径
  { label: "中文释义", value: "chineseInterpretation", index: 8, visible: false },
  { label: "中文翻译", value: "chinese", index: 9, visible: false },
  { label: "中文翻译状态", value: "chineseTranslateState", index: 10, visible: false, hidden: true },
  { label: "英文释义", value: "englishInterpretation", index: 11, visible: false },
  { label: "英文翻译", value: "english", index: 12, visible: true },
  { label: "英文翻译状态", value: "englishTranslateState", index: 13, visible: false, hidden: true },
  { label: "俄文释义", value: "russianInterpretation", index: 14, visible: false },
  { label: "俄文翻译", value: "russian", index: 15, visible: true },
  { label: "俄文翻译状态", value: "russianTranslateState", index: 16, visible: false, hidden: true },
  { label: "西文释义", value: "spanishInterpretation", index: 17, visible: false },
  { label: "西文翻译", value: "spanish", index: 18, visible: true },
  { label: "西文翻译状态", value: "spanishTranslateState", index: 19, visible: false, hidden: true },
  { label: "法文释义", value: "frenchInterpretation", index: 20, visible: false },
  { label: "法文翻译", value: "french", index: 21, visible: true },
  { label: "法文翻译状态", value: "frenchTranslateState", index: 22, visible: false, hidden: true },
  { label: "一级分类", value: "classfy1", index: 23, visible: false },
  { label: "二级分类", value: "classfy2", index: 24, visible: false },
  { label: "辞典名称", value: "diFileName", index: 25, visible: false },// 可以看到.dic文件的相对路径
  { label: "备注", value: "remark", index: 28, visible: false },
  { label: "来源表名", value: "srcTabName", index: 29, visible: false, hidden: true },
  { label: "数据库记录ID", value: "dbRID", index: 30, visible: false, hidden: true },
  { label: "中文字符上限", value: "maxChineseLength", index: 31, visible: false },
  { label: "外文字符上限", value: "foreignMaxLength", index: 32, visible: false },
  { label: "创建人", value: "creator", index: 33, visible: false, hidden: true },
  { label: "创建时间", value: "createTime", index: 34, visible: false, hidden: true },
  { label: "修改人", value: "update", index: 35, visible: false },
  { label: "修改时间", value: "updateTime", index: 36, visible: false },
  { label: "是否最新版本", value: "isLatestVersion", index: 37, visible: false, hidden: true },
  { label: "词性备注", value: "partOfSpeech", index: 41, visible: false },
  { label: "词条所属分类", value: "classifyId", index: 42, visible: false },
  { label: "重复词条id", value: "repeatEntryId", index: 43, visible: false, hidden: true },
  { label: "环境备注", value: "environmentRemark", index: 44, visible: false, hidden: true },
  { label: "产品名", value: "productName", index: 45, visible: false },
  { label: "版本名", value: "versionName", index: 46, visible: false },
  { label: "翻译最大长度", value: "maxLength", index: 47, visible: false },// 长度超限校验：max(maxLength,foreign_max_byte)，foreign_max_byte后端没传给我，所有只能修改maxLength进行词条的长度限制
  { label: "英文术语字符数", value: "enCharLength", index: 48, visible: false },
  { label: "中文术语字符数", value: "zhCharLength", index: 49, visible: false },
  { label: "俄文术语字符数", value: "ruCharLength", index: 50, visible: false },
  { label: "西文术语字符数", value: "spaCharLength", index: 51, visible: false },
  { label: "法文术语字符数", value: "fraCharLength", index: 52, visible: false },
  { label: "英文翻译id", value: "enTransId", index: 53, visible: false, hidden: true },
  { label: "俄文翻译id", value: "ruTransId", index: 54, visible: false, hidden: true },
  { label: "西文翻译id", value: "spaTransId", index: 55, visible: false, hidden: true },
  { label: "法文翻译id", value: "fraTransId", index: 56, visible: false, hidden: true },
  { label: "中文翻译id", value: "zhTransId", index: 57, visible: false, hidden: true },
  { label: "导入类型", value: "importType", index: 58, visible: false, hidden: true },
  { label: "回写类型", value: "writeType", index: 59, visible: false, hidden: true },
  { label: "词条标签", value: "entryLabel", index: 60, visible: false, hidden: true },
  { label: "abbr", value: "abbr", index: 99, visible: true },
  { label: "操作", value: "operation", index: 100, required: true, hidden: true },
];

/**
 * 词条各页列覆盖（ovrd 推荐只写 label + 差异属性）
 * @type {Record<string, import('@/components/ColumnFilter/colPreset.js').ColPreset>}
 */
export const entryPresets = {
  /** 词条管理：唯一覆盖 allCols hidden 的场景，ColumnFilter 展示全量列 */
  productEntry: { ovrd: [], defaults: { hidden: false } },
  /** 导出字段：继承 allCols，hidden:true 列自动排除 */
  export: { ovrd: [], defaults: null },
  /** Excel 筛选：在 allCols 基础上页级差异 */
  filterExcel: {
    ovrd: [
      { label: "序号", hidden: false },
      { label: "操作", hidden: false },
      { label: "词条状态", hidden: true },
      { label: "tag", hidden: true },
      { label: "词条版本", hidden: true },
      { label: "abbr", hidden: true },
      { label: "comment", visible: true },
      { label: "英文翻译", visible: true },
      { label: "俄文翻译", visible: true },
      { label: "西文翻译", visible: true },
      { label: "法文翻译", visible: true },
    ],
    defaults: { visible: false },
  },
  /** 创建版本弹窗：继承 allCols hidden，表格需序号与操作列 */
  createVersion: {
    ovrd: [
      { label: "序号", hidden: false },
      { label: "操作", hidden: false },
    ],
    defaults: null,
  },
};

/** 导出词条的字段属性（由 export preset 派生） */
const entry_exportFields = colsToFieldOptions(
  resolvePresetCols(entryPresets.export, entryAllCols),
);
// 词条管理-查询条件的展示列
const entry_searchConditionList = [
  { label: "词条", value: "entry", index: 1 },
  { label: "词条状态", value: "state", index: 2 },
  { label: "tag", value: "tag", index: 3 },
  { label: "一级分类", value: "classfy1", index: 4 },
  { label: "二级分类", value: "classfy2", index: 5 },
  { label: "词条来源", value: "entrySource", index: 6 },
  { label: "翻译语种", value: "language", index: 7 },
  { label: "翻译状态", value: "translateState", index: 8 },
  { label: "翻译结果", value: "translate", index: 9 },
  { label: "翻译过滤", value: "filter_translate", index: 10 },
  { label: "Comment", value: "comment", index: 11 },
  { label: "辞典名称", value: "diFileName", index: 12 },
  { label: "开始时间", value: "startTime", index: 13 },
  { label: "结束时间", value: "endTime", index: 14 },
  { label: "修改人", value: "update", index: 15 },
  { label: "校验类型", value: "searchType", index: 16 },
];
const entry_readonlyFields = [
  "entry", // 词条
  "comment", // comment
  "entryLength", // 词条字符数
  "entrySource", // 词条来源
  "classfy1", // 一级分类
  "diFileName", // 辞典名称
  "update", // 修改人
  "updateTime", // 修改时间
  "classifyId", // 词条所属分类
  "productName", // 产品名
  "versionName", // 版本名
  "enCharLength", // 英文术语字符数
  "zhCharLength", // 中文术语字符数
  "ruCharLength", // 俄文术语字符数
  "spaCharLength", // 西文术语字符数
  "fraCharLength", // 法文术语字符数
  "maxChineseLength", // 中文字符上限
  "foreignMaxLength", // 外文字符上限
];
const entryvalid_readonlyFields = [
  "maxChineseLength", // 中文字符上限
  "foreignMaxLength", // 外文字符上限
];
const entry_checkedSearchCondition = entry_searchConditionList.map(
  (item) => item.value,
); // 默认选中所有查询条件
export const entryParams = {
  inputColumn: [
    "abbr",
    "entryLength",
    "partOfSpeech",
    "remark",
    "maxLength",
    "diFileName",
    "comment",
    "writeType",
  ].concat(default_languageList.map((item) => item.interpretation)),
  translateColumn: default_languageList.map((item) => item.value),
  overlayStyle: columnFilterOverlayStyle,
  exportFields: entry_exportFields,
  searchConditionList: entry_searchConditionList,
  checkedSearchCondition: entry_checkedSearchCondition,
  defaultCheckedColumn: [
    "index",
    "entry",
    "comment",
    "english",
    "russian",
    "spanish",
    "french",
  ],
  // 不可修改的字段列表（更新词条时不能选择这些字段）
  readonlyFields: entry_readonlyFields,
  // 更新字段选项（从entry_checkboxList过滤掉不可修改列）
  updateableFields: entry_exportFields.filter((item) => {
    return !entry_readonlyFields.includes(item.value);
  }),
  // 校验字段选项
  validateFields: entry_exportFields.filter((item) => {
    return !entryvalid_readonlyFields.includes(item.value);
  }),
  // 更新词条相关常量（右键菜单-更新）
  updateEntry: {
    // localStorage key 集合，方便后续扩展更多需要缓存的字段
    localStorageKey: {
      i18nUrl: "i18nUrl",
    }, // 用于缓存选中的 i18nUrl
    pollingInterval: 5000, // 轮询间隔（毫秒），开发阶段缩短为5秒
    // 任务状态消息映射
    taskStatusMessages: {
      0: "没有查到正在执行的任务信息",
      1: "任务正在执行中", // 状态1改为执行中
      2: "任务执行成功", // 状态2改为执行成功
      3: "任务执行失败",
      4: "任务终止执行",
      5: "任务终止执行失败",
      6: "系统服务存在异常，联系研发，存在未知的任务状态",
    },
  },
};

// ==================== 术语库列设置（glossary，单轨）====================

/** @type {import('@/components/ColumnFilter/colPreset.js').ColDef[]} 术语库列全集 */
export const glossaryAllCols = [
  { label: "序号", value: "index", index: 0, required: true },
  { label: "词条", value: "entry", index: 1, required: true },
  { label: "翻译状态", value: "translateState", index: 2, visible: true },
  { label: "翻译", value: "translate", index: 3, visible: true },
  { label: "翻译类型", value: "type", index: 4, visible: true },
  { label: "可见范围", value: "visualRange", index: 5, visible: true },
  { label: "词条审核员", value: "entryAuditor", index: 6, visible: true },
  { label: "操作", value: "operation", index: 100, required: true },
];

/**
 * 术语库各页列覆盖
 * @type {Record<string, import('@/components/ColumnFilter/colPreset.js').ColPreset>}
 */
export const glossaryPresets = {
  /** 术语库主页 */
  glossary: { ovrd: [], defaults: null },
};

/** 术语库非列 UI 参数 */
export const glossaryParams = {
  inputColumn: [],
  translateColumn: [],
  overlayStyle: columnFilterOverlayStyle,
};

// 4.workbenchParams如下：工作台通用 UI 参数（列设置见 wbPresets + applyTable）
export const workbenchParams = {
  overlayStyle: columnFilterOverlayStyle,
  keys: [
    "a",
    "b",
    "c",
    "d",
    "e",
    "f",
    "g",
    "h",
    "i",
    "j",
    "k",
    "l",
    "m",
    "n",
    "o",
    "p",
    "q",
    "r",
    "s",
    "t",
    "u",
    "v",
    "w",
    "x",
    "y",
    "z",
  ],
};

// ==================== 工作台列设置（wb，单轨）====================
// 列逻辑见 @/components/ColumnFilter；此处仅 SSOT 数据

/**
 * 工作台列全集（必选列与可选列均在此定义，各页通过 preset ovrd 覆盖差异）
 * @type {import('@/components/ColumnFilter/colPreset.js').ColDef[]}
 */
export const wbAllCols = [
  { label: "序号", value: "index", index: 0, required: true },
  { label: "词条", value: "entry", index: 1, required: true, width: 200 },
  { label: "存在状态", value: "isExist", index: 2 },
  { label: "翻译状态", value: "translateState", index: 3 },
  { label: "翻译", value: "translate", index: 5, required: true, width: 100 },
  { label: "释义", value: "interpretation", index: 6 },
  { label: "tag", value: "tag", index: 7, visible: true },
  { label: "comment", value: "comment", index: 8, visible: true },
  {
    label: "中文释义",
    value: "chineseInterpretation",
    index: 9,
    visible: false,
  },
  { label: "中文翻译", value: "chinese", index: 10, visible: false },
  {
    label: "英文释义",
    value: "englishInterpretation",
    index: 11,
    visible: false,
  },
  { label: "英文翻译", value: "english", index: 12, visible: false },
  {
    label: "俄文释义",
    value: "russianInterpretation",
    index: 13,
    visible: false,
  },
  { label: "俄文翻译", value: "russian", index: 14, visible: false },
  {
    label: "西文释义",
    value: "spanishInterpretation",
    index: 15,
    visible: false,
  },
  { label: "西文翻译", value: "spanish", index: 16, visible: false },
  {
    label: "法文释义",
    value: "frenchInterpretation",
    index: 17,
    visible: false,
  },
  { label: "法文翻译", value: "french", index: 18, visible: false },
  { label: "一级分类", value: "classfy1", index: 19, visible: false },
  { label: "二级分类", value: "classfy2", index: 20, visible: false },
  { label: "词条来源", value: "entrySource", index: 21, visible: false },
  { label: "回写辞典", value: "diFileName", index: 22, visible: false },
  { label: "abbr", value: "abbr", index: 23, visible: true },
  { label: "外文字符上限", value: "foreignMaxByte", index: 24, visible: false },
  { label: "来源表名", value: "srcTabName", index: 25, visible: false },
  { label: "数据库记录ID", value: "dbRID", index: 26, visible: false },
  { label: "审核意见", value: "auditSuggest", index: 99, fixed: "right" },
  { label: "词条状态", value: "entryState", index: 100, fixed: "right" },
  {
    label: "操作",
    value: "operation",
    index: 101,
    required: true,
    fixed: "right",
  },
];

/**
 * 工作台各 modal 列预设（只写与本页不同的 ovrd）
 * @type {Record<string, import('@/components/ColumnFilter/colPreset.js').ColPreset>}
 */
export const wbPresets = {
  /** 导入 */
  importModal: {
    ovrd: [
      { label: "存在状态", visible: true },
      { label: "释义", visible: true },
      { label: "词条状态", visible: true, fixed: "right" },
      { label: "翻译状态", hidden: true },
      { label: "审核意见", hidden: true },
      { label: "操作", hidden: true },
    ],
    defaults: { visible: false },
  },
  /** 词条审核 */
  examineModal: {
    ovrd: [
      { label: "存在状态", visible: true },
      { label: "翻译", width: 200 },
      { label: "词条状态", visible: true, index: 99, fixed: "right" },
      { label: "翻译状态", hidden: true },
      { label: "审核意见", hidden: true },
    ],
    defaults: null,
  },
  /** 翻译 */
  translateModal: {
    ovrd: [
      { label: "词条", width: 100 },
      { label: "审核意见", visible: true },
      { label: "存在状态", hidden: true },
      { label: "释义", hidden: true },
      { label: "词条状态", hidden: true },
      { label: "操作", hidden: true },
    ],
    defaults: null,
  },
  /** 翻译审核 */
  examineTranslateModal: {
    ovrd: [
      { label: "翻译状态", visible: true },
      { label: "翻译", width: 200 },
      { label: "审核意见", visible: true },
      { label: "存在状态", hidden: true },
      { label: "释义", hidden: true },
      { label: "词条状态", hidden: true },
    ],
    defaults: null,
  },
  /** 归档 */
  archiveModal: {
    ovrd: [
      { label: "存在状态", visible: true },
      { label: "翻译状态", visible: true },
      { label: "翻译", width: 200 },
      {
        label: "审核意见",
        visible: true,
        dataIndex: "auditSuggess",
      },
      { label: "词条状态", visible: true, fixed: "right" },
      { label: "释义", hidden: true },
      { label: "操作", hidden: true },
    ],
    defaults: null,
  },
};

// ==================== 冗余校验列设置（redundant，单轨）====================

/** @type {import('@/components/ColumnFilter/colPreset.js').ColDef[]} 冗余词条校验列全集 */
export const redundantAllCols = [
  { label: "序号", value: "index", index: 0, required: true },
  { label: "词条", value: "entry", index: 1, required: true },
  { label: "词条来源", value: "entrySource", index: 2, visible: true },
  { label: "词条状态", value: "entryState", index: 3, visible: true },
  { label: "导入类型", value: "importType", index: 4, visible: true },
  { label: "tag", value: "tag", index: 5, visible: true },
  { label: "修改人", value: "update", index: 6, visible: true },
  { label: "修改时间", value: "updateTime", index: 7, visible: true },
  { label: "写入类型", value: "writeType", index: 8, visible: true },
];

/**
 * 冗余校验各页列覆盖
 * @type {Record<string, import('@/components/ColumnFilter/colPreset.js').ColPreset>}
 */
export const redundantPresets = {
  /** 冗余词条校验 */
  redundantCheck: { ovrd: [], defaults: null },
};

/** 冗余校验非列 UI 参数 */
export const redundantTableParams = {
  inputColumn: [],
  translateColumn: [],
  overlayStyle: columnFilterOverlayStyle,
};

// 6.createBranchModal分支新建的参数如下：
const linkList = [
  ["db", "数据库-元数据"],
  ["meta", "数据库-对象数据"],
  ["enum", "枚举"],
  ["config", "配置文件"],
  ["ts", "源代码-qt(ts)"],
  ["tr", "源代码-qt(tr)"],
];
const linkOptions = linkList.map((item) => ({
  label: item[0],
  value: item[0],
}));
export const createBranchParams = {
  linkList: linkList,
  linkOptions: linkOptions,
  otherConfig: {
    通用平台部: {
      develop: [
        {
          link: "tr",
          title: "[不导入文件](工具-tr",
          files: ["tr/public"],
        },
        {
          link: "ts",
          title: "[i18n文件](工具-ts",
          files: ["i18n_tool"],
        },
        {
          link: "ts",
          title: "[不导入文件](工具-ts",
          files: [
            "apitesttool",
            "app_login",
            "app_runing_status",
            "cmd_sys_update",
            "graphic_auto_drawing",
            "graphic_base_data_check",
            "graphic_curve_default_setting_plug",
            "graphic_interface",
            "graphic_subview_dialog",
            "graphic_view_mgr",
            "gui_app_tool",
            "gui_backup_config",
            "gui_console_api",
            "gui_crontab_editor",
            "gui_curve",
            "gui_db_designer",
            "gui_db_indexviewer",
            "gui_dict_moveto_filemgr",
            "gui_expand_tool",
            "gui_force_start_tdb",
            "gui_general_advanced_configer",
            "gui_generate_license",
            "gui_integrate_calc_manager_edit",
            "gui_integrate_check_dbms",
            "gui_integrate_check_log",
            "gui_integrate_check_net",
            "gui_integrate_check_plat_log",
            "gui_integrate_check_sec",
            "gui_integrate_check_status",
            "gui_integrate_check_sys_module",
            "gui_integrate_check_sysmgr",
            "gui_integrate_file_local_clean",
            "gui_integrate_file_local_clean_drop",
            "gui_integrate_file_right_config",
            "gui_integrate_net_paramcfg",
            "gui_integrate_netmonitor",
            "gui_integrate_sdb_mgr",
            "gui_integrate_sec_selfcheck",
            "gui_integrate_syslog",
            "gui_integrate_sysrun_check",
            "gui_make_package_tool",
            "gui_net_check",
            "gui_ops_backupmgr",
            "gui_sdb_tool",
            "gui_smartSetup_tool",
            "gui_sms_tool",
            "gui_sys_check",
            "gui_sys_start_info",
            "gui_sys_update",
            "gui_system_config",
            "gui_tdb_con_query_dlg",
            "gui_text_compare",
            "gui_toolBox",
            "gui_tts_debug_tool",
            "gui_update_tool",
            "iasp_i18n",
            "iasp_update_tool_api",
            "libtool.so",
            "old_hisdb_tool",
            "saribbonbar",
            "saribbonbar_origin",
            "scui",
            "syscfg_plug",
            "window_manager_api",
            "WindowManagerAPI",
          ],
        },
        {
          link: "ts",
          title: "[测试文件](工具-ts",
          files: [
            "calc_editor_tool_test",
            "com_table_test",
            "db_performance_test",
            "db_selector_test",
            "graphic_app_func_data_imp",
            "graphic_app_func_imp",
            "graphic_app_gui_event_imp",
            "graphic_curve_demo",
            "graphic_graph_item_plugin_demo",
            "graphic_graph_menu_editor_plugin_demo",
            "graphic_graph_menu_viewer_plugin_demo",
            "gui_app_demo",
            "gui_comp_demo",
            "gui_custom_frameless_demo",
            "gui_db_SELECTOR_test",
            "gui_graphic_event_demo",
            "gui_integrate_plugin_demo",
            "gui_login_mgmt_demo",
            "gui_manager_switch_login_demo",
            "gui_progress_demo",
            "login_demo",
            "net_client_demo",
            "pertesttool",
            "tdb_publish_test_demo",
            "test_identity",
            "test_login_dialog_unit",
          ],
        },
        {
          link: "ts",
          title: "qt通用语言(工具-ts",
          files: [
            "assistant",
            "designer",
            "linguist",
            "qt",
            "qt_help",
            "qtbase",
            "qtconnectivity",
            "qtlocation",
            "qtmultimedia",
            "qtwebsockets",
          ],
        },
      ],
      mon: [
        {
          link: "tr",
          title: "[不导入文件](工具-tr",
          files: ["tr/public"],
        },
        {
          link: "ts",
          title: "[i18n文件](工具-ts",
          files: ["i18n_tool"],
        },
        {
          link: "ts",
          title: "[不导入文件](工具-ts",
          files: [
            "apitesttool",
            "app_login",
            "app_runing_status",
            "cmd_sys_update",
            "graphic_auto_drawing",
            "graphic_base_data_check",
            "graphic_curve_default_setting_plug",
            "graphic_interface",
            "graphic_subview_dialog",
            "graphic_view_mgr",
            "gui_app_tool",
            "gui_backup_config",
            "gui_console_api",
            "gui_crontab_editor",
            "gui_curve",
            "gui_db_designer",
            "gui_db_indexviewer",
            "gui_dict_moveto_filemgr",
            "gui_expand_tool",
            "gui_force_start_tdb",
            "gui_general_advanced_configer",
            "gui_generate_license",
            "gui_integrate_calc_manager_edit",
            "gui_integrate_check_dbms",
            "gui_integrate_check_log",
            "gui_integrate_check_net",
            "gui_integrate_check_plat_log",
            "gui_integrate_check_sec",
            "gui_integrate_check_status",
            "gui_integrate_check_sys_module",
            "gui_integrate_check_sysmgr",
            "gui_integrate_file_local_clean",
            "gui_integrate_file_local_clean_drop",
            "gui_integrate_file_right_config",
            "gui_integrate_net_paramcfg",
            "gui_integrate_netmonitor",
            "gui_integrate_sdb_mgr",
            "gui_integrate_sec_selfcheck",
            "gui_integrate_syslog",
            "gui_integrate_sysrun_check",
            "gui_make_package_tool",
            "gui_net_check",
            "gui_ops_backupmgr",
            "gui_sdb_tool",
            "gui_smartSetup_tool",
            "gui_sms_tool",
            "gui_sys_check",
            "gui_sys_start_info",
            "gui_sys_update",
            "gui_system_config",
            "gui_tdb_con_query_dlg",
            "gui_text_compare",
            "gui_toolBox",
            "gui_tts_debug_tool",
            "gui_update_tool",
            "iasp_i18n",
            "iasp_update_tool_api",
            "libtool.so",
            "old_hisdb_tool",
            "saribbonbar",
            "saribbonbar_origin",
            "scui",
            "syscfg_plug",
            "window_manager_api",
            "WindowManagerAPI",
          ],
        },
        {
          link: "ts",
          title: "[测试文件](工具-ts",
          files: [
            "calc_editor_tool_test",
            "com_table_test",
            "db_performance_test",
            "db_selector_test",
            "graphic_app_func_data_imp",
            "graphic_app_func_imp",
            "graphic_app_gui_event_imp",
            "graphic_curve_demo",
            "graphic_graph_item_plugin_demo",
            "graphic_graph_menu_editor_plugin_demo",
            "graphic_graph_menu_viewer_plugin_demo",
            "gui_app_demo",
            "gui_comp_demo",
            "gui_custom_frameless_demo",
            "gui_db_SELECTOR_test",
            "gui_graphic_event_demo",
            "gui_integrate_plugin_demo",
            "gui_login_mgmt_demo",
            "gui_manager_switch_login_demo",
            "gui_progress_demo",
            "login_demo",
            "net_client_demo",
            "pertesttool",
            "tdb_publish_test_demo",
            "test_identity",
            "test_login_dialog_unit",
          ],
        },
        {
          link: "ts",
          title: "qt通用语言(工具-ts",
          files: [
            "assistant",
            "designer",
            "linguist",
            "qt",
            "qt_help",
            "qtbase",
            "qtconnectivity",
            "qtlocation",
            "qtmultimedia",
            "qtwebsockets",
          ],
        },
      ],
    },
  },
};
