package com.shr.translationtoolservice.service.impl;


import com.shr.translationtoolservice.dao.RoleAuthorityMapper;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import com.shr.translationtoolservice.util.CommonUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName ConfigManageService
 * @Description
 * @USER: Cola
 * @Date 2023/6/20 0020 14:09
 **/
@Service
public class ConfigManageServiceImpl implements ConfigManageInterface {

    @Autowired
    UserMapper userMapper;
    @Autowired
    RoleMapper roleMapper;
    @Autowired
    CommonUtils commonUtils;
    @Autowired
    RoleAuthorityMapper roleAuthorityMapper;
    @Override
    public List<ConfigResUser> queryUserInfo(ConfigResUser user) {
        List<ConfigResUser> configResUser = userMapper.querUser(user);



        return configResUser;
    }

    @Override
    public Integer deleteUserInfoByList(List<String> idList) {
        return userMapper.deleteUserInfoByList(idList);
    }

    @Override
    public Integer updateUserInfo(ConfigResUser user) {
        //roleName 转成roleID

        return userMapper.updateUserInfo(user);
    }

    @Override
    public List<RoleEntity> queryRoleInfo(String userName) {
        return userMapper.queryRoleInfo(userName);

    }

    @Override
    public Integer deleteRoleInfo(String id) {
        return roleMapper.deleteByPrimaryKey(id);
    }

    @Override
    public Integer updateRoleInfo(Role role) {
        return roleMapper.updateByPrimaryKeySelective(role);
    }

    @Override
    public Integer insertSelective(Role role) {
        return roleMapper.insertSelective(role);
    }

    @Override
    public String addUser(ConfigResUser user) {
        User newUser = new User();
        String id = commonUtils.getUUID();
        newUser.setId(id);

        if (StringUtils.isBlank(user.getRoleId())){
            String role_id = roleMapper.getRoleIDByName(user.getRoleName());
            newUser.setRoleId(role_id);
        }

        newUser.setUserName(user.getUserName());
        newUser.setDepartment(user.getDepartment());
        newUser.setJobNumber(user.getJobNumber());

        int a= userMapper.insertSelective(newUser);

        return id;
    }

    @Override
    public Integer bindRoleInfo(ConfigResUser configResUser) {
        if (StringUtils.isBlank(configResUser.getRoleId())){
            configResUser.setRoleId(roleMapper.getRoleIDByName(configResUser.getRoleName()));
        }
        int res = userMapper.updateUserInfo(configResUser);
        return res;
    }

    @Override
    public Integer bindPermission(RoleAuthority roleAuthority) {

        int result =0;
        roleAuthorityMapper.deleteAuthorityByID(roleAuthority.getRoleID());
        for (String authId : roleAuthority.getAuthorityIDList()){
           result  += roleAuthorityMapper.bindPermission(authId,roleAuthority.getRoleID());
        }
        return result;
    }
}
