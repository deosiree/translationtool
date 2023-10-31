package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.Role;
import com.shr.translationtoolservice.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    int deleteByPrimaryKey(String id);
    int deleteByList(List<String> idList);
    int insert(Role record);

    int insertSelective(Role record);

    Role selectByPrimaryKey(String id);

    int updateEntry(@Param("role") Role role);

    int updateByPrimaryKey(Role record);

    Role getRoleByDefault();
   /* List<RoleEntity> queryRoleInfo(String userName,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);*/
    List<Role> getRole( @Param("limit") int limit,
                  @Param("offset") int offset);

    List<Role> matchRoleByName(@Param("roleName") String roleName);

    Role getRoleByName(@Param("roleName") String roleName);

    int getRoleTotaNum(@Param("roleName") String roleName);

    int updateDefault0();

    String getDefault1();
}