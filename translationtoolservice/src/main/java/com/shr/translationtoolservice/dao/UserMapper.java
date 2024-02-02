package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.UserDetailsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper  extends BaseMapper<User> {
    int deleteByPrimaryKey(String id);

    int insert(User record);

    int insertSelective(User record);

    User selectByName(String userName);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);


    List<ConfigResUser> querUser(@Param("user") ConfigResUser user,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);

    Integer deleteUserInfoByList(List<String> idList);

    Integer updateUserInfo(@Param("user") ConfigResUser user);



    int getUserTotalNum(@Param("user") ConfigResUser user);


    List<User> getRoleUserByDepartment(String department);

    User getPermissionByNameAndDepartment( @Param("name") String name, @Param("department") String department);

    List<User> getUserInfo(@Param("user") User user,@Param("offset") Integer offset,@Param("limit") Integer pageSize);

    int getUserInfoTotal(@Param("user") User user);

    int deleteRoleAndUser(@Param("users") List<User> userList);

    int deleteUser(@Param("users") List<User> userList);

    int insertUser(@Param("users")  List<User> userList);

    int insertRoleAndUser(@Param("users") List<User> userList);

    int deleteUserRole(@Param("userList") List<User> userList);

    int insertUserRole(@Param("list") List<UserRole> userRoleList);

    List<User> getUser(@Param("user") User querUser);
}