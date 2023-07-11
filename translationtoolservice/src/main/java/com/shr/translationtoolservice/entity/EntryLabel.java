package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 词条标签表
 * @TableName t_entry_label
 */
@TableName(value ="t_entry_label")
@Data
public class EntryLabel implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;

    /**
     * 标签名称
     */
    private String labelName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}