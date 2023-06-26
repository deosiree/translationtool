package com.shr.translationtoolservice.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @ClassName ConfigResUser
 * @Description
 * @USER: Cola
 * @Date 2023/6/20 0020 16:44
 **/
@Data
public class ConfigResUser {
    private String id;

    private String userName;

    private String jobNumber;

    private String department;

    private String roleId;

    private String roleName;

    private String isDefault;
}
