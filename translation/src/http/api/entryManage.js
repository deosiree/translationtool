//引入request.js文件
import request, { requestMultipart } from "../request";
import {
  entryImportExcle_v2 as entryImportExcle_v2_mock,
  entryValidate_v2 as entryValidate_v2_mock,
} from "./mock/entryManage";

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

// 更新翻译
export function entryImportExcle(params, data) {
  return requestMultipart({
    url: "/entryInfo/entryImportExcle",
    method: "POST",
    params,
    data,
  });
}

// 更新翻译 (v2版本 - 新API)
// 当前使用 Mock 接口，未来切换到真实 API 时，只需取消注释下面的代码并注释掉 Mock 调用即可
export function entryImportExcle_v2(params, data) {
  // TODO: 切换到真实 API 时，取消注释下面的代码，并注释掉 Mock 调用
  // return requestMultipart({
  //   url: "/entryInfo/entryImportExcle_v2",
  //   method: "POST",
  //   params,
  //   data,
  // });

  // 当前使用 Mock 接口
  return entryImportExcle_v2_mock(params, data);
}

// 校验词条 (v2版本 - 新API)
// 当前使用 Mock 接口，未来切换到真实 API 时，只需取消注释下面的代码并注释掉 Mock 调用即可
export function entryValidate_v2(params, data) {
  // TODO: 切换到真实 API 时，取消注释下面的代码，并注释掉 Mock 调用
  // return requestMultipart({
  //   url: "/entryInfo/entryValidate_v2",
  //   method: "POST",
  //   params,
  //   data,
  // });

  // 当前使用 Mock 接口
  return entryValidate_v2_mock(params, data);
}

// 读取 Excel 文件为dataSource
export function entryReadExcel(data) {
  return requestMultipart({
    url: "/entryInfo/parseFileToEntryInfos",
    method: "POST",
    data,
  });
}

// 去重导出
export function exportDeduplicatedData(params, data) {
  return request({
    url: "/entryInfo/makeGroupForEntryInfos",
    method: "POST",
    params,
    data: data
  });
}

// 翻译导入
export function workImportExcleTrans(data) {
  return requestMultipart({
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