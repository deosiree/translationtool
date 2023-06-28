package com.shr.translationtoolservice.service.impl;


import com.shr.translationtoolservice.dao.EntryVersionMapper;
import com.shr.translationtoolservice.dao.RoleAuthorityMapper;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import com.shr.translationtoolservice.util.CommonUtils;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * @ClassName ConfigManageService
 * @Description
 * @USER: Cola
 * @Date 2023/6/20 0020 14:09
 **/
@Service
@Slf4j
@Transactional
public class ConfigManageServiceImpl implements ConfigManageInterface {

    @Autowired
    UserMapper userMapper;
    @Autowired
    RoleMapper roleMapper;
    @Autowired
    EntryVersionMapper entryVersionMapper;


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
    public List<EntryVersion> queryVersionInfo(String versionName, Integer pageIndex, Integer pageSize) {
        List<EntryVersion> entryVersions = new ArrayList<>();
        if (StringUtils.isBlank(versionName)) {
            if (commonUtils.checkPage(pageIndex, pageSize)) {
                int offset = (pageIndex - 1) * pageSize;
                entryVersions = entryVersionMapper.queryVersionInfo(pageSize, offset);
            }
        } else {
            entryVersions.add(entryVersionMapper.queryVersionInfoByName(versionName));
        }

        return entryVersions;
    }

    @Override
    public int getVersionTotaNum(String versionName) {
        return entryVersionMapper.getVersionTotalNum(versionName);

    }

