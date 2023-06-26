package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ConfigResUser;
import com.shr.translationtoolservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConfigManageMapper {
    List<ConfigResUser> querUser(@Param("user") ConfigResUser user);

    Integer deleteUserInfoByList(List<String> idList);

    Integer changeUserInfo(@Param("user") ConfigResUser user);
}
