package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ConfigResUser;
import com.shr.translationtoolservice.entity.RoleEntity;
import com.shr.translationtoolservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
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


}