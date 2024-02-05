//引入request.js文件
import request from "../request";
 
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
export function getEntryByVersion(data,params) {
    return request({
        url: "/entryInfo/getEntryByVersion",
        method: "POST", 
        data,
        params
    });
}

// 删除版本词条
export function deleteEntryInfo(data,params) {
    return request({
        url: "/entryInfo/deleteEntryInfo",
        method: "POST", 
        data,
        params
    });
}

// 编辑词条
export function updateEntryInfo(data,params) {
    return request({
        url: "/entryInfo/updateEntryInfo",
        method: "POST", 
        data,
        params
    });
}

// 查询公共库数据
export function getPublicEntry(data,params) {
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