package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName TDBFieldInfo
 * @Description 数据库字段实体
 * @USER: Cola
 * @Date 2023/12/20 0020 11:47
 **/
@Data
public class TDBFieldInfo {
    private String fieldName = "";
    private String aliasName = "";
    private String common = "";
    private String db_name = "";
    private List<String> fieldDatas ;
    private long fieldID ;
    private long size;
}
