package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.Menu;
import com.shr.translationtoolservice.entity.Role;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.entity.vo.RoleUserVO;
import com.shr.translationtoolservice.entity.vo.UserDetailsVo;

import java.util.HashMap;
import java.util.List;

public interface UserManageService {
    int getRoleUserByDepartmentTotal();

    HashMap<String, List<User>> getRoleUserByDepartment(String department);

    int getRoleTotal();

    List<String> getDepartments();

    List<UserDetailsVo> getUserPermission(String name);

    List<User> getUserInfo(User user, Integer offset, Integer pageSize);

    int getUserInfoTotal(User user);

    String addUserPermission(List<UserDetailsVo> users);

    String changeRoleAndMenu(String roleId, List<String> menuIdList);

    List<Role> getRoleAndMenu(Role role, int offset, int pageSize);

    List<Menu> getMenu();
}
