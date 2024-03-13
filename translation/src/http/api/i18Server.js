import request from "../request";

// 查询任务列表
export function getFileListByLang(params) {
    return request({
        url: "/I18Sever/getFileListByLang",
        method: "GET", 
        params
    });
}

// 查询任务列表
export function getTsWords(params,data) {
    return request({
        url: "/I18Sever/getWords",
        method: "POST", 
        params,
        data
    });
}

//获取字典列表
export function getDictionary() {
    return request({
        url: "/I18Sever/getDictionary",
        method: "GET"
    });
}

// 获取字典词条
export function getDictionaryEntry(params) {
    return request({
        url: "/I18Sever/getDictionaryInfo",
        method: "GET", 
        params
    });
}

//获取数据库节点信息
export function getAllNode() {
    return request({
        url: "/I18Sever/getAllNode",
        method: "GET"
    });
}

//获取数据库应用信息
export function getAppByNode(params) {
    return request({
        url: "/I18Sever/getAppByNode",
        method: "GET",
        params
    });
}

//获取库信息
export function getdbByApp(params) {
    return request({
        url: "/I18Sever/getdbByApp",
        method: "GET",
        params
    });
}

//获取数据库表信息
export function getTableByApp(params) {
    return request({
        url: "/I18Sever/getTableByApp",
        method: "GET",
        params
    });
}

// 获取表中字段信息
export function getFieldByTable(params) {
    return request({
        url: "/I18Sever/getFieldByTable",
        method: "GET",
        params
    });
}

// 获取字段内容
export function getFieldData(params,data){
    return request({
        url: "/I18Sever/getFieldData",
        method: "POST",
        params,
        data
    });
}

// 获取元数据/别名
export function getAlias(params){
    return request({
        url: "/I18Sever/getAlias",
        method: "GET",
        params
    });
}

// 回写
export function setInfo(params,data){
    return request({
        url: "/I18Sever/setInfo",
        method: "POST",
        params,
        data
    });
}

// 回写
export function setInfoByTask(params){
    return request({
        url: "/I18Sever/setInfoByTask",
        method: "POST",
        params
    });
}

// 读取配置文件中的词条
export function getConfigEntry(params){
    return request({
        url: "/I18Sever/getConfigEntry",
        method: "POST",
        params
    });
}

// 创建字典
export function createDic(params){
    return request({
        url: "/I18Sever/createDic",
        method: "POST",
        params
    });
}

// 通过节点名获取数据库词条
export function getDBALLEntryByNode(params,data){
    return request({
        url: "/I18Sever/getDBALLEntryByNode",
        method: "POST",
        params,
        data
    });
}

// 通过应用名获取数据库词条
export function getDBALLEntryByApp(params,data){
    return request({
        url: "/I18Sever/getDBALLEntryByApp",
        method: "POST",
        params,
        data
    });
}

// 通过库名获取数据库词条
export function getDBALLEntryByDB(params,data){
    return request({
        url: "/I18Sever/getDBALLEntryByDB",
        method: "POST",
        params,
        data
    });
}