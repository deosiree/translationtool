package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName EntryVersion
 * @Description
 * @USER: Cola
 * @Date 2023/6/26 0026 9:23
 **/
@Data
public class EntryVersion {
    private String id;

    private String name;

    private String isDefault;

    private String isSelect;

    private String notes;
}
