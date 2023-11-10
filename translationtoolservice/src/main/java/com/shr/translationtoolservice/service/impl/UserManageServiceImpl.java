package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.entity.vo.RoleUserVO;
import com.shr.translationtoolservice.service.UserManageService;
import com.shr.translationtoolservice.util.LDAPUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    private LDAPUtils ldapUtils;

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
            String role = ConstantInterface.constructUserAndRole().get(user.getRoleName());
            List<User> userList = userMap.get(role);
            if (CollectionUtils.isEmpty(userList)) {
                userList = new ArrayList<>();
            }
            userList.add(user);
            userMap.put(role, userList);
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

        return departments ;
    }
}
