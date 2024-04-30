package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.dao.UserPartialityMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.UserPartiality;
import com.shr.translationtoolservice.service.UserPartialityService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @title UserPartialityServiceImpl
 * @create 2024/4/11 9:31
 * @description <TODO description class purpose>
 **/
@Service
public class UserPartialityServiceImpl implements UserPartialityService {

    @Autowired
    private UserPartialityMapper userPartialityMapper;

    @Autowired
    private CommonUtils commonUtils;


    @Override
    public List<UserPartiality> queryUserPartiality(HttpServletRequest request) {
        // 获取token
        String token = request.getHeader(ConstantInterface.TOKEN);
        // 获取用户名称
        String userName = JWTTokenUtils.getUserName(token);
        UserPartiality user = new UserPartiality();
        user.setUserName(userName);
        List<UserPartiality> select = userPartialityMapper.select(user);
        return select;
    }

    @Override
    public Integer updateUserPartiality(UserPartiality userPartiality, HttpServletRequest request) {
        // 获取token
        String token = request.getHeader(ConstantInterface.TOKEN);
        // 获取用户名称
        String userName = JWTTokenUtils.getUserName(token);
        userPartiality.setUserName(userName);
        // 查询当前用户 表中是否已存在
        List<UserPartiality> select = userPartialityMapper.select(userPartiality);
        Integer update = null;
        userPartiality.setUpdateTime(new Date());
        if (select.isEmpty()) {
            // 表中没有  则新增
            userPartiality.setId(commonUtils.getUUID());
            update = userPartialityMapper.insert(userPartiality);
        } else {
            // 表中存在  则修改
            userPartiality.setId(select.get(ConstantInterface.ZERO).getId());
            update = userPartialityMapper.updateByPrimaryKeySelective(userPartiality);
        }
        return update;
    }
}
