package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 词性
 * @TableName t_entry_property
 */
@TableName(value ="t_entry_property")
@Data
public class EntryProperty implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;

    /**
     * 
     */
    private String propertyName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}