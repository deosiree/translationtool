//引入request.js文件
import request from "../request";
 
// 保存临时词条
export function insertEntry(params,data) {
    return request({
        url: "/workbench/insertEntry",
        method: "POST", 
        params,
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
export function preTranslate(params,data) {
    return request({
        url: "/workbench/preTranslate",
        method: "POST", 
        params,
        data
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

//读取装置 excel
export function readZZExcle(data) {
    return request({
        url: "/workbench/entryImportExcle",
        method: "POST", 
        data 
    });
}

// 按词条状态查询词条
export function getEntryInfoList(params,data) {
    return request({
        url: "/workbench/getEntryInfoList",
        method: "POST", 
        params,
        data
    });
}

// 修改词条
export function updateEntryList(params,data) {
    return request({
        url: "/workbench/updateEntryList",
        method: "POST", 
        params,
        data
    });
}

// 删除词条
export function deleteEntryInfoByID(data) {
    return request({
        url: "/workbench/deleteEntryInfoByID",
        method: "POST", 
        data
    });
}

// 过滤语言
export function filterSourceLanguage(params,data) {
    return request({
        url: "/workbench/filterSourceLanguage",
        method: "POST", 
        params,
        data
    });
}

// 查看辞典
export function getDictory(params) {
    return request({
        url: "/workbench/getDictory",
        method: "POST", 
        params
    });
}

// 导入
export function importCommonExcle(data) {
    return request({
        url: "/workbench/importCommonExcle",
        method: "POST",
        data:data
    });
}

// 获取i18 ip地址
export function getI18nAdress() {
    return request({
        url: "/workbench/getI18nAdress",
        method: "GET"
    });
}

// 首字母转换
export function capitalizeWords(params,data) {
    return request({
        url: "/workbench/capitalizeWords",
        method: "POST",
        params,
        data
    });
}