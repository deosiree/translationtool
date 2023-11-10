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
 * @TableName t_product
 */
@TableName(value ="t_product")
@Data
public class ProductEntity implements Serializable {
    /**
     * 主键
     */
    @TableField(value = "id")
    private String id;

    /**
     * 产品名字
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
     * 部门
     */
    @TableField(value = "department")
    private String department;

    /**
     * 删除状态
     */
    @TableField(value = "is_delete")
    private int isDelete;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}