import request from "../request";

// 查询角色
export function queryRoleInfo(params) {
    return request({
        url: "/configManage/queryRoleInfo",
        method: "POST", 
        params //参数为键值对用params  对象用data
    });
}

//新增角色
export function addRoleInfo(params) {
    return request({
        url: "/configManage/addRoleInfo",
        method: "POST", 
        params //参数为键值对用params  对象用data
    });
}
//编辑角色
export function updateRoleInfo(params) {
    return request({
        url: "/configManage/updateRoleInfo",
        method: "POST", 
        params //参数为键值对用params  对象用data
    });
}
//删除角色
export function deleteRoleInfo(data) {
    return request({
        url: "/configManage/deleteRoleInfo",
        method: "POST", 
        data
    });
}

//菜单角色配置查询
export function getMenuInfoByRole(params) {
    return request({
        url: "/configManage/getMenuInfoByRole",
        method: "POST", 
        params
    });
}

//绑定权限信息
export function bindPermission(data) {
    return request({
        url: "/configManage/bindPermission",
        method: "POST", 
        data
    });
}

