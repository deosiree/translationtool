package com.shr.translationtoolservice.entity;

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

    private List<String>  roleId;

    private List<String> roleName;

}