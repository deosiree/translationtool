import request from "../request";

// 查询产品
export function getProduct(data) {
    return request({
        url: "/product/getProduct",
        method: "POST", 
        data,
    });
}

// 新增产品
export function addProduct(data) {
    return request({
        url: "/product/addProduct",
        method: "POST", 
        data, 
    });
}

// 编辑产品
export function updateProduct(data) {
    return request({
        url: "/product/updateProduct",
        method: "POST", 
        data, 
    });
}

// 删除产品(已经弃用，统一使用deleteEntryClassfy来删除分类、产品、模块)
export function deleteProduct(data) {
    return request({
        url: "/product/deleteProduct",
        method: "POST", 
        data, 
    });
}

// 获取产品版本
export function getProductVersion(params) {
    return request({
        url: "/product/getProductVersion",
        method: "POST", 
        params, 
    });
}

// 用户产品权限查询
export function getPermissonByUserProduct(params) {
    return request({
        url: "/product/getPermissonByUserProduct",
        method: "POST", 
        params, 
    });
}

// 产品用户绑定
export function bindtPermissonByUserProduct(data,params) {
    return request({
        url: "/product/bindtPermissonByUserProduct",
        method: "POST", 
        params, 
        data
    });
}

// 查询产品用户权限
export function getUserProduct(params) {
    return request({
        url: "/product/getUserProduct",
        method: "POST", 
        params
    });
}


