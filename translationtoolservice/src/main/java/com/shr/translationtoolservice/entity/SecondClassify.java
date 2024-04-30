package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
*@title SecondClassify
*@create 2024/3/8 14:08
*@description <TODO description class purpose>
**/
/**
    * 二级分类表
    */
@Data
public class SecondClassify {
    private String id;

    /**
    * 二级分类名称
    */
    private String name;

    /**
    * 所属一级分类名称
    */
    private String parentName;

    /**
     * 所属一级分类id
     */
    private String parentId;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
}