//引入request.js文件
import request, { requestMultipart } from "../request";
 
// 查询词条
export function searchEntry(data,params) {
    return request({
        url: "/entry/searchEntry",
        method: "POST", 
        params,
        data //参数为键值对用params  对象用data
    });
}

// 批量审核
export function bathAudit(data,params) {
    return request({
        url: "/entry/bathAudit",
        method: "POST", 
        params,
        data 
    });
}

// 查询词库
export function getThesaurus() {
    return request({
        url: "/entry/getThesaurus",
        method: "POST"
    });
}

// 查询词条分类
export function getEntryClassfy(params) {
    return request({
        url: "/entry/getEntryClassfy",
        method: "POST",
        params
    });
}

// 新增词条分类
export function addEntryClassfy(params) {
    return request({
        url: "/entry/addEntryClassfy",
        method: "POST",
        params
    });
}

// 编辑词条分类
export function updateEntryClassfy(params) {
    return request({
        url: "/entry/updateEntryClassfy",
        method: "POST",
        params
    });
}

// 删除词条分类
export function deleteEntryClassfy(data) {
    return request({
        url: "/entry/deleteEntryClassfy",
        method: "POST",
        data
    });
}

// 删除词条
export function deleteEntry(data) {
    return request({
        url: "/entry/deleteEntry",
        method: "POST",
        data
    });
}

// 新增词条
export function insertEntry(data) {
    return request({
        url: "/entry/insertEntry",
        method: "POST",
        data
    });
}

// 批量新增词条
export function bachAddEntry(data) {
    return request({
        url: "/entry/bachAddEntry",
        method: "POST",
        data
    });
}

// 编辑词条
export function updateEntry(params,data) {
    return request({
        url: "/entry/updateEntry",
        method: "POST",
        params,
        data
    });
}

// 查询词条操作记录
export function getOperateByEntryId(data) {
    return request({
        url: "/entry/getOperateByEntryId",
        method: "POST",
        data
    });
}

// 词条翻译
export function translate(params) {
    return request({
        url: "/entry/translate",
        method: "POST",
        params
    });
}

// 查询未合并词条
export function getEntryNoMerge(params) {
    return request({
        url: "/entry/getEntryNoMerge",
        method: "POST",
        params
    });
}

// 查询已合并词条
export function getEntryMerge(params) {
    return request({
        url: "/entry/getEntryMerge",
        method: "POST",
        params
    });
}

// 词条合并
export function entryMerge(data) {
    return request({
        url: "/entry/entryMerge",
        method: "POST",
        data
    });
}

// 词条拆分
export function mergerSplit(data) {
    return request({
        url: "/entry/mergerSplit",
        method: "POST",
        data
    });
}

// 词性查询
export function getEntryProperty(data) {
    return request({
        url: "/entry/getEntryProperty",
        method: "POST",
        data
    });
}

// 导出
export function exportEntry(data) {
    return requestMultipart({
        url: "/test/exportEntry",
        method: "POST",
        responseType: 'blob',
        data:data
    });
}

// 导入
export function importExcle(data) {
    return requestMultipart({
        url: "/entry/importCommonExcle",
        method: "POST",
        data:data
    });
}

// 获取语种表
export function getLanguage() {
    return request({
        url: "/entry/getLanguage",
        method: "POST",
    });
}

// 获取语种表
export function getTranslatedEntry(params) {
    return request({
        url: "/entry/getTranslatedEntry",
        method: "POST",
        params
    });
}

// 词条升级
export function upgradeEntry(data) {
    return request({
        url: "/entry/upgradeEntry",
        method: "POST",
        data
    });
}

// 查询同一词条的所有版本号
export function getKindEntryVersion(params) {
    return request({
        url: "/entry/getKindEntryVersion",
        method: "POST",
        params
    });
}
