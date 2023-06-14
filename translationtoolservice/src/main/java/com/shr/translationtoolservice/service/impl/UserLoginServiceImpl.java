package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.common.ResultCode;
import com.shr.translationtoolservice.dao.AuthorityMapper;
import com.shr.translationtoolservice.dao.MenuMapper;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.Authority;
import com.shr.translationtoolservice.entity.Menu;
import com.shr.translationtoolservice.entity.Role;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.service.UserLoginService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.LDAPUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
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
            List<JSONObject> userKey = ldapUtils.getUserKey(account);
            String department = "";
            if (!userKey.isEmpty()) {
                for (Object o : userKey) {
                }
                String memberOf = userKey.get(0).getString(Constant.MEMBEROF);
                department = memberOf.substring(memberOf.indexOf(Constant.EQUALE_SIGN) + 1, memberOf.indexOf(Constant.COMMA));
            }
            // 获取默认角色
            Role role = roleMapper.getRoleByDefault();
            // 封装数据  插入数据库
            User user = new User();
            user.setId(commonUtils.getUUID());
            user.setUserName(account);
            user.setDepartment(department);
            if (null != role) {
                user.setRoleId(role.getId());
            }
            userDao.insert(user);
            userInfo = user;
        }
        // 获取当前角色对应的菜单权限
        List<Menu> menus = menuMapper.selectByRoleId(userInfo.getRoleId());

        // 获取当前角色对应的权限
        List<Authority> authorities = authorityMapper.selectByRoleId(userInfo.getRoleId());
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
        }
        Map<String, String> user = new HashMap<>();
        user.put(Constant.USER_NAME, userInfo.getUserName());
        // 生成token
        String token = JWTTokenUtils.createToken(user, authorityList);
        // 封装数据返回
        Map<String, Object> rMap = new HashMap<>();
        rMap.put("user", userInfo);
        rMap.put("token", token);
        rMap.put("menu", listTree(menus));
        rMap.put("authority", menus);

        return Result.ok(rMap);
    }

    /**
     * list转tree
     *
     * @param menuList
     * @return
     */
    public List<Menu> listTree(List<Menu> menuList) {
        //新集合
        List<Menu> returnList = new ArrayList<>();

        List<String> tempList = new ArrayList<>();
        for (Menu menu : menuList) {
            tempList.add(menu.getId());
        }
        for (Menu menu : menuList) {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(menu.getParentId())) {
                recursionFn(menuList, menu);
                returnList.add(menu);
            }
        }
        //没有查询到节点则以当前节点
        if (returnList.isEmpty()) {
            returnList = menuList;
        }
        return returnList;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<Menu> list, Menu t) {
        // 得到子节点列表
        List<Menu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (Menu tChild : childList) {
            // 判断是否有子节点
            if (StringUtils.isNotBlank(tChild.getParentId()) && tChild.getParentId().equals(t.getId())) {
                for (Menu n : childList) {
                    recursionFn(list, n);
                }
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<Menu> getChildList(List<Menu> list, Menu t) {
        List<Menu> tList = new ArrayList<>();
        for (Menu n : list) {
            if (StringUtils.isNotBlank(n.getParentId()) && n.getParentId().equals(t.getId())) {
                tList.add(n);
            }
        }
        return tList;
    }
}
