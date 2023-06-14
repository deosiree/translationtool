package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;

@Data
public class Menu {
    /**
    * 主键
    */
    private String id;

    /**
    * 父级菜单id
    */
    private String parentId;

    /**
    * 菜单名称
    */
    private String menuName;

    /**
    * 路径
    */
    private String url;

    /**
    * 菜单排序
    */
    private Integer rank;

    /**
    * 图标
    */
    private String icon;

    private String name;

    private String component;

    private List<Menu> children;

    private List<Authority> authorities;
}