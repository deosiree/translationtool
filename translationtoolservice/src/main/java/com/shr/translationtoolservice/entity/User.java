package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@TableName(value ="t_user")
@Data
public class User implements Serializable {
    private String id;

    private String userName;

    private String jobNumber;

    private String department;

    @TableField(exist = false)
    private List<String>  roleId;

    @TableField(exist = false)
    private List<String> roleName;

}