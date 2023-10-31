//引入request.js文件
import request from "../request";
 
// 查询版本列表
export function queryVersionInfo(params) {
    return request({
        url: "/configManage/queryVersionInfo",
        method: "POST", 
        params 
    });
}

// 新增版本
export function addVersionInfo(params) {
    return request({
        url: "/configManage/addVersionInfo",
        method: "POST", 
        params 
    });
}

// 编辑版本
export function updateVersionInfo(params) {
    return request({
        url: "/configManage/updateVersionInfo",
        method: "POST", 
        params 
    });
}

// 删除版本
export function deleteVersionInfo(data) {
    return request({
        url: "/configManage/deleteVersionInfo",
        method: "POST", 
        data 
    });
}