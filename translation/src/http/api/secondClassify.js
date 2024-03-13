//引入request.js文件
import request from "../request";
 
// 查询二级分类
export function getSecondClassify(data) {
    return request({
        url: "/secondClassify/getSecondClassify",
        method: "POST", 
        data
    });
}

// 新增二级分类
export function addSecondClassify(data) {
    return request({
        url: "/secondClassify/addSecondClassify",
        method: "POST", 
        data
    });
}

// 编辑二级分类
export function updateSecondClassify(data) {
    return request({
        url: "/secondClassify/updateSecondClassify",
        method: "POST", 
        data
    });
}

// 删除二级分类
export function deleteSecondClassify(data) {
    return request({
        url: "/secondClassify/deleteSecondClassify",
        method: "POST", 
        data
    });
}