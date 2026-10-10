//引入request.js文件
import request from "../request";

// 检查预翻译
export function checkSykEntryBeforeSave(data) {
  return request({
    url: "/Syk/checkSykEntryBeforeSave",
    method: "POST",
    data
  });
}

// 条件查询
export function getSykEntry(params, data) {
  return request({
    url: "/Syk/getSykEntry",
    method: "POST",
    params,
    data
  });
}

// 查重自检查询
export function checkSameEntry(params, data) {
  return request({
    url: "/Syk/checkSykSameEntry",
    method: "POST",
    params,
    data
  });
}

// 空挂术语查询
export function getSykNotUsed(params, data) {
  return request({
    url: "/Syk/getSykNotUsed/",
    // url: "https://apifoxmock.com/m1/5916202-5603218-default/Syk/getSykNotUsed/",
    method: "POST",
    params,
    data
  });
}

// 格式校验查询：检查术语库翻译
export function checkSykEntry(params, data) {
  return request({
    url: "/Syk/checkSykEntry",
    method: "POST",
    params,
    data
  });
}

// 删除术语库翻译
export function deleteSykEntry(data) {
  return request({
    url: "/Syk/deleteSykEntry",
    method: "POST",
    data
  });
}

// 更新术语库翻译
export function updateSykEntry(data) {
  return request({
    url: "/Syk/updateSykEntry",
    method: "POST",
    data
  });
}

// 获取术语库词条关联信息
export function getSykEntryRelation(data) {
  return request({
    url: "/Syk/getSykEntryRelation",
    method: "POST",
    data
  });
}

// 获取术语库同名词条关联信息
export function getSameEntryRelation(data) {
  return request({
    // url: "/Syk/getSykEntryRelation",
    url: "/Syk/getSameEntryRelation",
    method: "POST",
    data
  });
}

