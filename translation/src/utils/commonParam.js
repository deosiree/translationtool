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

// 1.commonParam如下：
const default_languageList = [
  {
    name: "英文",// XX语种的中文
    value: "english",// XX语种
    state: "englishTranslateState",// XX翻译状态
    chineseState: "englishChineseState",// XX翻译状态的中文描述
    // id: "englishId",// XX语种id
    publicState: "englishPublicState",// XX发布状态
    checked: "englishChecked",// XX是否已选
    auditSuggest: "englishAuditSuggest",// XX审核意见
    transIdName: "engTransId",// XX语种的翻译id（工作台-翻译：保存编辑数据时使用了）
    interpretation: "englishInterpretation",// XX释义
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
    chineseState: "chineseChineseState",// （词条管理-翻译状态）中文翻译状态过于拗口
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
  langNameList: default_languageList.map(item => item.name),
  langValList: default_languageList.map(item => item.value),
  langTranslateStateList: default_languageList.map(item => item.state),
  langCNStateList: default_languageList.map(item => item.chineseState),
  langPublicStateList: default_languageList.map(item => item.publicState),
  langAudSugList: default_languageList.map(item => item.auditSuggest),
  langInterList: default_languageList.map(item => item.interpretation),
  languageMap: default_languageMap,
  checkboxList: [
    { label: '存在状态', value: 'isExist', index: 1 },
    // {label:'翻译状态',value:'translateState',index:2},
    // {label:'审核状态',value:'state',index:3},// (前端以前写的是state,我觉得写的不严谨，查了后端，后端表里没定义审核状态)
    // {label:'词条',value:'entry',index:4},
    // {label:'翻译',value:'translate',index:5},
    // {label:'释义',value:'interpretation',index:6},
    { label: 'tag', value: 'tag', index: 7 },
    { label: "comment", value: "comment", index: 8 },
    { label: '中文释义', value: 'chineseInterpretation', index: 9 },
    { label: "中文翻译", value: "chinese", index: 10 },
    { label: '英文释义', value: 'englishInterpretation', index: 11 },
    { label: "英文翻译", value: "english", index: 12 },
    { label: '俄文释义', value: 'russianInterpretation', index: 13 },
    { label: "俄文翻译", value: "russian", index: 14 },
    { label: '西文释义', value: 'spanishInterpretation', index: 15 },
    { label: "西文翻译", value: "spanish", index: 16 },
    { label: '法文释义', value: 'frenchInterpretation', index: 17 },
    { label: "法文翻译", value: "french", index: 18 },
    { label: "一级分类", value: "classfy1", index: 19 },
    { label: "二级分类", value: "classfy2", index: 20 },
    { label: "词条来源", value: "entrySource", index: 21 },
    { label: "回写辞典", value: "diFileName", index: 22 },
    { label: "abbr", value: "abbr", index: 23 },
    { label: '外文字符上限', value: 'foreignMaxByte', index: 24 },
    { label: "来源表名", value: "srcTabName", index: 25 },
    { label: "数据库记录ID", value: "dbRID", index: 26 },
    // {label:'审核意见',value:'auditSuggess',index:18},// 归档：后端传来的是auditSuggess，与翻译审核中有值的效果不一样；翻译审核和翻译处的值是前端根据翻译语种动态锁定的
    // {label:'词条状态',value:'entryState',index:19},
  ],//展示列
  departmentMap: {
    "通用平台部": {
      label: "通用平台部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      value: "common",
      classfyID: "1",
      ops: new Set(["needIP", "needExamine", "needDelete","needBranch", "dev"]),
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
      // -1.词条管理-右键-分支新建
      // -2.词条管理-右键-冗余校验
      // -3.词条管理-classifyModal:分类的编辑-批量修改归档分支；产品的编辑-修改归档分支
      // -4.工作台-平铺展示，分支列
      // -5.工作台-批量选择，
      // -5.1.工作台-批量选择，勾选后才显示多选按钮（平铺有下三角可全部选择/反选；层级无全部选择）
      // -dev正在开发中的功能，暂时只放开给平台部
    },
    "监控系统部": {
      label: "监控系统部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      value: "jk",
      classfyID: "6",
      ops: new Set(["needIP", "needExamine", "needDelete"]),
    },
    "装置开发部": {
      label: "装置开发部",
      importTypes: ["file"],// 导入类型
      xml_temp: false,// 临时修改一下xml的样式(现已置false，改回去了)
      templateType: [
        { label: 'excel新模板', value: '新模板' },
        { label: 'excel旧模板', value: '旧模板' },
        { label: 'excel通用模板', value: '通用模板' },
        { label: 'xml可视化词条', value: '可视化词条' },
        { label: 'xml装置辞典', value: '装置辞典' },
      ],
      value: "zz",
      classfyID: "2",
      ops: new Set(["needExamine", "entryState3", "needForbidden"]),
      // -entryState3默认词条审核状态为“已审核”
      // -needForbidden是否需要禁用词条（词条管理-已选词条）
    },
    "人工智能部": {
      label: "人工智能部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      value: "zn",// 瞎写的
      ops: new Set([]),
    },
    "柔性输电系统部": {
      label: "柔性输电系统部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      value: "rx",// 瞎写的
      classfyID: "101",
      ops: new Set([]),
    },
    "default": {
      label: "公共库",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      value: "default",
      classfyID: "3",
      ops: new Set([]),
    }
  },
  departmentList: [
    {
      label: "通用平台部",
      value: "common"
    },
    {
      label: "监控系统部",
      value: "jk"
    },
    {
      label: "装置开发部",
      value: "zz"
    },
    {
      label: "人工智能部",
      value: "zn"
    },
    {
      label: "柔性输电系统部",
      value: "rx"
    },
    {
      label: "公共库",
      value: "default"
    }
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
        }
      ]
    },
    {
      title: "中研院",
      children: [
        {
          title: "柔性输电系统部",
        },
      ]
    },
    {
      title: "清能",
    },
    {
      title: "公共库",
    }
  ],// 公司->部门的状态树分布
  rulesOptions: [
    { key: "toLong", label: "校验字符长度", checked: true },
    { key: "special", label: "校验特殊字符", checked: true }, // %1翻成% 1
  ],// 表单校验规则
};

