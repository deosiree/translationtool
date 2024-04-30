package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.RoleAuthorityEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.RoleAuthorityEntry
 */
@Mapper
public interface RoleAuthorityEntryMapper extends BaseMapper<RoleAuthorityEntity> {
    Integer insertPermission(@Param("roleAuthorityEntity") RoleAuthorityEntity roleAuthorityEntity);

    List<String> selectAuthID(@Param("roleId") String roleId);

    List<RoleAuthorityEntity> selectPermiss (@Param("roleAuthorityEntity") RoleAuthorityEntity roleAuthorityEntity);

    int updatePermiss (@Param("authId") String authId,
                                           @Param("roleId") String roleId);
    int deleteAuthorityByID(String roidID);

    int deleteByRoleIds(List<String> idList);
}




