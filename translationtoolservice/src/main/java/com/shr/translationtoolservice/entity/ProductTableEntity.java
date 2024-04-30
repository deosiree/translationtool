package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 
 * @TableName t_version_table
 */
@TableName(value ="t_product_table")
@Data
public class ProductTableEntity implements Serializable {
    /**
     * 
     */
    @TableId(value = "id")
    private String id;

    /**
     * 版本表名
     */
    @TableField(value = "entryInfo_tableName")
    private String entryInfoTableName;

    /**
     * 版本名
     */
    @TableField(value = "product_id")
    private String productId;

    /**
     * 词条版本产品关系表名
     */
    @TableField(value = "entry_relation_tableName")
    private String entryRelationTableName;



    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}