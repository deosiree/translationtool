package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.ConfigResUser;

import java.util.List;

public interface ConfigManageInterface {

    List<ConfigResUser> queryUserInfo(ConfigResUser user);

    Integer deleteUserInfoByList(List<String> idList);

    Integer changeUserInfo(ConfigResUser user);
}
