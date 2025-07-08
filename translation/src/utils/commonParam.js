const languageList = [
  {
    name: "英文",
    value: "english",
    state: "englishTranslateState",
    chineseState: "englishChineseState",
    id: "englishId",
    publicState: "englishPublicState",
    checked: "englishChecked",
    auditSuggest: "englishAuditSuggest",
    transIdName: "engTransId",
    interpretation: "englishInterpretation",
  },
  {
    name: "俄文",
    value: "russian",
    state: "russianTranslateState",
    chineseState: "russianChineseState",
    id: "russianId",
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
    id: "spanishId",
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
    id: "frenchId",
    publicState: "frenchPublicState",
    checked: "frenchChecked",
    auditSuggest: "frenchAuditSuggest",
    transIdName: "fraTransId",
    interpretation: "frenchInterpretation",
  }
];

const languageMap = languageList.reduce((acc, lang) => {
  acc[lang.name] = lang;
  return acc;
}, {});

export default {
  languageList,
  languageMap,
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
    { label: "词条来源", value: "entrySource", index: 19 },
    { label: "中文翻译", value: "chineseTranslation", index: 30 },
    { label: "来源表名", value: "srcTabName", index: 31 },
    { label: "数据库记录ID", value: "dbRID", index: 32 },
    // {label:'审核意见',value:'auditSuggess',index:18},// 归档：后端传来的是auditSuggess，与翻译审核中有值的效果不一样；翻译审核和翻译处的值是前端根据翻译语言动态锁定的
    // {label:'词条状态',value:'entryState',index:19},
  ],
  departmentMap:{
    "通用平台部":{
      label: "通用平台部",
      importTypes:["file","ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack:true,// 是否需要回写
      value: "common",
    },
    "监控系统部":{
      label: "监控系统部",
      importTypes:["file","ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack:true,// 是否需要回写
      value: "jk",
    },
    "装置开发部":{
      label: "装置开发部",
      importTypes:["file"],// 导入类型
      needWriteBack:false,// 是否需要回写
      value: "zz",
    },
    "default":{
      label: "默认部门",
      importTypes:["file","ts", "database", "dictionary", "config", "enum"],// 导入类型
      needWriteBack:true,// 是否需要回写
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
    }
  ]
};