import request from "../request";

// 查询任务列表
export function getVersion(data) {
    return request({
        url: "/version/getVersion",
        method: "POST", 
        data, //参数为键值对用params  对象用data
    });
}

// 创建版本
export function createVersion(data){
    return request({
        url: "/version/createVersion",
        method: "POST", 
        data, 
    });
}

// 编辑版本
export function updateVersion(data){
    return request({
        url: "/version/updateVersion",
        method: "POST", 
        data, 
    });
}

// 删除版本
export function deleteVersion(data){
    return request({
        url: "/version/deleteVersion",
        method: "POST", 
        data, 
    });
}