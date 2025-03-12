import request from "../request";
import { cancelRequest, cancelAllRequests } from "../request";
// 查询校验列表
export function searchCheckInfo(params, data, lastRequestId) { //参数为键值对用params  对象用data
  const config = {
    // url: "https://apifoxmock.com/m1/5916202-5603218-default/checkManage/searchCheckInfo/",
    url: "/checkManage/searchCheckInfo",
    method: "POST",
    params,
    data,
  };
  // console.log("3.请求配置", {params, data});
  const req = request(config);
  if (lastRequestId) {// 如果需要取消请求就会有上次请求的id属性lastRequestId，则可以取消上次的请求
    cancelRequest(lastRequestId);
  }
  return req;
}

// 查询所有模块
export function getModuleNames(params) {
  return request({
    url: "/checkManage/getModuleNames",
    // url: "https://apifoxmock.com/m1/5869278-5555786-default/translate/check/code/",
    // method: "POST",
    method: "GET",
    params,
  });
}

// 查询所有问题类型
export function getQuestionTypes(params) {
  return request({
    url: "/checkManage/getQuestionTypes",
    // url: "https://apifoxmock.com/m1/5869278-5555786-default/translate/check/code/",
    // method: "POST",
    method: "GET",
    params,
  });
}
