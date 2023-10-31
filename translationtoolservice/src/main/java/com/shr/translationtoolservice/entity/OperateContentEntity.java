package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;

/**
 * @ClassName OperateContentEntity
 * @Description TODO
 * @USER: Cola
 * @Date 2023/7/4 0004 17:09
 **/
@Data
public class OperateContentEntity {
    List<ComparisonResult> results;
    String entryID;


}
