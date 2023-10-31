import request from "../request";

// 查询词性
export function getPropertyByName(params) {
    return request({
        url: "/configManage/getPropertyByName",
        method: "POST", 
        params //参数为键值对用params  对象用data
    });
}

// 新增词性
export function addProperty(params) {
    return request({
        url: "/configManage/addProperty",
        method: "POST", 
        params
    });
}

//编辑标签
export function updateProperty(params) {
    return request({
        url: "/configManage/updateProperty",
        method: "POST", 
        params
    })
}

//删除标签
export function deleteProperty(data) {
    return request({
        url: "/configManage/deleteProperty",
        method: "POST", 
        data
    })
}
