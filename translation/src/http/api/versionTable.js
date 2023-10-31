//引入request.js文件
import request from "../request";


// 查询历史版本库(条件查询)
export function getVersionTableByCondition(params,data) {
    return request({
        url: "/entry/getVersionTableByCondition",
        method: "POST", 
        params,
        data
    });
}

// 查询历史版本库中的词条
export function getVersionTable(params) {
    return request({
        url: "/entry/getVersionTable",
        method: "POST", 
        params
    });
}

// 创建版本库
export function createVersionTable(params,data) {
    return request({
        url: "/entry/createVersionTable",
        method: "POST", 
        params,
        data
    });
}

// 查询词条
export function getEntryToVersion(params,data) {
    return request({
        url: "/entry/getEntryToVersion",
        method: "POST", 
        params,
        data
    });
}

// 删除版本库
export function batchDeleteVersionTable(data) {
    return request({
        url: "/entry/batchDeleteVersionTable",
        method: "POST",
        data
    });
}
