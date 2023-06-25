package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.ConfigResUser;
import com.shr.translationtoolservice.entity.Role;
import com.shr.translationtoolservice.entity.RoleAuthority;
import com.shr.translationtoolservice.entity.RoleEntity;

import java.util.List;

public interface ConfigManageInterface {

    List<ConfigResUser> queryUserInfo(ConfigResUser user);

    Integer deleteUserInfoByList(List<String> idList);

    Integer updateUserInfo(ConfigResUser user);

    List<RoleEntity> queryRoleInfo(String userName);

    Integer deleteRoleInfo(String id);

    Integer updateRoleInfo(Role role);

    Integer insertSelective(Role role);

    String addUser(ConfigResUser user);

    Integer bindRoleInfo(ConfigResUser configResUser);

    Integer bindPermission(RoleAuthority roleAuthority);
}
