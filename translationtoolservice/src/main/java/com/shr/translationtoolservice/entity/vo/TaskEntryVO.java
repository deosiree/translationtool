package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import lombok.Data;

import java.util.List;

/**
 * @ClassName TaskEntry
 * @Description
 * @USER: Cola
 * @Date 2025/2/19 210093 18:50
 **/
@Data
public class TaskEntryVO {
    private String taskID;
    private String taskName;
    private String productID;
    private String productName;
    private List<EntryInfoEntity> entities;
    private String translateType;
}
