import request from "../request";

// 查询任务列表
export function searchTaskInfo(data, params) {
  return request({
    url: "/taskManage/searchTaskInfo",
    method: "POST",
    params,
    data, //参数为键值对用params  对象用data
  });
}

// 新增任务
export function addTaskInfos(data) {
  return request({
    url: "/taskManage/addTaskInfos",
    method: "POST",
    data
  });
}

// 删除任务
export function deleteTaskInfo(data) {
  return request({
    url: "/taskManage/deleteTaskInfo",
    method: "POST",
    data
  });
}

// 编辑任务
export function updateTaskInfo(data) {
  return request({
    url: "/taskManage/updateTaskInfo",
    method: "POST",
    data
  });
}

// 任务下发
export function taskSubmission(data) {
  return request({
    url: "/taskManage/taskSubmission",
    method: "POST",
    data
  });
}

// 任务待办查询
export function getToDoTaskInfo(params, data) {
  return request({
    url: "/taskManage/getToDoTaskInfo",
    method: "POST",
    params,
    data
  });
}

// 任务已办查询
export function getFinishTaskInfo(params, data) {
  return request({
    url: "/taskManage/getFinishTaskInfo",
    method: "POST",
    params,
    data
  });
}

// 基于任务生成新的任务
export function taskCreateNewLanguageTask(params, data) {
  return request({
    url: "/taskManage/taskCreateNewLanguageTask",
    method: "POST",
    params,
    data
  });
}

// 分支新建-基于lang文档新增任务，写入词条并修改任务状态(下发-流程中)、词条状态(有翻译-已审核、无翻译-新建)
export function createTaskByLang(params, data) {
  return request({
    url: "/taskManage/createTaskByLang",
    method: "POST",
    params,
    data
  });
}

// 获取指定任务的待处理词条数
export function getTaskPending(data) {
  return request({
    url: "/taskManage/getTaskPending",
    method: "POST",
    data
  });
}