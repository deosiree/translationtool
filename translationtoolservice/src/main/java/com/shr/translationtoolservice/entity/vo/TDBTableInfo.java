package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName TDBTableInfo
 * @Description 数据库表实体
 * @USER: Cola
 * @Date 2023/12/20 0020 11:48
 **/
@Data
public class TDBTableInfo {
    private String tableName = "";
    private String aliasName = "";
    private String common = "";
    private List<TDBFieldInfo> fields;
    private int tableId = 0;
}
