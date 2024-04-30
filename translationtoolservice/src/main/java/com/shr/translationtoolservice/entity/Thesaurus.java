package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 词库表
 * @TableName t_thesaurus
 */
@TableName(value ="t_thesaurus")
@Data
public class Thesaurus implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;

    /**
     * 词库代码
     */
    private String tableCode;

    /**
     * 词库名称
     */
    private String tableName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}