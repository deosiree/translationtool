package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.RoleAuthorityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleAuthorityMapper {


    Integer bindPermission(@Param("authID") String authID,
                           @Param("roleID") String roleID);

    RoleAuthorityEntity selectPermiss(@Param("roleID") String roleID);

    RoleAuthorityEntity checkPermiss (@Param("authID") String authID,
                                      @Param("roleID") String roleID);
    int deleteAuthorityByID(String roidID);
}