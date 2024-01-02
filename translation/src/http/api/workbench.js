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

