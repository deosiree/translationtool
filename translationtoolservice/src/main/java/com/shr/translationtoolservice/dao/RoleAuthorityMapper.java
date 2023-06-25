package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.Authority;
import com.shr.translationtoolservice.entity.RoleAuthority;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleAuthorityMapper {


    Integer bindPermission(@Param("authID") String authID,
                           @Param("roleID") String roleID);

    int deleteAuthorityByID(String roidID);
}