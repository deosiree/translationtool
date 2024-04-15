import request from "../request";

// 查询
export function queryUserPartiality(params) {
    return request({
        url:"/userPartiality/queryUserPartiality",
        method: "POST"
    });
}

// 编辑
export function updateUserPartiality(data) {
    return request({
        url:"/userPartiality/updateUserPartiality",
        method: "POST",
        data
    });
}