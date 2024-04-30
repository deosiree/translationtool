package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.dao.SecondClassifyMapper;
import com.shr.translationtoolservice.entity.SecondClassify;
import com.shr.translationtoolservice.service.SecondClassifyInterface;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @title SecondClassifyServiceImpl
 * @create 2024/3/8 14:16
 * @description <TODO description class purpose>
 **/
@Service
public class SecondClassifyServiceImpl implements SecondClassifyInterface {

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private SecondClassifyMapper secondClassifyMapper;

    @Override
    public String addSecondClassify(SecondClassify secondClassify, HttpServletRequest request) {
        String id = commonUtils.getUUID();
        secondClassify.setId(id);
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        secondClassify.setCreator(userName);
        Date date = new Date(System.currentTimeMillis());
        secondClassify.setCreateTime(date);
        secondClassifyMapper.insert(secondClassify);
        return id;
    }

    @Override
    public List<SecondClassify> getSecondClassify(SecondClassify secondClassify) {
        List<SecondClassify> classifyList = secondClassifyMapper.selectSecondClassify(secondClassify);
        return classifyList;
    }

    @Override
    public Integer updateSecondClassify(SecondClassify secondClassify) {
        Integer i = secondClassifyMapper.updateByPrimaryKeySelective(secondClassify);
        return i;
    }

    @Override
    public Integer deleteSecondClassify(List<String> ids) {
        Integer delete = secondClassifyMapper.deleteByPrimaryKey(ids);
        return delete;
    }
}
