package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.*;

import java.util.List;

public interface ConfigManageInterface {



    Integer deleteUserInfoByList(List<String> idList);

    Integer updateUserInfo(ConfigResUser user);

    List<Role> queryRoleInfo(String roleName,
                                   Integer pageIndex,
                                   Integer pageSize);

    Integer deleteRoleInfo(List<String> id);

    Integer updateRoleInfo(Role role);

    Integer insertSelective(Role role);

    String addUser(ConfigResUser user);

    Integer bindRoleInfo(ConfigResUser configResUser);

    Integer bindPermission(RoleAuthority roleAuthority);

    List<ConfigResUser>  queryUserInfo(ConfigResUser user, Integer pageIndex, Integer pageIndex1);

    int getUserTotalNum(ConfigResUser user);

    int getRoleTotaNum(String roleName);
}
