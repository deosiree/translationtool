import request from "../request";

// 查询各部门不同角色用户
export function getRoleUserByDepartment(params) {
    return request({
        url:"/userManage/getRoleUserByDepartment",
        method: "POST", 
        params 
    });
}
// 查询所有部门
export function getDepartments() {
    return request({
        url:"/userManage/getDepartments",
        method: "POST", 
    });
}

// 查询用户权限
export function getUserPermission(params) {
    return request({
        url:"/userManage/getUserPermission",
        method: "POST", 
        params
    });
}

// 查询用户
export function getUserInfo(data,params) {
    return request({
        url: "/userManage/getUserInfo",
        method: "POST", 
        params,
        data
    });
}

// 新增用户权限
export function addUserPermission(data) {
    return request({
        url: "/userManage/addUserPermission",
        method: "POST", 
        data
    });
}

// 查询角色
export function getRoleAndMenu(data,params) {
    return request({
        url: "/userManage/getRoleAndMenu",
        method: "POST", 
        params,
        data
    });
}

// 编辑角色
export function changeRoleAndMenu(data,params) {
    return request({
        url: "/userManage/changeRoleAndMenu",
        method: "POST", 
        params,
        data
    });
}

// 获取菜单
export function getMenu() {
    return request({
        url: "/userManage/getMenu",
        method: "POST"
    });
}

