//引入request.js文件
import request from "../request";

// 查询分类中新增的词条来源
export function getEntrysourceListByClassfy(params) {
  return request({
    url: "/entryInfo/getEntrysourceListByClassfy",
    // url: "https://apifoxmock.com/m1/5916202-5603218-default/entryInfo/getEntrysourceListByClassfy/",
    method: "POST",
    params,
  });
}

// 更新来源中新增的词条来源
export function updateEntryByClassfy(data) {
  return request({
    url: "/entryInfo/updateEntryByClassfy",
    // url: "https://apifoxmock.com/m1/5916202-5603218-default/entryInfo/updateEntryByClassfy",
    method: "POST",
    data,
  });
}

// 查询来源中新增的词条
export function updateEntryByEntrySource(params) {
  return request({
    url: "/entryInfo/updateEntryByEntrySource",
    method: "POST",
    params,
  });
}

// 校验未使用的词条
export function checkNotUseEntry(params) {
  return request({
    url: "/entryInfo/checkNotUseEntry",
    method: "POST",
    params,
    data: {}
  });
}

// 查询分类中的更新词条
export function getUpdateEntryByClassfy(params, data) {
  return request({
    url: "/entryInfo/checkNewEntryByClassfy",
    // url: "https://apifoxmock.com/m1/5869278-5555786-default/updateEntry",
    method: "POST",
    params,
    data
  });
}

// 查询分类树
export function getClassTree(params) {
  return request({
    url: "/entryInfo/getClassTree",
    method: "POST",
    params
  });
}

// 词条分类新增
export function addEntryClassfy(data) {
  return request({
    url: "/entryInfo/addEntryClassfy",
    method: "POST",
    data
  });
}

// 词条分类删除
export function deleteEntryClassfy(data) {
  return request({
    url: "/entryInfo/deleteEntryClassfy",
    method: "POST",
    data
  });
}

// 词条分类编辑
export function updateEntryClassfy(data) {
  return request({
    url: "/entryInfo/updateEntryClassfy",
    method: "POST",
    data
  });
}

// 获取版本词条
export function getEntryByVersion(data, params) {
  return request({
    url: "/entryInfo/getEntryByVersion",
    method: "POST",
    data,
    params
  });
}

// 删除版本词条
export function deleteEntryInfo(data, params) {
  return request({
    url: "/entryInfo/deleteEntryInfo",
    method: "POST",
    data,
    params
  });
}

// 编辑词条
export function updateEntryInfo(data, params) {
  return request({
    url: "/entryInfo/updateEntryInfo",
    method: "POST",
    data,
    params
  });
}

// 禁用词条
export function forbiddenEntryInfo(data) {
  return request({
    url: "/entryInfo/forrbiddenEntry",
    method: "POST",
    data
  });
}

// 查询公共库数据
export function getPublicEntry(data, params) {
  return request({
    url: "/entryInfo/getPublicEntry",
    method: "POST",
    params,
    data
  });
}

// 修改公共库数据
export function updatePublicEntry(data) {
  return request({
    url: "/entryInfo/updatePublicEntry",
    method: "POST",
    data
  });
}

// 删除公共库数据
export function deletePublicEntry(data) {
  return request({
    url: "/entryInfo/deletePublicEntry",
    method: "POST",
    data
  });
}

// 编辑翻译
export function updateTranslation(data) {
  return request({
    url: "/entryInfo/updateTranslation",
    method: "POST",
    data
  });
}

// 辅助翻译
export function translate(params) {
  return request({
    url: "/entryInfo/translate",
    method: "POST",
    params
  });
}

// 版本导出
export function versionExport(params) {
  return request({
    url: "/entryInfo/versionExport",
    method: "POST",
    responseType: 'blob',
    params
  });
}


// 词条生成版本
export function createVersionByEntry(params, data) {
  return request({
    url: "/entryInfo/createVersionByEntry",
    method: "POST",
    params,
    data
  });
}

// 新增单个词条
export function addSingleEntry(data) {
  return request({
    url: "/entryInfo/addSingleEntry",
    method: "POST",
    data
  });
}

// 查询分类限制字符串长度
export function getClassfy(params) {
  return request({
    url: "/entryInfo/getClassfy",
    method: "POST",
    params
  });
}

// 关系表新增
export function addProductRelation(data) {
  return request({
    url: "/entryInfo/addProductRelation",
    method: "POST",
    data
  });
}

// 词条管理中  词条翻译回写
export function writeBack(params, data) {
  return request({
    url: "/entryInfo/setInfo",
    method: "POST",
    params,
    data
  });
}

export function entryImportExcle(params, data) {
  return request({
    url: "/entryInfo/entryImportExcle",
    method: "POST",
    params,
    data,
  });
}

