package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.RoleAuthorityEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.RoleAuthorityEntry
 */
@Mapper
public interface RoleAuthorityEntryMapper extends BaseMapper<RoleAuthorityEntry> {
    Integer insertPermission(@Param("authID") String authID,
                           @Param("roleID") String roleID);

    List<String> selectAuthID(@Param("roleId") String roleId);

    List<RoleAuthorityEntry> checkPermiss (@Param("authId") String authId,
                                           @Param("roleId") String roleId);

    int updatePermiss (@Param("authId") String authId,
                                           @Param("roleId") String roleId);
    int deleteAuthorityByID(String roidID);
}




