package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName RoleEntity
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/21 0021 14:09
 **/
@Data
public class RoleEntity {
    String role_id;
    String role_name;
    String describe;
    String is_default;
    String authority_id;
}
