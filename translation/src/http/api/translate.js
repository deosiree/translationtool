import request from "../request";

// 查询翻译语种
export function getLanguage(data) {
    return request({
        url:"/translate/getLanguage",
        method: "POST", 
        data 
    });
}

// 新增翻译
export function addTranslate(data) {
    return request({
        url:"/translate/addTranslate",
        method: "POST", 
        data 
    });
}