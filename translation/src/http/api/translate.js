import request from "../request";

// 查询翻译语言
export function getLanguage(data) {
    return request({
        url:"/translate/getLanguage",
        method: "POST", 
        data 
    });
}