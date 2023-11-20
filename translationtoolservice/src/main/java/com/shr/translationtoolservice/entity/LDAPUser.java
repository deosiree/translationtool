package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName LDAPUserEntity
 * @USER: Cola
 * @Date 2023/11/14 0014 9:56
 **/
@Data
public class LDAPUser {
    private String name;
    private String department;
    private String center;
    private String group;
    private String email;
}
