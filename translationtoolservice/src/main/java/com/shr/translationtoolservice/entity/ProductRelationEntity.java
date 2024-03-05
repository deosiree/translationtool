package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName t_product_relation
 */
@TableName(value ="t_product_relation")
@Data
public class ProductRelationEntity implements Serializable {
    /**
     * id
     */
    @TableId(value = "id")
    private String id;

    /**
     * 词条id
     */
    @TableField(value = "entry_id")
    private String entryId;

    /**
     * 版本id
     */
    @TableField(value = "version_id")
    private String versionId;

    /**
     * 产品id
     */
    @TableField(value = "product_id")
    private String productId;

    /**
     * 任务id
     */
    @TableField(value = "task_id")
    private String taskId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}