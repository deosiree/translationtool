package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;

/**
 * @ClassName AuditReqEntry
 * @Description TODO
 * @USER: Cola
 * @Date 2023/7/3 0003 14:24
 **/
@Data
public class EntryGroupEntity {

    //审核ID
    List<String > ids;
    //表类型
    String type;
    //审核状态
    String state;

}
