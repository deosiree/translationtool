package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import com.shr.translationtoolservice.service.impl.ConfigManageServiceImpl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName ConfigManageController
 * @Description
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
    public HttpResponse<ResponseListModel<ConfigResUser>>  queryUserInfo(@RequestBody ConfigResUser user,
                                                           @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                           @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize){
        ResponseListModel<ConfigResUser> result = new ResponseListModel<>();
        List<ConfigResUser>  configResUsers = configManageService.queryUserInfo(user,pageIndex,pageSize);
        int total = configManageService.getUserTotalNum(user);
        result.setList(configResUsers);
        result.setTotalNum(total);
        return checkResult(result);
    }

    @PostMapping("/addUser")
    @ApiOperation("新增用户")
    @CrossOrigin
    public HttpResponse<String> addUser(@RequestBody ConfigResUser user){
        String  userRes = configManageService.addUser(user);

        return checkResult(userRes);
    }

    @PostMapping("/updateUserInfo")
    @ApiOperation("编辑用户信息")
    @CrossOrigin
    public HttpResponse<Integer> updateUserInfo(@RequestBody ConfigResUser user){
        Integer res = configManageService.updateUserInfo(user);
        return checkResult(res);
    }

    @PostMapping("/deleteUser")
    @ApiOperation("删除用户信息")
    @CrossOrigin
    public HttpResponse<Integer> deleteUserInfoByList(@RequestBody List<String> idList){
        Integer res = configManageService.deleteUserInfoByList(idList);
        return checkResult(res);
    }


    @PostMapping("/queryRoleInfo")
    @ApiOperation("查询角色信息")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel> queryRoleInfo( String roleName,
                                                         @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                         @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize){
        ResponseListModel<Role> result = new ResponseListModel<>();
        List<Role> roleRes = configManageService.queryRoleInfo(roleName,pageIndex,pageSize);
        int total = configManageService.getRoleTotaNum(roleName);
        result.setList(roleRes);

        result.setTotalNum(total);

        return checkResult(result);
    }

    @PostMapping("/deleteRoleInfo")
    @ApiOperation("删除角色信息")
    @CrossOrigin
    public HttpResponse<Integer> deleteRoleInfo( String id){

        return checkResult(configManageService.deleteRoleInfo(id));
    }

    @PostMapping("/updateRoleInfo")
    @ApiOperation("编辑角色信息")
    @CrossOrigin
    public HttpResponse<Integer> updateRoleInfo( Role role){

        return checkResult(configManageService.updateRoleInfo(role));
    }

    @PostMapping("/addRoleInfo")
    @ApiOperation("新增角色信息")
    @CrossOrigin
    public HttpResponse<Integer> addRoleInfo( Role role){

        return checkResult(configManageService.insertSelective(role));
    }

    @PostMapping("/bindRoleInfo")
    @ApiOperation("绑定角色信息")
    @CrossOrigin
    public HttpResponse<Integer> bindRoleInfo( ConfigResUser configResUser){

        return checkResult(configManageService.bindRoleInfo(configResUser));
    }

    @PostMapping("/bindPermission")
    @ApiOperation("绑定权限信息")
    @CrossOrigin
    public HttpResponse<Integer> bindPermission( @RequestBody RoleAuthority roleAuthority){

        return checkResult(configManageService.bindPermission(roleAuthority));
    }
}