    @Override
    public String updateVersionInfo(EntryVersion entryVersion) {
        //重名校验
        if (findVersionSameName(entryVersion.getName())) {
            return ErrorCodeList.NAME_EXIST;
        }
        int update = entryVersionMapper.updateVersionInfo(entryVersion);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String deleteVersionInfo(List<String> idList) {
        int delete = entryVersionMapper.deleteVersionInfo(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String addVersionInfo(EntryVersion entryVersion) {
        String uuid = commonUtils.getUUID();
        entryVersion.setId(uuid);
        int insert = entryVersionMapper.addVersionInfo(entryVersion);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return uuid;
    }

    @Override
    public String deleteUserInfoByList(List<String> idList) {
        int delete = userMapper.deleteUserInfoByList(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String updateUserInfo(ConfigResUser user) {
        if (StringUtils.isBlank(user.getRoleId()) && StringUtils.isNotBlank(user.getRoleName())) {
            Role role = roleMapper.getRoleByName(user.getRoleName());
            //未找到角色信息
            if (Objects.isNull(role)) {
                return ErrorCodeList.UPDATE_ERROR;
            }
            user.setRoleId(role.getId());
        }

        int update = userMapper.updateUserInfo(user);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<Role> queryRoleInfo(String roleName,
                                    Integer pageIndex,
                                    Integer pageSize) {
        List<Role> roleEntities = new ArrayList<>();
        if (StringUtils.isBlank(roleName)) {
            if (commonUtils.checkPage(pageIndex, pageSize)) {
                int offset = (pageIndex - 1) * pageSize;
                roleEntities = roleMapper.getRole(pageSize, offset);
            }
        } else {
            roleEntities.addAll(roleMapper.matchRoleByName(roleName));
        }

        return roleEntities;

    }

    @Override
    public String deleteRoleInfo(List<String> idList) {
        int delete = roleMapper.deleteByList(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String updateRoleInfo(Role role) {
        if (Objects.nonNull(role.getIsDefault()) && 1 == role.getIsDefault()) {
             roleMapper.updateDefault0();
        }
        int update = roleMapper.updateEntry(role);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String insertRoleInfo(Role role) {

        if (findRoleSameName(role.getRoleName())) {
            return ErrorCodeList.NAME_EXIST;
        }
        if (Objects.isNull(role.getIsDefault())) {
            role.setIsDefault(0);
            //其他角色default改成0
        }else if ("1".equals(role.getIsDefault())){
            roleMapper.updateDefault0();
        }
        role.setId(commonUtils.getUUID());
        int insert = roleMapper.insertSelective(role);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return role.getId();
    }

    @Override
    //1===
    public String addUser(ConfigResUser user) {
        User newUser = new User();
        if (findUserSameName(user.getUserName())) {
            return ErrorCodeList.NAME_EXIST;
        }
        String id = commonUtils.getUUID();
        newUser.setId(id);

        //如果没有角色信息，默认设置2
        if (StringUtils.isBlank(user.getRoleId()) && StringUtils.isNotBlank(user.getRoleName())) {
            Role role = roleMapper.getRoleByName(user.getRoleName());
            if (Objects.isNull(role) || StringUtils.isBlank(role.getId())) {
                logger.warning(" **** roleID 匹配异常，已默认设置角色为用户 ！ **** ");
                String roleID = getDefaultRoleID();
                if (StringUtils.isBlank(roleID)) {
                    logger.warning(" **** 未找到用户角色ID ！ **** ");
                    return ErrorCodeList.INSERT_ERROR;
                }

                newUser.setRoleId(roleID);
            }
            newUser.setRoleId(role.getId());
        } else if (StringUtils.isBlank(user.getRoleId()) && StringUtils.isBlank(user.getRoleName())) {

            //设置默认用户角色
            String roleID = getDefaultRoleID();
            if (StringUtils.isBlank(roleID)) {
                logger.warning(" **** 未找到用户角色ID ！ **** ");
                return ErrorCodeList.INSERT_ERROR;
            }
            newUser.setRoleId(roleID);
            //
        } else if (StringUtils.isBlank(user.getRoleId()) && StringUtils.isBlank(user.getRoleName())) {
            String roleID = roleMapper.getDefault1();

            user.setRoleId(roleID);
        }

        newUser.setUserName(user.getUserName());
        newUser.setDepartment(user.getDepartment());
        newUser.setJobNumber(user.getJobNumber());
        newUser.setRoleId(user.getRoleId());
        int insert = userMapper.insertSelective(newUser);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return id;
    }

    //查找用户角色ID
    public String getDefaultRoleID() {
        logger.warning(" **** roleID 匹配异常，已默认设置角色为用户 ！ **** ");
        Role userRole = roleMapper.getRoleByName("用户");
        if (Objects.isNull(userRole)) {
            logger.warning(" **** 未找到用户角色 ！ **** ");
            return "error";
        }
        return userRole.getId();
    }

    @Override
    public String bindRoleInfo(ConfigResUser configResUser) {
        //如果只有角色名字 无ID 则 查出ID 插入对象内
        if (StringUtils.isBlank(configResUser.getRoleId()) && StringUtils.isNotBlank(configResUser.getRoleName())) {

            Role role = roleMapper.getRoleByName(configResUser.getRoleName());


            if (Objects.nonNull(role)) {
                configResUser.setRoleId(role.getId());
            } else {
                logger.info(" role return waring !");
            }

        } else if (StringUtils.isBlank(configResUser.getRoleId()) && StringUtils.isBlank(configResUser.getRoleName())) {
            return ErrorCodeList.INPUT_IS_NULL;
        }
        int update = userMapper.updateUserInfo(configResUser);

        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String bindPermission(RoleAuthority roleAuthority) {


        if (StringUtils.isBlank(roleAuthority.getRoleID())) {
            return ErrorCodeList.INPUT_IS_NULL;
        }
        int delete = roleAuthorityMapper.deleteAuthorityByID(roleAuthority.getRoleID());
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        for (String authId : roleAuthority.getAuthorityIDList()) {
            int bind = roleAuthorityMapper.bindPermission(authId, roleAuthority.getRoleID());
            if (bind != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }
        }
        return ConstantInterface.OK_STR;
    }

    //校验用户重名 重名 true
    private boolean findUserSameName(String userName) {
        User user = userMapper.selectByName(userName);
        if (Objects.nonNull(user)) {
            return true;
        }
        return false;
    }

    //校验角色重名 重名 true
    private boolean findRoleSameName(String roleName) {
        Role role = roleMapper.getRoleByName(roleName);

        if (Objects.nonNull(role)) {
            return true;
        }
        return false;
    }

    //校验版本重名 重名 true
    private boolean findVersionSameName(String versionName) {
        EntryVersion entryVersion = entryVersionMapper.queryVersionInfoByName(versionName);
        if (Objects.nonNull(entryVersion)) {
            return true;
        }
        return false;
    }
}
