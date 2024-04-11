package com.shr.translationtoolservice.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
*@title UserPartiality
*@create 2024/4/11 9:27
*@description <TODO description class purpose>
**/
/**
    * 用户偏好表
    */
@Data
public class UserPartiality {
    /**
    * id
    */
    private String id;

    /**
    * 用户名称
    */
    private String userName;

    /**
    * 展示列
    */
    private String displayColumn;

    /**
    * 导出列
    */
    private String exportColumn;

    /**
    * 修改时间
    */
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;
}