// 2.tableParam如下：
const entry_checkboxList = [
  // {label:'存在状态',value:'isExist',index:1},
  // { label: "词条状态", value: "entryState", index: 1 },
  // { label: "词条", value: "entry", index: 2 },
  { label: 'tag', value: 'tag', index: 3 },
  { label: "comment", value: "comment", index: 4 },
  { label: "词条版本", value: "entryVersion", index: 5 },
  { label: "词条字符数", value: "entryLength", index: 6 },
  { label: "词条来源", value: "entrySource", index: 7 },
  { label: "中文释义", value: "chineseInterpretation", index: 8 },
  { label: "中文翻译", value: "chinese", index: 9 },
  { label: "中文翻译状态", value: "chineseTranslateState", index: 10 },
  { label: "英文释义", value: "englishInterpretation", index: 11 },
  { label: "英文翻译", value: "english", index: 12 },
  { label: "英文翻译状态", value: "englishTranslateState", index: 13 },
  { label: "俄文释义", value: "russianInterpretation", index: 14 },
  { label: "俄文翻译", value: "russian", index: 15 },
  { label: "俄文翻译状态", value: "russianTranslateState", index: 16 },
  { label: "西文释义", value: "spanishInterpretation", index: 17 },
  { label: "西文翻译", value: "spanish", index: 18 },
  { label: "西文翻译状态", value: "spanishTranslateState", index: 19 },
  { label: "法文释义", value: "frenchInterpretation", index: 20 },
  { label: "法文翻译", value: "french", index: 21 },
  { label: "法文翻译状态", value: "frenchTranslateState", index: 22 },
  { label: "一级分类", value: "classfy1", index: 23 },
  { label: "二级分类", value: "classfy2", index: 24 },
  { label: "辞典名称", value: "diFileName", index: 25 },
  { label: "备注", value: "remark", index: 28 },
  { label: "来源表名", value: "srcTabName", index: 29 },
  { label: "数据库记录ID", value: "dbRID", index: 30 },
  { label: "中文字符上限", value: "maxChineseLength", index: 31 },
  { label: "外文字符上限", value: "foreignMaxLength", index: 32 },
  // { label: "创建人", value: "creator", index: 33 },// 词条管理-导出csv后端会报错
  // { label: "创建时间", value: "createTime", index: 34 },// 词条管理-导出csv后端会报错
  { label: "修改人", value: "update", index: 35 },
  { label: "修改时间", value: "updateTime", index: 36 },
  // { label: "是否最新版本", value: "isLatestVersion", index: 37 },// 词条管理-导出csv后端会报错
  { label: "词性备注", value: "partOfSpeech", index: 41 },
  { label: "词条所属分类", value: "classifyId", index: 42 },
  // { label: "重复词条id", value: "repeatEntryId", index: 43 },// 词条管理-导出csv后端会报错
  // { label: "环境备注", value: "environmentRemark", index: 44 },// 词条管理-导出csv后端会报错
  { label: "产品名", value: "productName", index: 45 },
  { label: "版本名", value: "versionName", index: 46 },
  { label: "翻译最大长度", value: "maxLength", index: 47 },
  { label: "英文术语字符数", value: "enCharLength", index: 48 },
  { label: "中文术语字符数", value: "zhCharLength", index: 49 },
  { label: "俄文术语字符数", value: "ruCharLength", index: 50 },
  { label: "西文术语字符数", value: "spaCharLength", index: 51 },
  { label: "法文术语字符数", value: "fraCharLength", index: 52 },
  { label: "英文翻译id", value: "enTransId", index: 53 },
  { label: "俄文翻译id", value: "ruTransId", index: 54 },
  { label: "西文翻译id", value: "spaTransId", index: 55 },
  { label: "法文翻译id", value: "fraTransId", index: 56 },
  { label: "中文翻译id", value: "zhTransId", index: 57 },
  { label: "导入类型", value: "importType", index: 58 },
  { label: "回写类型", value: "writeType", index: 59 },
  { label: "词条标签", value: "entryLabel", index: 60 },
  { label: "abbr", value: "abbr", index: 99 },
];
const entry_exportFields = [
  { label: "词条", value: "entry", index: 2 },
  { label: 'tag', value: 'tag', index: 3 },
  { label: "comment", value: "comment", index: 4 },
  // { label: "词条版本", value: "entryVersion", index: 5 },
  { label: "词条字符数", value: "entryLength", index: 6 },
  { label: "词条来源", value: "entrySource", index: 7 },
  { label: "中文释义", value: "chineseInterpretation", index: 8 },
  { label: "中文翻译", value: "chinese", index: 9 },
  // { label: "中文翻译状态", value: "chineseTranslateState", index: 10 },
  { label: "英文释义", value: "englishInterpretation", index: 11 },
  { label: "英文翻译", value: "english", index: 12 },
  // { label: "英文翻译状态", value: "englishTranslateState", index: 13 },
  { label: "俄文释义", value: "russianInterpretation", index: 14 },
  { label: "俄文翻译", value: "russian", index: 15 },
  // { label: "俄文翻译状态", value: "russianTranslateState", index: 16 },
  { label: "西文释义", value: "spanishInterpretation", index: 17 },
  { label: "西文翻译", value: "spanish", index: 18 },
  // { label: "西文翻译状态", value: "spanishTranslateState", index: 19 },
  { label: "法文释义", value: "frenchInterpretation", index: 20 },
  { label: "法文翻译", value: "french", index: 21 },
  // { label: "法文翻译状态", value: "frenchTranslateState", index: 22 },
  { label: "一级分类", value: "classfy1", index: 23 },
  { label: "二级分类", value: "classfy2", index: 24 },
  { label: "辞典名称", value: "diFileName", index: 25 },
  { label: "备注", value: "remark", index: 28 },
  // { label: "来源表名", value: "srcTabName", index: 29 },
  // { label: "数据库记录ID", value: "dbRID", index: 30 },
  { label: "中文字符上限", value: "maxChineseLength", index: 31 },
  { label: "外文字符上限", value: "foreignMaxLength", index: 32 },
  // { label: "创建人", value: "creator", index: 33 },// 词条管理-导出csv后端会报错
  // { label: "创建时间", value: "createTime", index: 34 },// 词条管理-导出csv后端会报错
  { label: "修改人", value: "update", index: 35 },
  { label: "修改时间", value: "updateTime", index: 36 },
  // { label: "是否最新版本", value: "isLatestVersion", index: 37 },// 词条管理-导出csv后端会报错
  { label: "词性备注", value: "partOfSpeech", index: 41 },
  { label: "词条所属分类", value: "classifyId", index: 42 },
  // { label: "重复词条id", value: "repeatEntryId", index: 43 },// 词条管理-导出csv后端会报错
  // { label: "环境备注", value: "environmentRemark", index: 44 },// 词条管理-导出csv后端会报错
  { label: "产品名", value: "productName", index: 45 },
  { label: "版本名", value: "versionName", index: 46 },
  { label: "翻译最大长度", value: "maxLength", index: 47 },
  { label: "英文术语字符数", value: "enCharLength", index: 48 },
  { label: "中文术语字符数", value: "zhCharLength", index: 49 },
  { label: "俄文术语字符数", value: "ruCharLength", index: 50 },
  { label: "西文术语字符数", value: "spaCharLength", index: 51 },
  { label: "法文术语字符数", value: "fraCharLength", index: 52 },
  // { label: "英文翻译id", value: "enTransId", index: 53 },
  // { label: "俄文翻译id", value: "ruTransId", index: 54 },
  // { label: "西文翻译id", value: "spaTransId", index: 55 },
  // { label: "法文翻译id", value: "fraTransId", index: 56 },
  // { label: "中文翻译id", value: "zhTransId", index: 57 },
  // { label: "导入类型", value: "importType", index: 58 },
  // { label: "回写类型", value: "writeType", index: 59 },
  // { label: "词条标签", value: "entryLabel", index: 60 },
  { label: "abbr", value: "abbr", index: 99 },
];
const entry_searchConditionList = [
  { label: '词条', value: 'entry', index: 1 },
  { label: '词条状态', value: 'state', index: 2 },
  { label: 'tag', value: 'tag', index: 3 },
  { label: '一级分类', value: 'classfy1', index: 4 },
  { label: '二级分类', value: 'classfy2', index: 5 },
  { label: '词条来源', value: 'entrySource', index: 6 },
  { label: '翻译语种', value: 'language', index: 7 },
  { label: '翻译状态', value: 'translateState', index: 8 },
  { label: '翻译结果', value: 'translate', index: 9 },
  { label: '翻译过滤', value: 'filter_translate', index: 10 },
  { label: 'Comment', value: 'comment', index: 11 },
  { label: '辞典名称', value: 'diFileName', index: 12 },
  { label: '开始时间', value: 'startTime', index: 13 },
  { label: '结束时间', value: 'endTime', index: 14 },
  { label: '修改人', value: 'update', index: 15 },
];// 查询条件列表
const entry_checkedSearchCondition = entry_searchConditionList.map(item => item.value);// 默认选中所有查询条件
export const entryParams = {
  checkboxList: entry_checkboxList,
  inputColumn: ["abbr", "entryLength", "partOfSpeech", "remark", "diFileName", "comment"].concat(default_languageList.map(item => item.interpretation)),
  translateColumn: default_languageList.map(item => item.value),
  overlayStyle: {
    maxHeight: '300px',
    overflowY: 'scroll',
    backgroundColor: '#fff',
    backgroundClip: 'padding-box',
    borderRadius: '2px',
    boxShadow: '0 3px 6px -4px rgb(0 0 0 / 12%), 0 6px 16px 0 rgb(0 0 0 / 8%), 0 9px 28px 8px rgb(0 0 0 / 5%)'
  },
  exportFields: entry_exportFields,
  searchConditionList: entry_searchConditionList,
  checkedSearchCondition: entry_checkedSearchCondition,
}

