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