import request from "../request";

// 查询校验列表
export function searchCheckInfo(data, params) {
  return request({
    // url: "/checkManage/searchCheckInfo",
    url: "https://apifoxmock.com/m1/5916202-5603218-default/check",
    method: "POST",
    params,
    data, //参数为键值对用params  对象用data
  });
}

// 查询所有模块
export function getModuleNames() {
  return request({
    // url:"/checkManage/getModuleNames",
    url: "https://apifoxmock.com/m1/5869278-5555786-default/translate/check/code/",
    method: "POST",
    // method: "GET",
  });
}

// 查询所有问题类型
export function getQuestionTypes() {
  return request({
    url: "/checkManage/getQuestionTypes",
    url: "https://apifoxmock.com/m1/5869278-5555786-default/translate/check/code/",
    method: "POST",
  });
}
