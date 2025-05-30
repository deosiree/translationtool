package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.Date;

/**
 * @ClassName LanguageEntity
 * @USER: Cola
 * @Date 2023/8/17 0017 8:46
 **/

@Data
public class LanguageEntity {
    //语言类别
    private String language;
    //翻译结果
    private String value;
    private boolean state;
    private Date createTime;
    private String id;
    private String source;
}
