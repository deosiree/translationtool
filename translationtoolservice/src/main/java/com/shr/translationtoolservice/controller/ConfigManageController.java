package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.entity.ConfigResUser;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import com.shr.translationtoolservice.service.impl.ConfigManageServiceImpl;
import com.shr.translationtoolservice.util.HttpResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName ConfigManageController
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/20 0020 14:07
 **/
@RestController
@RequestMapping("/configManage")
@Api(tags = "配置管理")
@Slf4j
public class ConfigManageController extends BaseController {
    @Autowired
    ConfigManageInterface configManageService;


    @PostMapping("/queryUser")
    @ApiOperation("查询用户信息")
    @PassToken
    @CrossOrigin
    public HttpResponse<List<ConfigResUser>> queryUserInfo(@RequestBody ConfigResUser user){
        List<ConfigResUser> userRes = configManageService.queryUserInfo(user);
        return checkResult(userRes);
    }

    @PostMapping("/changeUser")
    @ApiOperation("修改用户信息")
    @PassToken
    @CrossOrigin
    public HttpResponse<Integer> changeUserInfo(@RequestBody ConfigResUser user){
        Integer res = configManageService.changeUserInfo(user);
        return checkResult(res);
    }

    @PostMapping("/deleteUser")
    @ApiOperation("删除用户信息")
    @PassToken
    @CrossOrigin
    public HttpResponse<Integer> deleteUserInfoByList(@RequestBody List<String> idList){
        Integer res = configManageService.deleteUserInfoByList(idList);
        return checkResult(res);
    }
}
