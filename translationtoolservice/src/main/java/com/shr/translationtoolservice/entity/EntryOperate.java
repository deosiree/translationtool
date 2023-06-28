package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName EntryOperate
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/27 0027 10:27
 **/

@Data
public class EntryOperate {
    private String id;
    private String operator;
    private String operateTime;
    private String operateContent;
    private String entryId;
    private String notes;
    private String type;

}
