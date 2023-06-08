package com.shr.translationtoolservice.common;

import lombok.Data;

/**
 * 统一返回值
 * @param <T>
 */
@Data
public class Result<T> {
    /**
     * 返回业务码用来判断成功失败
     * 200 成功
     * 201 失败
     */
    private Integer code;

    /** 描述 */
    private String massage;

    /** 描述 */
    private T date;

    public Result(Integer code, String massage, T date) {
        this.code = code;
        this.massage = massage;
        this.date = date;
    }

    public static Result ok(){
        return getCommonResponse(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }
    public static <T> Result<T> ok(T date){
        return getCommonResponse(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), date);
    }
    public static <T> Result<T> ok(String massage,T date){
        return getCommonResponse(ResultCode.SUCCESS.getCode(), massage, date);
    }

    public static Result fail(String massage){
        return getCommonResponse(ResultCode.FAIL.getCode(), massage, null);
    }
    public static Result fail(Integer code,String massage){
        return getCommonResponse(code, massage, null);
    }
    public static Result fail(){
        return getCommonResponse(ResultCode.FAIL.getCode(), ResultCode.FAIL.getMessage(), null);
    }

    public static <T> Result<T> getCommonResponse(Integer code, String massage, T date){
        return new Result(code,massage,date);
    }
}
