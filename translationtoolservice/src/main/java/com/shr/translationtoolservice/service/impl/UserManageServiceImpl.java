package com.shr.translationtoolservice.service.impl;

import ch.qos.logback.classic.pattern.LineOfCallerConverter;
import cn.hutool.core.lang.tree.TreeUtil;
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
        List<UserDetailsVo> userDetailsAllVos = new ArrayList<>();
        // key -> department , value -> ldapUsers
        Map<String, List<LDAPUser>> allUser = ldapUtils.getAllUser();

        //匹配指定条件结果
        if (StringUtils.isNotBlank(filter)) {
            //部门匹配
            if (!CollectionUtils.isEmpty(allUser.get(filter))) {

                UserDetailsVo userDetailsDepartVo = new UserDetailsVo();
                userDetailsDepartVo.setName(filter);
                userDetailsDepartVo.setType(ConstantInterface.DEPARTMENT);
                List<UserDetailsVo> userDetailsVos = constructLdapList(allUser.get(filter), filter, "");
                userDetailsDepartVo.setChildren(userDetailsVos);
                userDetailsAllVos.add(userDetailsDepartVo);
                return userDetailsAllVos;
            }
        }

        //全量构建
        Set<String> ldapUserSet = allUser.keySet();
        Iterator<String> iterator = ldapUserSet.iterator();
        while (iterator.hasNext()) {
            UserDetailsVo userDetailsDepartVo = new UserDetailsVo();
            String department = iterator.next();

            List<LDAPUser> ldapUsers = allUser.get(department);
            List<UserDetailsVo> userDetailsVos = constructLdapList(ldapUsers, department, filter);
            if (CollectionUtils.isEmpty(userDetailsVos)) {
                continue;
            }
            userDetailsDepartVo.setName(department);
            userDetailsDepartVo.setType("department");
            userDetailsDepartVo.setChildren(userDetailsVos);
            userDetailsAllVos.add(userDetailsDepartVo);
            //userDetailsVo.setChildren(allUser.get(department));
        }


        return userDetailsAllVos;

    }

    private List<UserDetailsVo> constructLdapList(List<LDAPUser> ldapUsers, String department, String filter) {
        List<UserDetailsVo> userDetailsVos = new ArrayList<>();
        for (LDAPUser ldapUser : ldapUsers) {
            UserDetailsVo userDetailsVo = new UserDetailsVo();

            String name = ldapUser.getName();
            if (!StringUtils.isBlank(filter) && !name.equals(filter)) {
                continue;
            }
            User user = userMapper.getPermissionByNameAndDepartment(name, department);
            userDetailsVo.setType(ConstantInterface.USER);
            userDetailsVo.setName(name);

            if (!Objects.isNull(user)) {
                userDetailsVo.setDepartment(user.getDepartment());
                //角色信息添加
                if (user.getRoleName().contains(ConstantInterface.TRANSLATOR)) {
                    userDetailsVo.setTranslator(true);
                } else {
                    userDetailsVo.setTranslator(false);
                }
                if (user.getRoleName().contains(ConstantInterface.DEVELOPER)) {
                    userDetailsVo.setDeveloper(true);
                } else {
                    userDetailsVo.setDeveloper(false);
                }
                if (user.getRoleName().contains(ConstantInterface.TRANSLATE_AUDITOR)) {
                    userDetailsVo.setTranslateReviewer(true);
                } else {
                    userDetailsVo.setTranslateReviewer(false);
                }
                if (user.getRoleName().contains(ConstantInterface.ENTRY_AUDITOR)) {
                    userDetailsVo.setEntryReviewer(true);
                } else {
                    userDetailsVo.setEntryReviewer(false);
                }
                if (user.getRoleName().contains(ConstantInterface.ADMIN)) {
                    userDetailsVo.setAdmin(true);
                } else {
                    userDetailsVo.setAdmin(false);
                }
            }else {
                userDetailsVo.setTranslator(false);
                userDetailsVo.setDeveloper(false);
                userDetailsVo.setTranslateReviewer(false);
                userDetailsVo.setEntryReviewer(false);
                userDetailsVo.setAdmin(false);
                userDetailsVo.setDepartment(department);
            }
            userDetailsVos.add(userDetailsVo);
        }
        return userDetailsVos;
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

    @Override
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
