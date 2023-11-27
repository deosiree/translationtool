package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName t_user_product
 */
@TableName(value ="t_user_product")
@Data
public class UserProductEntity implements Serializable {

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String department;
    /**
     * 唯一id
     */
    @TableId(value = "id")
    private String id;

    /**
     * 用户id
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 产品id
     */
    @TableField(value = "product_id")
    private String productId;

    /**
     * 读（0无1有）)
     */
    @TableField(value = "`read`")
    private Integer read;

    /**
     * 写（0无1有）)
     */
    @TableField(value = "`write`")
    private Integer write;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}