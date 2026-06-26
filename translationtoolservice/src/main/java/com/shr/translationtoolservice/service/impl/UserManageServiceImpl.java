package com.shr.translationtoolservice.service.impl;

import ch.qos.logback.classic.pattern.LineOfCallerConverter;
import cn.hutool.core.lang.tree.TreeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.MenuMapper;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.RoleUserVO;
import com.shr.translationtoolservice.entity.vo.UserDetailsVo;
import com.shr.translationtoolservice.service.UserManageService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.LDAPUtils;
import com.shr.translationtoolservice.util.TreeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.record.DVALRecord;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName UserManageServiceImpl
 * @USER: Cola
 * @Date 2023/11/10 0010 8:52
 **/
@Service
@Slf4j
public class UserManageServiceImpl implements UserManageService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private LDAPUtils ldapUtils;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TreeUtils treeUtils;

    @Override
    public int getRoleUserByDepartmentTotal() {
        return 0;
    }

    @Override
    public HashMap<String, List<User>> getRoleUserByDepartment(String department) {
        List<User> users = userMapper.getRoleUserByDepartment(department);
        HashMap<String, List<User>> userMap = new HashMap();
        //角色分类
        for (User user : users) {
            String role = "";
            List<Role> roles = roleMapper.selectRoleName(user.getId());

            for (Role role1 : roles) {
                role = ConstantInterface.constructUserAndRole().get(role1.getRoleName());
                List<User> userList = userMap.get(role);
                if (CollectionUtils.isEmpty(userList)) {
                    userList = new ArrayList<>();
                }
                userList.add(user);
                userMap.put(role, userList);
            }


        }
        return userMap;
    }

    @Override
    public int getRoleTotal() {

        return roleMapper.getRoleTotal();
    }

    @Override
    public List<String> getDepartments() {
        List<String> departments = ldapUtils.getDepartments();

        return departments;
    }

    @Override
    public List<UserDetailsVo> getUserPermission(String filter) {
        return buildUserPermissionFromDb(filter);
    }

    private List<UserDetailsVo> buildUserPermissionFromDb(String filter) {
        Map<String, User> userById = aggregateUsersWithRoles();
        List<User> allDbUsers = userMapper.selectList(new QueryWrapper<>());
        for (User dbUser : allDbUsers) {
            userById.putIfAbsent(dbUser.getId(), dbUser);
        }

        String keyword = StringUtils.isNotBlank(filter) ? filter.trim() : "";
        Map<String, List<User>> usersByDepartment = new LinkedHashMap<>();
        for (User user : userById.values()) {
            String department = StringUtils.isBlank(user.getDepartment()) ? "" : user.getDepartment();
            if (StringUtils.isNotBlank(keyword)
                    && !department.contains(keyword)
                    && !user.getUserName().contains(keyword)) {
                continue;
            }
            usersByDepartment.computeIfAbsent(department, key -> new ArrayList<>()).add(user);
        }

        List<UserDetailsVo> result = new ArrayList<>();
        for (Map.Entry<String, List<User>> entry : usersByDepartment.entrySet()) {
            List<UserDetailsVo> children = new ArrayList<>();
            for (User user : entry.getValue()) {
                UserDetailsVo userVo = new UserDetailsVo();
                userVo.setType(ConstantInterface.USER);
                userVo.setName(user.getUserName());
                userVo.setDepartment(entry.getKey());
                fillRoleCheckboxes(userVo, user.getRoleName());
                children.add(userVo);
            }
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            UserDetailsVo departmentVo = new UserDetailsVo();
            departmentVo.setName(entry.getKey());
            departmentVo.setType(ConstantInterface.DEPARTMENT);
            departmentVo.setDepartment(entry.getKey());
            departmentVo.setChildren(children);
            result.add(departmentVo);
        }
        return result;
    }

    private Map<String, User> aggregateUsersWithRoles() {
        List<User> usersWithRoles = getUserInfo(new User(), -1, -1);
        Map<String, User> userById = new LinkedHashMap<>();
        for (User user : usersWithRoles) {
            userById.merge(user.getId(), user, (existing, incoming) -> {
                mergeRoleLists(existing, incoming);
                return existing;
            });
        }
        return userById;
    }

    private void mergeRoleLists(User target, User source) {
        if (CollectionUtils.isEmpty(source.getRoleId())) {
            return;
        }
        if (target.getRoleId() == null) {
            target.setRoleId(new ArrayList<>());
        }
        if (target.getRoleName() == null) {
            target.setRoleName(new ArrayList<>());
        }
        for (int i = 0; i < source.getRoleId().size(); i++) {
            String roleId = source.getRoleId().get(i);
            if (!target.getRoleId().contains(roleId)) {
                target.getRoleId().add(roleId);
                if (source.getRoleName() != null && source.getRoleName().size() > i) {
                    target.getRoleName().add(source.getRoleName().get(i));
                }
            }
        }
    }

    private void fillRoleCheckboxes(UserDetailsVo userDetailsVo, List<String> roleNames) {
        List<String> roles = roleNames != null ? roleNames : Collections.emptyList();
        userDetailsVo.setAdmin(roles.contains(ConstantInterface.ADMIN));
        userDetailsVo.setDeveloper(roles.contains(ConstantInterface.DEVELOPER));
        userDetailsVo.setTranslator(roles.contains(ConstantInterface.TRANSLATOR));
        userDetailsVo.setEntryReviewer(roles.contains(ConstantInterface.ENTRY_AUDITOR));
        userDetailsVo.setTranslateReviewer(roles.contains(ConstantInterface.TRANSLATE_AUDITOR));
        userDetailsVo.setDevelopAdmin(roles.contains(ConstantInterface.DEVELOP_ADMIN));
        userDetailsVo.setSuperAdmin(roles.contains(ConstantInterface.SUPER_ADMIN));
    }

    @Override
    public List<User> getUserInfo(User user, Integer offset, Integer pageSize) {
        List<User> users = userMapper.getUserInfo(user, offset, pageSize);
        return users;
    }

    @Override
    public int getUserInfoTotal(User user) {
        return userMapper.getUserInfoTotal(user);
    }

    /*@Override
    public String addUserPermission(List<UserDetailsVo>  users) {
        List<User> userList = new ArrayList<>();
        for (UserDetailsVo userDetailsVo : users){
           User user = new User();
            user.setUserName(userDetailsVo.getName());

            user.setId(commonUtils.getUUID());
            user.setDepartment(userDetailsVo.getDepartment());
            List<String> roleNameList = new ArrayList<>();
            List<String> roleIdList = new ArrayList<>();
            if (userDetailsVo.getAdmin()){
                Role role = roleMapper.getRoleByName(ConstantInterface.ADMIN);
                roleIdList.add(role.getId());
                roleNameList.add(ConstantInterface.ADMIN);
            }
            if (userDetailsVo.getDeveloper()){
                Role role = roleMapper.getRoleByName(ConstantInterface.DEVELOPER);
                roleIdList.add(role.getId());
                roleNameList.add(ConstantInterface.DEVELOPER);
            }
            if (userDetailsVo.getEntryReviewer()){
                Role role = roleMapper.getRoleByName(ConstantInterface.ENTRY_AUDITOR);
                roleIdList.add(role.getId());
                roleNameList.add(ConstantInterface.ENTRY_AUDITOR);
            }
            if (userDetailsVo.getTranslator()){
                Role role = roleMapper.getRoleByName(ConstantInterface.TRANSLATOR);
                roleIdList.add(role.getId());
                roleNameList.add(ConstantInterface.TRANSLATOR);
            }
            if (userDetailsVo.getTranslateReviewer()){
                Role role = roleMapper.getRoleByName(ConstantInterface.TRANSLATE_AUDITOR);
                roleIdList.add(role.getId());
                roleNameList.add(ConstantInterface.TRANSLATE_AUDITOR);
            }
            user.setRoleName(roleNameList);
            user.setRoleId(roleIdList);
            userList.add(user);

        }
        //删除用户及角色信息
        int delete = userMapper.deleteRoleAndUser(userList);
        int insert = userMapper.insertUser(userList);

        int insert1 = userMapper.insertRoleAndUser(userList);

        return ConstantInterface.OK_STR;
    }*/

    @Override
    public String addUserPermission(List<UserDetailsVo>  users) {
        // 获取所有角色
        List<Role> roleList = roleMapper.getRole(-1, -1);
        Map<String,Role> roleMap = new HashMap<>();
        for (Role role : roleList) {
            roleMap.put(role.getRoleName(),role);
        }
        List<User> userList = new ArrayList<>();
        for (UserDetailsVo userDetailsVo : users) {
            // 判断当前用户是否已存在
            User querUser = new User();
            querUser.setDepartment(userDetailsVo.getDepartment());
            querUser.setUserName(userDetailsVo.getName());
            List<User> userInfo = userMapper.getUser(querUser);

            User user = new User();
            if (userInfo.isEmpty()){
                // 当前用户不存在时则新增用户
                user.setId(commonUtils.getUUID());
                user.setUserName(userDetailsVo.getName());
                user.setDepartment(userDetailsVo.getDepartment());
                userMapper.insert(user);
            }else {
                // 当前用户已存在
                user = userInfo.get(0);
            }

            List<String> roleNameList = new ArrayList<>();
            List<String> roleIdList = new ArrayList<>();

            if (userDetailsVo.getAdmin()){
                // 管理员
                roleIdList.add(roleMap.get(ConstantInterface.ADMIN).getId());
                roleNameList.add(roleMap.get(ConstantInterface.ADMIN).getRoleName());
            }
            if (userDetailsVo.getDeveloper()){
                // 开发员
                roleIdList.add(roleMap.get(ConstantInterface.DEVELOPER).getId());
                roleNameList.add(roleMap.get(ConstantInterface.DEVELOPER).getRoleName());
            }
            if (userDetailsVo.getTranslator()){
                // 翻译员
                roleIdList.add(roleMap.get(ConstantInterface.TRANSLATOR).getId());
                roleNameList.add(roleMap.get(ConstantInterface.TRANSLATOR).getRoleName());
            }
            if (userDetailsVo.getEntryReviewer()){
                // 词条审核员
                roleIdList.add(roleMap.get(ConstantInterface.ENTRY_AUDITOR).getId());
                roleNameList.add(roleMap.get(ConstantInterface.ENTRY_AUDITOR).getRoleName());
            }
            if (userDetailsVo.getTranslateReviewer()){
                // 翻译审核员
                roleIdList.add(roleMap.get(ConstantInterface.TRANSLATE_AUDITOR).getId());
                roleNameList.add(roleMap.get(ConstantInterface.TRANSLATE_AUDITOR).getRoleName());
            }
            if(userDetailsVo.getDevelopAdmin()){
                // 开发管理员
                roleIdList.add(roleMap.get(ConstantInterface.DEVELOP_ADMIN).getId());
                roleNameList.add(roleMap.get(ConstantInterface.DEVELOP_ADMIN).getRoleName());
            }
            if (userDetailsVo.getSuperAdmin()) {
                // 超级管理员
                roleIdList.add(roleMap.get(ConstantInterface.SUPER_ADMIN).getId());
                roleNameList.add(roleMap.get(ConstantInterface.SUPER_ADMIN).getRoleName());
            }
            user.setRoleName(roleNameList);
            user.setRoleId(roleIdList);
            userList.add(user);
        }
        // 1、将t_user_role 表中的用户关联的角色删除
        int delete = userMapper.deleteUserRole(userList);

        // 2、新增用户关联的新角色 t_user_role
        List<UserRole> userRoleList = new ArrayList<>();
        for (User user : userList) {
            for (String s : user.getRoleId()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(s);
                userRoleList.add(userRole);
            }
        }
        if (!userRoleList.isEmpty()){
            // 新增用户角色关联信息
            int insert = userMapper.insertUserRole(userRoleList);
        }
        return ConstantInterface.OK_STR;
    }


    @Override
    public String changeRoleAndMenu(String roleId, List<String> menuIdList) {
        int delete = roleMapper.deleteRoleAndMenu(roleId);
        if (!CollectionUtils.isEmpty(menuIdList)){
            int insert = roleMapper.insertRoleAndMenu(roleId,menuIdList);
        }

        return  ConstantInterface.OK_STR;
    }

    @Override
    public List<Role> getRoleAndMenu(Role role, int offset, int pageSize) {

        List<Role> roles =  roleMapper.getRoleByEntity(role,offset,pageSize);

        return roles;
    }


    @Override
    public List<Menu> getMenu() {
         List<Menu> menus = menuMapper.selectAllInfo();
         List<Menu> menus1 = treeUtils.listTree(menus);
        return menus1;
    }
}
