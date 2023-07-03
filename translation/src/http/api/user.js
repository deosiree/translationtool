import request from "../request";

// 查询用户
export function queryUser(data) {
    return request({
        url: "/configManage/queryUser",
        method: "POST", 
        data //参数为键值对用params  对象用data
    });
}

// 新增用户
export function addUser(data) {
    return request({
        url: "/configManage/addUser",
        method: "POST", 
        data //参数为键值对用params  对象用data
    });
}

//编辑用户
export function updateUserInfo(data) {
    return request({
        url: "/configManage/updateUserInfo",
        method: "POST", 
        data
    })
}

//删除用户
export function deleteUser(data) {
    return request({
        url: "/configManage/deleteUser",
        method: "POST", 
        data
    })
}
