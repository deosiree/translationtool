package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName t_entry_public
 */
@TableName(value ="t_entry_public")
@Data
public class EntryPublicEntity implements Serializable {
    /**
     * 唯一id
     */
    @TableId(value = "id")
    private String id;

    /**
     * 词条
     */
    @TableField(value = "entry")
    private String entry;

    /**
     * 翻译
     */
    @TableField(value = "translate")
    private String translate;

    /**
     * 部门
     */
    @TableField(value = "department")
    private String department;

    /**
     * 创建人
     */
    @TableField(value = "creator")
    private String creator;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 翻译类型
     */
    @TableField(value = "translate_type")
    private String translateType;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}