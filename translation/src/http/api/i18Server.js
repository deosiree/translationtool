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

//获取辞典列表
export function getDictionary(params) {
    return request({
        url: "/I18Sever/getDictionary",
        method: "GET",
        params
    });
}

// 获取辞典词条
export function getDictionaryEntry(params) {
    return request({
        url: "/I18Sever/getDictionaryInfo",
        method: "GET", 
        params
    });
}

//获取数据库节点信息
export function getAllNode(params) {
    return request({
        url: "/I18Sever/getAllNode",
        method: "GET",
        params
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
export function getConfigEntry(params,data){
    return request({
        url: "/I18Sever/getConfigEntry",
        method: "POST",
        params,
        data
    });
}

// 创建辞典
export function createDic(params){
    return request({
        url: "/I18Sever/createDic",
        method: "GET",
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

// 获取枚举文件词条
export function getEnumEntry(params,data){
    return request({
        url: "/I18Sever/getEnumEntry",
        method: "POST",
        params,
        data
    });
}

// 获取枚举文件词条
export function clearDic(params){
    return request({
        url: "/I18Sever/clearDic",
        method: "GET",
        params
    });
}

// 删除辞典
export function removeDic(params){
    return request({
        url: "/I18Sever/removeDic",
        method: "GET",
        params
    });
}

// 删除辞典中的数据
export function removeDicTerms(params,data){
    return request({
        url: "/I18Sever/removeDicTerms",
        method: "POST",
        params,
        data
    });
}

// 新增辞典数据
export function addDicTerm(params,data){
    return request({
        url: "/I18Sever/addDicTerm",
        method: "POST",
        params,
        data
    });
}

// 编辑辞典数据
export function updateDicTrans(params,data){
    return request({
        url: "/I18Sever/updateDicTrans",
        method: "POST",
        params,
        data
    });
}

// 获取全部辞典
export function getInvalidDictionary(params){
    return request({
        url: "/I18Sever/getInvalidDictionary",
        method: "GET",
        params
    });
}

// 辞典生效
export function valDictionary(data){
    return request({
        url: "/I18Sever/valDictionary",
        method: "POST",
        data
    });
}

// 批量获取辞典内容
export function importDictionaryEntry(params,data){
    return request({
        url: "/I18Sever/importDictionaryEntry",
        method: "POST",
        params,
        data
    });
}

// 批量配置文件列表
export function getConfigList(params){
    return request({
        url: "/I18Sever/getConfigList",
        method: "GET",
        params
    });
}

// 批量枚举文件列表
export function getEnumList(params){
    return request({
        url: "/I18Sever/getEnumList",
        method: "GET",
        params
    });
}