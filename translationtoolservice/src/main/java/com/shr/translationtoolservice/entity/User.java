package com.shr.translationtoolservice.entity;

import lombok.Data;

@Data
public class User {
    private String id;

    private String userName;

    private String jobNumber;

    private String department;

    private String roleId;

    private String roleName;
}