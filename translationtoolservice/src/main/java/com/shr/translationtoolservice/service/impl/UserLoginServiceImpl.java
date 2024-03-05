package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.common.ResultCode;
import com.shr.translationtoolservice.dao.AuthorityMapper;
import com.shr.translationtoolservice.dao.MenuMapper;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.UserLoginService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.LDAPUtils;
import com.shr.translationtoolservice.util.TreeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class UserLoginServiceImpl implements UserLoginService {

    @Autowired
    private UserMapper userDao;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private AuthorityMapper authorityMapper;

    @Autowired
    private LDAPUtils ldapUtils;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private TreeUtils treeUtils;

    @Override
    public User getUserInfo(String jobNumber) {
        return userDao.selectByName(jobNumber);
    }

    @Override
    public Result login(String account, String password) {

        // 通过ldap鉴权
        Boolean flag = ldapUtils.LDAP_AUTH_AD(account, password);
        if (!flag) {
            return Result.fail(ResultCode.LOGIN_FAIL.getCode(), ResultCode.LOGIN_FAIL.getMessage());
        }
        // 判断用户表中是否已存在该用户  不存在则新增
        User userInfo = userDao.selectByName(account);


        if (null == userInfo) {
            // 第一次登录   获取用户所属部门
            List<JSONObject> userKey = ldapUtils.getUserKey(account, password);
            String department = "";
            if (!userKey.isEmpty()) {
                String memberOf = userKey.get(0).getString(Constant.MEMBEROF);
                department = memberOf.substring(memberOf.indexOf(Constant.EQUALE_SIGN) + 1, memberOf.indexOf(Constant.COMMA));
            }
            // 获取默认角色
            //   Role role = roleMapper.getRoleByDefault();
            // 封装数据  插入数据库
            User user = new User();
            user.setId(commonUtils.getUUID());
            user.setUserName(account);
            user.setDepartment(department);
         /*   if (null != role) {
                List<String> roleIdList = new ArrayList<>();
                roleIdList.add(role.getId());
                user.setRoleId(roleIdList);
                List<String> roleList = new ArrayList<>();
                roleList.add(role.getRoleName());
                user.setRoleName(roleList);
            }*/
            userDao.insert(user);
            userInfo = user;
        }
        //更新部门信息
        /*String department = "";
        List<JSONObject> userKey = ldapUtils.getUserKey(account);
        if (!userKey.isEmpty()){
            String memberOf = userKey.get(0).getString(Constant.MEMBEROF);
            department = memberOf.substring(memberOf.indexOf(Constant.EQUALE_SIGN) + 1, memberOf.indexOf(Constant.COMMA));
        }*/

      /*  ConfigResUser configResUser = new ConfigResUser();
        BeanUtils.copyProperties(userInfo,configResUser);
        configResUser.setDepartment(department);
        userDao.updateUserInfo(configResUser);
*/
        // 获取当前角色对应的菜单权限
        List<Role> roles = roleMapper.selectRoleName(userInfo.getId());
        List<Menu> menus = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roles)) {
            List<String> roleIds = new ArrayList<>();
            for (Role role : roles) {
                roleIds.add(role.getId());
            }
            // userInfo.setRoleName();
            menus = menuMapper.selectByRoleId(roleIds);
        }


        // 获取当前角色对应的权限
    /*    List<Authority> authorities = authorityMapper.selectByRoleId(userInfo.getRoleId());
        for (Menu menu : menus) {
            List<Authority> list = new ArrayList<>();
            for (Authority authority : authorities) {
                if (menu.getId().equals(authority.getMenuId())) {
                    list.add(authority);
                }
            }
            menu.setAuthorities(list);
        }
        // 设置token
        List<String> authorityList = new ArrayList<>();
        for (Authority authority : authorities) {
            authorityList.add(authority.getUri());
        }*/
        Map<String, String> user = new HashMap<>();
        user.put(Constant.USER_NAME, userInfo.getUserName());
        user.put(Constant.USER_DEPARTMENT, userInfo.getDepartment());
        // 生成token
        String token = JWTTokenUtils.createToken(user);
        // 封装数据返回
        Map<String, Object> rMap = new HashMap<>();
        rMap.put("user", userInfo);
        rMap.put("token", token);
        rMap.put("menu", treeUtils.listTree(menus));
        // rMap.put("authority", menus);

        return Result.ok(rMap);
    }


}
