package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class Authority {

    /**
    * 主键
    */
    private String id;

    /**
    * 权限名称
    */
    private String authorityName;

    /**
    * 权限代码
    */
    private String authorityCode;

    /**
    * 权限uri
    */
    private String uri;

    /**
    * 菜单id
    */
    private String menuId;

    @TableField("'rank'")
    private int  rank;

    private boolean clecked;
}