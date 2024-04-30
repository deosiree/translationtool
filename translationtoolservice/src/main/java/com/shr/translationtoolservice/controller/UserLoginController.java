package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.common.ResultCode;
import com.shr.translationtoolservice.service.UserLoginService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/userLogin")
@Api(tags = "用户登录")
@Slf4j
public class UserLoginController {

    @Autowired
    private UserLoginService userLoginService;

    @PostMapping("/login")
    @ApiOperation("用户登录")
    @CrossOrigin
    public Result login(@RequestParam("account") String account, @RequestParam("password") String password){
        if (account.isEmpty() || password.isEmpty()){
            log.info("用户名或密码为空，登录失败");
            return Result.fail(ResultCode.ACCOUNT_PASSWARD_NULL.getCode(),ResultCode.ACCOUNT_PASSWARD_NULL.getMessage());
        }
        Result result = userLoginService.login(account, password);
        return result;
    }

}
