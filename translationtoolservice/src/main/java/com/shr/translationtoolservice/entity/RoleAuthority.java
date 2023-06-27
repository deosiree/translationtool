package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;

@Data
public class RoleAuthority {
    private String id;
    private String roleID;
    private List<String> authorityIDList;
}
