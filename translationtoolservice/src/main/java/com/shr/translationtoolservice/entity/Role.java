package com.shr.translationtoolservice.entity;

import lombok.Data;

@Data
public class Role {
    /**
    * 主键
    */
    private String id;

    /**
    * 角色名
    */
    private String roleName;

    /**
    * 角色描述
    */
    private String describe;

    /**
    * 是否默认角色 1是 0否
    */
    private Integer isDefault;
}