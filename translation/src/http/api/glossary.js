//引入request.js文件
import request from "../request";

// 检查术语库翻译
export function checkSykEntry(data) {
  return request({
      url: "/Syk/checkSykEntry",
      method: "POST", 
      data
  });
}

// 检查预翻译
export function checkSykEntryBeforeSave(data) {
  return request({
      url: "/Syk/checkSykEntryBeforeSave",
      method: "POST", 
      data
  });
}
 
// 获取术语库
export function getSykEntry(params,data) {
    return request({
        url: "/Syk/getSykEntry",
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

export function getSykNotUsed() {
  return request({
      url: "/Syk/getSykNotUsed/",
      // url: "https://apifoxmock.com/m1/5916202-5603218-default/Syk/getSykNotUsed/",
      method: "GET", 
  });
}