package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.Role;
import com.shr.translationtoolservice.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    int deleteByPrimaryKey(String id);

    int insert(Role record);

    int insertSelective(Role record);

    Role selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Role record);

    int updateByPrimaryKey(Role record);

    Role getRoleByDefault();
   /* List<RoleEntity> queryRoleInfo(String userName,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);*/
    List<Role>  getRoleIDByName(@Param("roleName") String roleName,
                          @Param("limit") int limit,
                          @Param("offset") int offset);

    int getRoleTotaNum(@Param("roleName") String roleName);
}