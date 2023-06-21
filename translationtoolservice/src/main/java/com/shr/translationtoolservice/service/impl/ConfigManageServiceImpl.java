package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.dao.ConfigManageMapper;
import com.shr.translationtoolservice.entity.ConfigResUser;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName ConfigManageService
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/20 0020 14:09
 **/
@Service
public class ConfigManageServiceImpl implements ConfigManageInterface {
    @Autowired
    ConfigManageMapper configManageMapper;

    @Override
    public List<ConfigResUser> queryUserInfo(ConfigResUser user) {
        List<ConfigResUser> configResUser = configManageMapper.querUser(user);



        return configResUser;
    }

    @Override
    public Integer deleteUserInfoByList(List<String> idList) {
        return configManageMapper.deleteUserInfoByList(idList);
    }

    @Override
    public Integer changeUserInfo(ConfigResUser user) {
        return configManageMapper.changeUserInfo(user);
    }
}
