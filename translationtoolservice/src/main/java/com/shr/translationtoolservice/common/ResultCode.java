package com.shr.translationtoolservice.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200,"请求成功"),
    FAIL(201, "请求失败"),
    PARAM_ERROR( 202, "参数不正确"),
    SERVICE_ERROR(203, "服务异常"),
    LOGIN_AUTH(204, "未登陆"),
    LOGIN_EXPIRED(205, "登录过期，请重新登录"),
    PERMISSION(206, "暂无该权限"),
    ACCOUNT_PASSWARD_NULL(207, "用户名或密码为空！"),
    LOGIN_FAIL(208,"登录失败");


    private Integer code;
    private String message;


    private ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
