package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.common.ResultCode;
import com.shr.translationtoolservice.service.UserLoginService;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.LDAPUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/userLogin")
@Api(tags = "用户登录")
@Slf4j
public class UserLoginController {

    @Autowired
    private UserLoginService userLoginService;

    @PostMapping("/login")
    @ApiOperation("用户登录")
    @PassToken
    public Result login(@RequestParam String account, @RequestParam String passWard){
        if (account.isEmpty() || passWard.isEmpty()){
            log.info("用户名或密码为空，登录失败");
            return Result.fail(ResultCode.ACCOUNT_PASSWARD_NULL.getCode(),ResultCode.ACCOUNT_PASSWARD_NULL.getMessage());
        }
        Result result = userLoginService.login(account, passWard);
        return result;
    }

}
