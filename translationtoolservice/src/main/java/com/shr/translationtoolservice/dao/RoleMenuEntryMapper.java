package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.RoleMenuEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.RoleMenuEntry
 */
@Mapper
public interface RoleMenuEntryMapper extends BaseMapper<RoleMenuEntry> {

    List<String> getMenuIDByRoleID(String roleID);

    int insertMenuIDByRoleID(@Param("roleMenuEntry") RoleMenuEntry roleMenuEntry);

    int deleteByRoleId(String roleId);

    int deleteByRoleIds(List<String> idList);
}