// 3.glossaryParams如下：
export const glossaryParams = {
  checkboxList: [
    { label: "词条", value: "entry", index: 1 },
    { label: "翻译状态", value: "translateState", index: 2 },
    { label: "翻译", value: "translate", index: 3 },
    { label: "翻译类型", value: "type", index: 4 },
    // { label: "翻译字符数", value: "charLength", index: 5 },
    { label: "可见范围", value: "visualRange", index: 5 },
    { label: "词条审核员", value: "entryAuditor", index: 6 },
    // { label: "公开状态", value: "publicState", index: 8 },
    // { label: "最大限制长度", value: "maxLength", index: 9 },
    // { label: "审核意见", value: "auditSuggest", index: 10 },
    // { label: "备注", value: "remark", index: 11 },
  ],
  inputColumn: [],
  translateColumn: [],
  overlayStyle: {
    maxHeight: '300px',
    overflowY: 'scroll',
    backgroundColor: '#fff',
    backgroundClip: 'padding-box',
    borderRadius: '2px',
    boxShadow: '0 3px 6px -4px rgb(0 0 0 / 12%), 0 6px 16px 0 rgb(0 0 0 / 8%), 0 9px 28px 8px rgb(0 0 0 / 5%)'
  },
}

// 4.workbenchParams如下：
export const workbenchParams = {
  checkboxList: [
    { label: 'tag', value: 'tag', index: 3 },
    { label: "Comment", value: "comment", index: 4 },
    { label: "英文释义", value: "englishInterpretation", index: 5 },
    { label: "中文释义", value: "chineseInterpretation", index: 6 },
    { label: "一级分类", value: "classfy1", index: 7 },
    { label: "二级分类", value: "classfy2", index: 8 },
    { label: "词条来源", value: "entrySource", index: 9 },
    { label: "回写辞典", value: "diFileName", index: 10 },
    { label: "abbr", value: "abbr", index: 11 },
  ],
  overlayStyle: {
    maxHeight: '300px',
    overflowY: 'scroll',
    backgroundColor: '#fff',
    backgroundClip: 'padding-box',
    borderRadius: '2px',
    // padding:'0',
    boxShadow: '0 3px 6px -4px rgb(0 0 0 / 12%), 0 6px 16px 0 rgb(0 0 0 / 8%), 0 9px 28px 8px rgb(0 0 0 / 5%)'
  },
  keys: ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'],
}

// 5.redundantTableParams如下：
export const redundantTableParams = {
  checkboxList: [
    { label: "词条", value: "entry", index: 1 },
    { label: "词条来源", value: "entrySource", index: 2 },
    { label: "词条状态", value: "entryState", index: 3 },
    { label: "导入类型", value: "importType", index: 4 },
    { label: "tag", value: "tag", index: 5 },
    { label: "修改人", value: "update", index: 6 },
    { label: "修改时间", value: "updateTime", index: 7 },
    // { label: "upgrade", value: "upgrade", index: 8 },
    { label: "写入类型", value: "writeType", index: 8 },
  ],
  inputColumn: [],
  translateColumn: [],
  overlayStyle: {
    maxHeight: '300px',
    overflowY: 'scroll',
    backgroundColor: '#fff',
    backgroundClip: 'padding-box',
    borderRadius: '2px',
    boxShadow: '0 3px 6px -4px rgb(0 0 0 / 12%), 0 6px 16px 0 rgb(0 0 0 / 8%), 0 9px 28px 8px rgb(0 0 0 / 5%)'
  },
}