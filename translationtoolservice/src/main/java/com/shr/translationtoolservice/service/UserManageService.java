package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.entity.vo.RoleUserVO;

import java.util.HashMap;
import java.util.List;

public interface UserManageService {
     int getRoleUserByDepartmentTotal();

     HashMap<String, List<User>> getRoleUserByDepartment(String department);

     int getRoleTotal();

     List<String> getDepartments();
}
