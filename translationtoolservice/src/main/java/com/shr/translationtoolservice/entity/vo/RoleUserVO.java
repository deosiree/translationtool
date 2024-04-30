package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.User;
import lombok.Data;

import java.util.List;

/**
 * @ClassName UserByDepartmentVo
 * @USER: Cola
 * @Date 2023/11/10 0010 8:56
 **/

@Data
public class RoleUserVO {
    private List<User> Users;
    private String role;
}
