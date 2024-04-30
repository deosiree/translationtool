package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.SecondClassify;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @title SecondClassifyInterface
 * @create 2024/3/8 14:15
 * @description <TODO description class purpose>
 **/
public interface SecondClassifyInterface {
    String addSecondClassify(SecondClassify secondClassify, HttpServletRequest request);

    List<SecondClassify> getSecondClassify(SecondClassify secondClassify);

    Integer updateSecondClassify(SecondClassify secondClassify);

    Integer deleteSecondClassify(List<String> id);
}
