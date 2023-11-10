package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@TableName(value ="t_user")
@Data
public class User implements Serializable {
    private String id;

    private String userName;

    private String jobNumber;

    private String department;

    private String roleId;

    private String roleName;
}