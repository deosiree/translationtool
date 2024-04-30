package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName ResultObject
 * @Description TODO
 * @USER: Cola
 * @Date 2023/8/25 0025 11:03
 **/
@Data
public class ResultObject {


    Object data;


    String msg;

    public ResultObject(Object data, String msg) {
        this.data = data;
        this.msg = msg;
    }

    public ResultObject(String msg) {
        this.msg = msg;
    }
}
