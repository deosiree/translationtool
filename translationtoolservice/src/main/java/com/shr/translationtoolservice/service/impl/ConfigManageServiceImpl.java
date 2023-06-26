package com.shr.translationtoolservice.service.impl;


import com.shr.translationtoolservice.dao.RoleAuthorityMapper;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import com.shr.translationtoolservice.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @ClassName ConfigManageService
 * @Description
 * @USER: Cola
 * @Date 2023/6/20 0020 14:09
 **/
@Service
@Slf4j
public class ConfigManageServiceImpl implements ConfigManageInterface {

    @Autowired
    UserMapper userMapper;
    @Autowired
    RoleMapper roleMapper;
    @Autowired
    CommonUtils commonUtils;
    @Autowired
    RoleAuthorityMapper roleAuthorityMapper;
    private final static Logger logger = Logger.getLogger("ConfigManageServiceImpl");

    @Override
    public List<ConfigResUser> queryUserInfo(ConfigResUser user,
                                             Integer pageIndex,
                                             Integer pageSize) {

        List<ConfigResUser> configResUser = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            configResUser = userMapper.querUser(user, pageSize, offset);
        }


        return configResUser;
    }

    @Override
    public int getUserTotalNum(ConfigResUser user) {
        return userMapper.getUserTotalNum(user);

    }

    @Override
    public int getRoleTotaNum(String roleName) {
        return roleMapper.getRoleTotaNum(roleName);
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
    public List<Role> queryRoleInfo(String roleName,
                                    Integer pageIndex,
                                    Integer pageSize) {
        List<Role> roleEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            roleEntities = roleMapper.getRoleIDByName(roleName, pageSize, offset);
        }
        return roleEntities;

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
    //1===
    public String addUser(ConfigResUser user) {
        User newUser = new User();
        String id = commonUtils.getUUID();
        newUser.setId(id);

        if (StringUtils.isBlank(user.getRoleId())) {
            List<Role> roles = roleMapper.getRoleIDByName(user.getRoleName(), 10, 0);
            if (roles.size() == 1) {
                newUser.setRoleId(roles.get(0).getId());
            } else {
                logger.warning(" role return warning ！");
                return "";
            }

        }

        newUser.setUserName(user.getUserName());
        newUser.setDepartment(user.getDepartment());
        newUser.setJobNumber(user.getJobNumber());

        int a = userMapper.insertSelective(newUser);

        return id;
    }

    @Override
    public Integer bindRoleInfo(ConfigResUser configResUser) {
        if (StringUtils.isBlank(configResUser.getRoleId())) {
            List<Role> roles = roleMapper.getRoleIDByName(configResUser.getRoleName(), 10, 0);
            if (roles.size() == 1) {
                configResUser.setRoleId(roles.get(0).getId());
            } else {
                logger.warning(" role return waring !");
            }

        }
        int res = userMapper.updateUserInfo(configResUser);
        return res;
    }

    @Override
    public Integer bindPermission(RoleAuthority roleAuthority) {

        int result = 0;
        roleAuthorityMapper.deleteAuthorityByID(roleAuthority.getRoleID());
        for (String authId : roleAuthority.getAuthorityIDList()) {
            result += roleAuthorityMapper.bindPermission(authId, roleAuthority.getRoleID());
        }
        return result;
    }
}
