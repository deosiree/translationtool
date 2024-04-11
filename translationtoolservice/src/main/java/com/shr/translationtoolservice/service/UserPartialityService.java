package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.UserPartiality;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @title UserPartialityService
 * @create 2024/4/11 9:31
 * @description <TODO description class purpose>
 **/
public interface UserPartialityService {
    List<UserPartiality> queryUserPartiality(HttpServletRequest request);

    Integer updateUserPartiality(UserPartiality userPartiality, HttpServletRequest request);
}
