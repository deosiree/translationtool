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
 * @TableName t_version
 */
@TableName(value ="t_version")
@Data
public class VersionEntity implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id")
    private String id;

    /**
     * 版本名字
     */
    @TableField(value = "name")
    private String name;

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
     * 产品id
     */
    @TableField(value = "product_id")
    private String productId;

    /**
     * 删除状态
     */
    @TableField(value = "is_delete")
    private String isDelete;

    /**
     *
     */
    @TableField(value = "details")
    private String details;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}