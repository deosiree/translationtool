import request from "../request";

// 查询任务列表
export function searchTaskInfo(data,params) {
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
export function getToDoTaskInfo(params,data) {
    return request({
        url: "/taskManage/getToDoTaskInfo",
        method: "POST", 
        params,
        data
    });
}

// 任务已办查询
export function getFinishTaskInfo(params,data) {
    return request({
        url: "/taskManage/getFinishTaskInfo",
        method: "POST", 
        params,
        data
    });
}