//引入request.js文件
import request from "../request";
 
// 查询分类树
export function getSykEntry(params,data) {
    return request({
        url: "/Syk/getSykEntry",
        method: "POST", 
        params,
        data
    });
}

export function updateSykEntry(data) {
    return request({
        url: "/Syk/updateSykEntry",
        method: "POST", 
        data
    });
}

export function getSykEntryRelation(data) {
    return request({
        url: "/Syk/getSykEntryRelation",
        method: "POST", 
        data
    });
}