package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("name")
    private String name;

    private int isDefault;

    private int isSelect;

    private String notes;
}
