package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.entity.User;

import java.util.Map;

public interface UserLoginService {
    User getUserInfo(String jobNumber);

    Result login(String account, String password);
}
