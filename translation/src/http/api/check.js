import request from "../request";
import { cancelRequest } from "../request";

// 查询校验列表
export function mockSearchCheckInfo(params, path, lastRequestId) { //参数为键值对用params  对象用data
  // let url = "/checkManage/searchCheckInfo";
  let url = "https://apifoxmock.com/m1/5916202-5603218-default/checkManage/searchCheckInfo/";
  if (path) {
    url += path;
    // console.log(url);
  }
  const config = {
    url,
    // url: "/checkManage/searchCheckInfo",
    method: "POST",
    params,
  };
  // console.log("3.请求配置", {params, data});
  const req = request(config);
  if (lastRequestId) {// 如果需要取消请求就会有上次请求的id属性lastRequestId，则可以取消上次的请求
    cancelRequest(lastRequestId);
  }
  return req;
}

// 展示 词条,所属类,tag,点击详情展示 对应ts文件,翻译
export function getEntryByTsVo(data) { //参数为键值对用params  对象用data
  const config = {
    url: "/checkManage/getEntryByTsVo",
    method: "POST",
    data,
  };
  return request(config);
}

// 查询校验列表
export function getTsProblems(params, data, lastRequestId) { //参数为键值对用params  对象用data
  const config = {
    url: "/checkManage/tsProblems",
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

// 查询冗余词条结果
export function getCheckNotUseEntry(params, data, lastRequestId) { //参数为键值对用params  对象用data
  const config = {
    url: "/entryInfo/getCheckNotUseEntry",
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

// 重新执行冗余词条校验
export function checkNotUseEntry(params, data, lastRequestId) { //参数为键值对用params  对象用data
  const config = {
    url: "/entryInfo/checkNotUseEntry",
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

// 删除冗余词条
export function deleteNotUseEntry(params, data, lastRequestId) { //参数为键值对用params  对象用data
  const config = {
    url: "/entryInfo/deleteNotUseEntry",
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
