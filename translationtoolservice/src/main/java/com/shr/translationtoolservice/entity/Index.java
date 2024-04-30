package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 索引表
 * @TableName t_index
 */
@TableName(value ="t_index")
@Data
public class Index implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;

    /**
     * 词条
     */
    private String entry;

    /**
     * 重复词条id
     */
    private String repeatEntryId;

    /**
     * 表1
     */
    private String table1;

    /**
     * 表2
     */
    private String table2;

    /**
     * 表3
     */
    private String table3;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}