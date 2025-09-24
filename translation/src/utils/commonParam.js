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
    // {label:'存在状态',value:'isExist',index:1},
    // {label:'翻译状态',value:'translateState',index:2},
    // {label:'审核状态',value:'state',index:3},// (前端以前写的是state,我觉得写的不严谨，查了后端，后端表里没定义审核状态)
    // {label:'词条',value:'entry',index:4},
    // {label:'翻译',value:'translate',index:5},
    { label: 'tag', value: 'tag', index: 6 },
    { label: "comment", value: "comment", index: 7 },
    { label: '中文释义', value: 'chineseInterpretation', index: 8 },
    { label: '英文释义', value: 'englishInterpretation', index: 9 },
    { label: '西文释义', value: 'spanishInterpretation', index: 10 },
    { label: '法文释义', value: 'frenchInterpretation', index: 11 },
    { label: '俄文释义', value: 'russianInterpretation', index: 12 },
    { label: "一级分类", value: "classfy1", index: 13 },
    { label: "二级分类", value: "classfy2", index: 14 },
    { label: "词条来源", value: "entrySource", index: 15 },
    { label: "回写辞典", value: "diFileName", index: 16 },
    { label: "abbr", value: "abbr", index: 17 },
    { label: '外文字符上限', value: 'foreignMaxByte', index: 18 },
    // { label: "中文翻译", value: "chinese", index: 30 },// 任务语种为“中文”时，放到“翻译”那一列中
    { label: "来源表名", value: "srcTabName", index: 19 },
    { label: "数据库记录ID", value: "dbRID", index: 20 },
    // {label:'审核意见',value:'auditSuggess',index:18},// 归档：后端传来的是auditSuggess，与翻译审核中有值的效果不一样；翻译审核和翻译处的值是前端根据翻译语言动态锁定的
    // {label:'词条状态',value:'entryState',index:19},
  ],
  departmentMap: {
    "通用平台部": {
      label: "通用平台部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack: true,// 是否需要回写
      needGit: true,// 是否需要git推送
      value: "common",
    },
    "监控系统部": {
      label: "监控系统部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack: true,// 是否需要回写
      needGit: true,// 是否需要git推送
      value: "jk",
    },
    "装置开发部": {
      label: "装置开发部",
      importTypes: ["file"],// 导入类型
      needWriteBack: false,// 是否需要回写
      needGit: false,// 是否需要git推送
      xml_temp: false,// 临时修改一下xml的样式(现已置false，改回去了)
      templateType: [
        { label: 'csv新模板', value: '新模板' },
        { label: 'csv旧模板', value: '旧模板' },
        { label: 'csv通用模板', value: '通用模板' },
        { label: 'xml可视化词条', value: '可视化词条' },
        { label: 'xml装置辞典', value: '装置辞典' },
      ],
      value: "zz",
    },
    "人工智能部": {
      label: "人工智能部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack: false,// 是否需要回写
      needGit: false,// 是否需要git推送
      value: "zn",// 瞎写的
    },
    "柔性输电系统部": {
      label: "柔性输电系统部",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack: false,// 是否需要回写
      needGit: false,// 是否需要git推送
      value: "rx",// 瞎写的
    },
    "default": {
      label: "默认部门",
      importTypes: ["file", "ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack: true,// 是否需要回写
      needGit: false,// 是否需要git推送
      value: "default",
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
      label: "默认部门",
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
  ]
};

// 2.tableParam如下：
const entry_checkboxList = [
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
  // { label: 'id', value: 'id' },// id不需要，id是默认会隐藏地导出出来的
  { label: '词条', value: 'entry' },
  { label: "词条状态", value: "entryState" },
].concat(entry_checkboxList
  .filter(item => item.label !== "修改时间")// 导出修改时间时，后端会出错
  .map(item => ({ label: item.label, value: item.value })));
export const entryParams = {
  checkboxList: entry_checkboxList,
  checkedColumn: ["abbr", "entry", "entryState", "entryVersion"].concat(default_languageList.map(item => item.value)),
  inputColumn: ["abbr", "entryLength", "partOfSpeech", "remark", "diFileName", "comment"].concat(default_languageList.map(item => item.interpretation)),
  translateColumn: default_languageList.map(item => item.value),
  // translateColumn: ["english", "russian", "spanish", "french"],
  overlayStyle: {
    maxHeight: '300px',
    overflowY: 'scroll',
    backgroundColor: '#fff',
    backgroundClip: 'padding-box',
    borderRadius: '2px',
    boxShadow: '0 3px 6px -4px rgb(0 0 0 / 12%), 0 6px 16px 0 rgb(0 0 0 / 8%), 0 9px 28px 8px rgb(0 0 0 / 5%)'
  },
  exportFields: entry_exportFields
  // exportFields: [
  //   // {label:'id',value:'id',disabled:true},
  //   { label: 'abbr', value: 'abbr' },
  //   { label: '词条', value: 'entry' },
  //   { label: '中文术语字符数', value: 'entryLength' },
  //   { label: '一级分类', value: 'classfy1' },
  //   { label: '二级分类', value: 'classfy2' },
  //   { label: '中文释义', value: 'chineseInterpretation' },
  //   { label: '词条来源', value: 'entrySource' },
  //   { label: '词条状态', value: 'entryState' },
  //   { label: '版本', value: 'entryVersion' },
  //   { label: '词条标签', value: 'tag' },
  //   { label: '英文释义', value: 'englishInterpretation' },
  //   { label: '英文翻译', value: 'english' },
  //   { label: '英文术语字符数', value: 'enCharLength' },
  //   { label: '英文翻译状态', value: 'englishTranslateState' },
  //   { label: '俄文释义', value: 'russianInterpretation' },
  //   { label: '俄文翻译', value: 'russian' },
  //   { label: '俄文术语字符数', value: 'ruCharLength' },
  //   { label: '俄文翻译状态', value: 'russianTranslateState' },
  //   { label: '西文释义', value: 'spanishInterpretation' },
  //   { label: '西文翻译', value: 'spanish' },
  //   { label: '西文术语字符数', value: 'spaCharLength' },
  //   { label: '西文翻译状态', value: 'spanishTranslateState' },
  //   { label: '法文释义', value: 'frenchInterpretation' },
  //   { label: '法文翻译', value: 'french' },
  //   { label: '法文术语字符数', value: 'fraCharLength' },
  //   { label: '法文翻译状态', value: 'frenchTranslateState' },
  //   { label: '产品名', value: 'productName' },
  //   { label: '版本名', value: 'versionName' },
  //   { label: "修改人", value: "update" },
  //   { label: "修改时间", value: "updateTime" },
  //   { label: "备注", value: "remark" },
  //   { label: "英文翻译id", value: "enTransld" },
  //   { label: "俄文翻译id", value: "ruTransId" },
  //   { label: "西文翻译id", value: "spaTransId" },
  //   { label: "法文翻译id", value: "fraTransId" },
  //   { label: "导入类型", value: "importType" },
  //   { label: "回写类型", value: "writeType" },
  //   { label: "DI文件名", value: "diFileName" },
  //   { label: "comment", value: "comment" },
  //   { label: 'tag', value: 'tag' },
  //   { label: "中文字符上限", value: "maxChineseLength" },
  //   { label: "外文字符上限", value: "foreignMaxLength" },
  //   { label: "来源表名", value: "srcTabName" },
  //   { label: "数据库记录ID", value: "dbRID" },
  //   { label: "abbr", value: "abbr" },
  // ],
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
  checkedColumn: [
    "entry",
    "translate",
    "type",
    "translateState",
    // "charLength",
    "visualRange",
    "entryAuditor",
    // "publicState",
    // "maxLength",
    // "auditSuggest",
    // "remark",
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
const workBench_langageMap = default_languageList.reduce((acc, cur) => {
  acc[cur.name] = {
    "language": cur.name,
    "code": cur.value,
    "transIdName": cur.transIdName,
  };
  return acc; // 添加返回累加器
}, {}); // 添加初始值为空对象
//   形如：'英文': { language: "英文", code: "english", transIdName: "enTransId" },
const workbench_languageList = Object.values(workBench_langageMap);
export const workbenchParams = {
  languageMap: workBench_langageMap,
  languageList: workbench_languageList,
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
  checkedColumn: ["abbr", "englishInterpretation", "chineseInterpretation"],
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
  checkedColumn: [
    'entry',
    'entrySource',
    'entryState',
    'importType',
    'tag',
    'update',
    'updateTime',
    'upgrade',
    'writeType'
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