//引入request.js文件
import request from "../request";
 
// 保存临时词条
export function insertEntry(data) {
    return request({
        url: "/workbench/insertEntry",
        method: "POST", 
        data 
    });
}

// 更新临时词条
export function updateEntryTemp(data) {
    return request({
        url: "/workbench/updateEntryTemp",
        method: "POST", 
        data 
    });
}

// 查询临时词条
export function getEntryTempByTaskID(params) {
    return request({
        url: "/workbench/getEntryTempByTaskID",
        method: "POST", 
        params 
    });
}

// 删除临时词条
export function deleteEntryTempByID(data) {
    return request({
        url: "/workbench/deleteEntryTempByID",
        method: "POST", 
        data 
    });
}

// 预翻译
export function preTranslate(params) {
    return request({
        url: "/workbench/preTranslate",
        method: "POST", 
        params 
    });
}

// 获取导出类型
export function getImportType(params) {
    return request({
        url: "/taskManage/getImportType",
        method: "POST", 
        params 
    });
}

//excel 导入
export function importExcle(data) {
    return request({
        url: "/workbench/importExcle",
        method: "POST", 
        data 
    });
}

