package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.ConfigManageInterface;
import com.shr.translationtoolservice.service.impl.ConfigManageServiceImpl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

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
    public HttpResponse<ResponseListModel<ConfigResUser>> queryUserInfo(@RequestBody ConfigResUser user,
                                                                        @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<ConfigResUser> result = new ResponseListModel<>();
        List<ConfigResUser> configResUsers = configManageService.queryUserInfo(user, pageIndex, pageSize);
        int total = configManageService.getUserTotalNum(user);
        result.setList(configResUsers);
        result.setTotalNum(total);
        return checkResult(result);
    }

    @PostMapping("/addUser")
    @ApiOperation("新增用户")
    @CrossOrigin
    public HttpResponse<String> addUser(@RequestBody ConfigResUser user) {
        if (Objects.isNull(user) || StringUtils.isBlank(user.getUserName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        String userRes = configManageService.addUser(user);

        return checkResult(userRes);
    }

    @PostMapping("/updateUserInfo")
    @ApiOperation("编辑用户信息")
    @CrossOrigin
    public HttpResponse<String> updateUserInfo(@RequestBody ConfigResUser user) {
        if (Objects.isNull(user)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.updateUserInfo(user));
    }

    @PostMapping("/deleteUser")
    @ApiOperation("删除用户信息")
    @CrossOrigin
    public HttpResponse<String> deleteUserInfoByList(@RequestBody List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.deleteUserInfoByList(idList));
    }


    @PostMapping("/queryRoleInfo")
    @ApiOperation("查询角色信息")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel> queryRoleInfo(String roleName,
                                                         @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                         @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<Role> result = new ResponseListModel<>();
        List<Role> roleRes = configManageService.queryRoleInfo(roleName, pageIndex, pageSize);
        int total = configManageService.getRoleTotaNum(roleName);
        result.setList(roleRes);

        result.setTotalNum(total);

        return checkResult(result);
    }

    @PostMapping("/deleteRoleInfo")
    @ApiOperation("删除角色信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteRoleInfo(@RequestBody List<String> roleIDs, HttpServletRequest request) {

        if (CollectionUtils.isEmpty(roleIDs)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.deleteRoleInfo(roleIDs));
    }

    @PostMapping("/updateRoleInfo")
    @ApiOperation("编辑角色信息")
    @CrossOrigin
    public HttpResponse<String> updateRoleInfo(Role role) {
        if (Objects.isNull(role)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.updateRoleInfo(role));
    }

    @PostMapping("/addRoleInfo")
    @ApiOperation("新增角色信息")
    @CrossOrigin
    public HttpResponse<String> addRoleInfo(Role role) {
        if (Objects.isNull(role) || StringUtils.isBlank(role.getRoleName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.insertRoleInfo(role));
    }

    @PostMapping("/bindRoleInfo")
    @ApiOperation("绑定角色信息")
    @CrossOrigin
    public HttpResponse<String> bindRoleInfo(ConfigResUser configResUser) {
        if (Objects.isNull(configResUser) || StringUtils.isNotBlank(configResUser.getId())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.bindRoleInfo(configResUser));
    }

    @PostMapping("/bindPermission")
    @ApiOperation("绑定权限信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> bindPermission(@RequestBody RoleAuthorityRes roleAuthorityRes) {

        if (Objects.isNull(roleAuthorityRes) || StringUtils.isBlank(roleAuthorityRes.getRoleID()) ){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.bindPermission(roleAuthorityRes));
    }

    @PostMapping("/queryVersionInfo")
    @ApiOperation("查询版本信息")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel> queryVersionInfo(String versionName,
                                                            @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryVersion> result = new ResponseListModel<>();
        List<EntryVersion> roleRes = configManageService.queryVersionInfo(versionName, pageIndex, pageSize);
        int total = configManageService.getVersionTotaNum(versionName);
        result.setList(roleRes);

        result.setTotalNum(total);

        return checkResult(result);
    }

    @PostMapping("/updateVersionInfo")
    @ApiOperation("编辑版本信息")
    @CrossOrigin
    //TODO
    public HttpResponse<String> updateVersionInfo(EntryVersion entryVersion) {
        if (Objects.isNull(entryVersion) || StringUtils.isBlank(entryVersion.getId())){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.updateVersionInfo(entryVersion));
    }

    @PostMapping("/deleteVersionInfo")
    @ApiOperation("删除版本信息")
    @CrossOrigin
    //TODO
    public HttpResponse<String> deleteVersionInfo(@RequestBody List<String> idList) {
        if (CollectionUtils.isEmpty(idList)){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.deleteVersionInfo(idList));
    }

    @PostMapping("/addVersionInfo")
    @ApiOperation("新增版本信息")
    @CrossOrigin
    public HttpResponse<String> addVersionInfo(EntryVersion entryVersion) {
        if (Objects.isNull(entryVersion) || StringUtils.isBlank(entryVersion.getName())){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(configManageService.addVersionInfo(entryVersion));
    }

    @PostMapping("/getMenuInfoByRole")
    @ApiOperation("菜单角色配置查询")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel<Menu>> getMenuInfoByRole( String roleID) {
        ResponseListModel<Menu> result = new ResponseListModel<>();
        if (StringUtils.isBlank(roleID)) {
            return checkResult(result);
        }
        result.setList(configManageService.getMenuInfoByRole(roleID));
        result.setTotalNum(configManageService.getMenuTotal());
        return checkResult(result);
    }

    @PostMapping("/getPropertyByName")
    @ApiOperation("词性条件查询")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel<EntryProperty>> getPropertyByName( String propertyName,
                                                                             @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                             @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryProperty> result = new ResponseListModel<>();
        result.setList(configManageService.getPropertyByName(propertyName,pageIndex,pageSize));
        result.setTotalNum(configManageService.getPropertyByNameTotal(propertyName));
        return checkResult(result);
    }

    @PostMapping("/updateProperty")
    @ApiOperation("词性修改")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> updateProperty( EntryProperty entryProperty) {

        return checkResult(configManageService.updateProperty(entryProperty));
    }

    @PostMapping("/addProperty")
    @ApiOperation("词性新增")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> addProperty( EntryProperty entryProperty) {

        return checkResult(configManageService.addProperty(entryProperty));
    }

    @PostMapping("/deleteProperty")
    @ApiOperation("词性删除")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> deleteProperty( String id) {

        return checkResult(configManageService.deleteProperty(id));
    }

}

/*
}
*/
