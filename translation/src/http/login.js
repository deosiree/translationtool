//引入request.js文件
import request from "./request";
 
// 用户登录
export function login(params) {
    return request({
        url: "/userLogin/login",
        method: "POST", 
        params //参数为键值对用params  对象用data

    });
}

// 用户登录
export function testToken() {
    return request({
        url: "/userLogin/testToken",
        method: "POST"
    });
}