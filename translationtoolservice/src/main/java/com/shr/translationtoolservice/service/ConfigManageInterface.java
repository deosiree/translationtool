package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.*;

import java.util.List;

public interface ConfigManageInterface {



    String deleteUserInfoByList(List<String> idList);

    String updateUserInfo(ConfigResUser user);

    List<Role> queryRoleInfo(String roleName,
                                   Integer pageIndex,
                                   Integer pageSize);

    String deleteRoleInfo(List<String> id);

    String updateRoleInfo(Role role);

    String insertRoleInfo(Role role);

    String addUser(ConfigResUser user);

    String bindRoleInfo(ConfigResUser configResUser);

    String bindPermission(RoleAuthorityRes roleAuthorityres);

    List<ConfigResUser>  queryUserInfo(ConfigResUser user, Integer pageIndex, Integer pageIndex1);

    int getUserTotalNum(ConfigResUser user);

    int getRoleTotaNum(String roleName);

    List<EntryVersion> queryVersionInfo(String versionName, Integer pageIndex, Integer pageSize);

    int getVersionTotaNum(String versionName);

    String updateVersionInfo(EntryVersion entryVersion);

    String deleteVersionInfo(List<String> idList);

    String addVersionInfo(EntryVersion entryVersion);

    List<Menu> getMenuInfoByRole(String roleID);

    int getMenuTotal();

    List<EntryProperty> getPropertyByName(String propertyName, Integer pageIndex, Integer pageSize);

    int getPropertyByNameTotal(String propertyName);

    String updateProperty(EntryProperty entryProperty);

    String addProperty(EntryProperty entryProperty);

    String deleteProperty(List<String> ids);
}
