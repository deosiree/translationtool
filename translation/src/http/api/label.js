import request from "../request";

// 查询标签
export function queryLabel(data,pageIndex,pageSize) {
    return request({
        url: "/entry/queryLabel?pageIndex="+pageIndex+"&pageSize="+pageSize,
        method: "POST", 
        data //参数为键值对用params  对象用data
    });
}

// 新增标签
export function addLabel(data) {
    return request({
        url: "/entry/addLabel",
        method: "POST", 
        data
    });
}

//编辑标签
export function updateLabel(data) {
    return request({
        url: "/entry/updateLabel",
        method: "POST", 
        data
    })
}

//删除标签
export function deleteLabel(data) {
    return request({
        url: "/entry/deleteLabel",
        method: "POST", 
        data
    })
}