// 读取 Excel 文件（mock 接口）
export function entryReadExcel(params, data) {
  // 正常数据，所有列都支持
  const mockData_normal = [
    { id:"1", entry: "1", english: "one", comment: "UI" },
    { id:"2", entry: "1", english: "one", comment: "UI" },
    { id:"3", entry: "2", english: "two", comment: "" },
    { id:"4", entry: "2", english: "two", comment: "" },
    { id:"5", entry: "2", english: "two1", comment: "" },
    { id:"6", entry: "2", english: "two", comment: "1" },
    { id:"7", entry: "3", english: "two", comment: "" },
    { id:"8", entry: "4", english: "4two", comment: "" },
    { id:"9", entry: "4", english: "4two", comment: "" },
    { id:"10", entry: "4", english: "4two", comment: "" },
    { id:"11", entry: "4", english: "4two", comment: "" },
    { id:"12", entry: "4", english: "4two", comment: "" },
    { id:"13", entry: "4", english: "4two", comment: "" },
    { id:"14", entry: "4", english: "4two", comment: "" },
    { id:"15", entry: "4", english: "4two", comment: "" },
    { id:"16", entry: "4", english: "4two", comment: "" },
    { id:"17", entry: "4", english: "4two", comment: "" },
    { id:"18", entry: "4", english: "4two", comment: "" },
    { id:"19", entry: "4", english: "4two", comment: "" },
    { id:"20", entry: "4", english: "4two", comment: "" },
    { id:"21", entry: "4", english: "4two", comment: "" },
    { id:"22", entry: "4", english: "4two", comment: "" },
  ];

  // 空数据
  const mockData_empty = [];
  const mockData_empty2 = null;

  // 包含不支持的列
  const mockData_unmatched = [
    { id:"1", entry: "1", english: "one", comment: "UI", unsupportedColumn1: "value1" },
    { id:"2", entry: "1", english: "one", comment: "UI", unsupportedColumn2: "value2" },
    { id:"3", entry: "2", english: "two", comment: "", anotherBadColumn: "value3" },
  ];

  // 包含比展示列更多的支持的列（会增加列，展示列被撑大，这样效果更好，因为导入后必然能看到完整的所有数据）
  const mockData_partialMatch = [
    { id:"1", entry: "1", english: "one", comment: "UI", classfy1: "UI", classfy2: "UI" },
    { id:"2", entry: "1", english: "one", comment: "UI", classfy1: "UI", classfy2: "UI" },
    { id:"3", entry: "2", english: "two", comment: "", classfy1: "UI", classfy2: "UI" },
  ];

  // 包含多个不支持的列
  const mockData_multipleUnmatched = [
    { id:"1", entry: "1", english: "one", comment: "UI", badColumn1: "val1", badColumn2: "val2", badColumn3: "val3" },
    { id:"2", entry: "1", english: "one", comment: "UI", badColumn4: "val4", badColumn5: "val5" },
  ];

  const mockDataOptions = [
    mockData_normal,
    mockData_empty,
    mockData_empty2,
    mockData_unmatched,
    mockData_partialMatch,
    mockData_multipleUnmatched
  ];
  // const currentMockData = mockDataOptions[Math.floor(Math.random() * mockDataOptions.length)];
  const currentMockData = mockData_partialMatch;

  return Promise.resolve({
    code: 200,
    message: "成功",
    type: "SUCCESS",
    data: {
      list: currentMockData,
    },
  });
}

// 去重导出（mock 接口）
export function exportDeduplicatedData(params) {
  const { data, params: deduplicateParams } = params;
  const deduplicateColumns = deduplicateParams.deduplicateColumns;

  if (!data || data.length === 0) {
    return Promise.resolve({
      code: 400,
      message: "数据为空，无法去重",
      type: "ERROR",
      code: "EMPTY_DATA"
    });
  }

  if (!deduplicateColumns || deduplicateColumns.length === 0) {
    return Promise.resolve({
      code: 400,
      message: "至少选择一列用于去重",
      type: "ERROR",
      code: "INVALID_PARAMS"
    });
  }

  const deduplicateMap = new Map();

  data.forEach((item, index) => {
    const key = deduplicateColumns.map(col => item[col]).join('|');

    if (!deduplicateMap.has(key)) {
      deduplicateMap.set(key, {
        parent: { ...item, id: index + 1 },
        children: []
      });
    } else {
      deduplicateMap.get(key).children.push({ ...item, id: index + 1 });
    }
  });

  const dataSource = [];
  const idMap = {};

  deduplicateMap.forEach((group, key) => {
    const parent = { ...group.parent };

    if (group.children.length > 0) {
      parent.children = group.children;
      idMap[parent.id] = group.children.map(child => child.id);
    }

    dataSource.push(parent);
  });

  return Promise.resolve({
    code: 200,
    message: "去重成功",
    type: "SUCCESS",
    data: {
      dataSource,
      idMap
    }
  });
}

// 翻译导入
export function workImportExcleTrans(data) {
  return request({
    url: "/entryInfo/workImportExcleTrans",
    method: "POST",
    data: data
  });
}

// 获取分类词条
export function getEntryByClassfy(params, data) {
  return request({
    url: "/entryInfo/getEntryByClassfy",
    method: "POST",
    params,
    data
  });
}

// 版本归档-拷贝分类(分支新建前的旧版本方案，弃用)
export function copyEntryClassify(params) {
  return request({
    url: "/entryInfo/copyEntryClassify",
    method: "POST",
    params
  });
}

// 分支新建-批量新增产品（基于lang文档传递对应的6个新建产品的信息-前端行为）
export function createProductByLang(data) {
  return request({
    url: "/entryInfo/createProductByLang",
    method: "POST",
    data
  });
}

// 查询指定产品的词条来源
export function getEntrySourcesByClassify(params) {
  return request({
    url: "/entryInfo/getEntrySourcesByClassify",
    method: "POST",
    params
  });
}

// 查询指定产品的辞典名称
export function getWriteFileNamesByClassify(params) {
  return request({
    url: "/entryInfo/getWriteFileNamesByClassify",
    method: "POST",
    params
  });
}


// 分支新建-查询lang文档中的文件名称（词条来源全集=已导入的+未导入的）
export function getSourceByLang(data) {
  return Promise.resolve({
    code: 200,
    message: "成功",
    data: {
      list: [
        "tr/public", "tr/public1", "tr/public2", "tr/public3", "tr/public4", "tr/public5"
      ]
    }
  });
  // return request({
  //   url: "/entryInfo/getSourceByLang",
  //   method: "POST",
  //   data
  // });
}