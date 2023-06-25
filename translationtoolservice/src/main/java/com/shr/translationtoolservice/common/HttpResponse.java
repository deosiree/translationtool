package com.shr.translationtoolservice.common;


/**
 * 后端应答的返回值
 * @author 210093
 * @param <T>
 */
public class HttpResponse<T> {
    private int code;
    private Type type;
    // 数据
    private T data;
    private String message;
    private String operationObject = "";


    public enum Type {
        OK(0), ERROR(1), UNAUTHORIZED(2), INTERNAL_ERROR(3);
        int val;
        Type(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOperationObject() {
        return operationObject;
    }

    public void setOperationObject(String operationObject) {
        this.operationObject = operationObject;
    }
}
