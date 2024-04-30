package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @title UserRole
 * @create 2024/1/31 16:54
 * @description <TODO description class purpose>
 **/
@TableName(value ="t_user_role")
@Data
public class UserRole  implements Serializable {
    private String id;

    private String userId;

    private String roleId;
}
