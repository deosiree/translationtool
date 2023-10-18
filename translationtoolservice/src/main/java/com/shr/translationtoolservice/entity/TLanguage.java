package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 翻译语言表
 * @TableName t_language
 */
@TableName(value ="t_language")
@Data
public class TLanguage implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;

    /**
     * 语言代码
     */
    private String code;

    /**
     * 语言名称
     */
    private String name;

    /**
     * 百度翻译语言代码
     */
    private String bdCode;

    /**
     * 有道翻译语言代码
     */
    private String ydCode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}