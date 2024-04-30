package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.RoleUserVO;
import com.shr.translationtoolservice.entity.vo.UserDetailsVo;
import com.shr.translationtoolservice.service.UserManageService;
import com.shr.translationtoolservice.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @ClassName UserController
 * @USER: Cola
 * @Date 2023/11/10 0010 8:41
 **/

@RestController
@RequestMapping("/userManage")
@Api(tags = "用户管理")
@Slf4j
public class UserController extends BaseController {

    @Autowired
    private UserManageService userManageService;
    @Autowired
    private CommonUtils commonUtils;

    @PostMapping("/getRoleUserByDepartment")
    @ApiOperation("查询各部门不同角色用户")
    @CrossOrigin
    @Transactional
    public HttpResponse<HashMap<String, List<User>>> getRoleUserByDepartment( String department) {

        HashMap<String, List<User>> roleUserVOS = userManageService.getRoleUserByDepartment(department);

        return checkResult(roleUserVOS);

    }

    @PostMapping("/getDepartments")
    @ApiOperation("查询所有部门")
    @CrossOrigin
    @Transactional
    public HttpResponse< ResponseListModel<String> > getDepartments() {
        ResponseListModel<String> result = new ResponseListModel<>();
        List<String> departments = userManageService.getDepartments();
        result.setList(departments);
        result.setTotalNum(departments.size());
        return checkResult(result);

    }





    @PostMapping("/getUserPermission")
    @ApiOperation("查询用户权限")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<UserDetailsVo> > getUserPermission(String name) {
        ResponseListModel<UserDetailsVo> result = new ResponseListModel<>();
        List<UserDetailsVo>  userDetailsVo = userManageService.getUserPermission(name);
        result.setList(userDetailsVo);
        result.setTotalNum(userDetailsVo.size());
        return checkResult(result);

    }


    @PostMapping("/getUserInfo")
    @ApiOperation("查询用户")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<User> > getUserInfo(@RequestBody User user,
                                                              @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                              @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<User> result = new ResponseListModel<>();
        List<User>  users = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            users = userManageService.getUserInfo(user,offset,pageSize);
        }
        result.setList(users);
        result.setTotalNum(userManageService.getUserInfoTotal(user));
        return checkResult(result);

    }


    @PostMapping("/addUserPermission")
    @ApiOperation("新增用户权限")
    @CrossOrigin
    @Transactional
    public HttpResponse<String > addUserPermission(@RequestBody List<UserDetailsVo> userDetailsVos) {

        String  res = userManageService.addUserPermission(userDetailsVos);
        return checkResult(res);
    }

    @PostMapping("/changeRoleAndMenu")
    @ApiOperation("角色权限修改")
    @CrossOrigin
    @Transactional
    public HttpResponse<String > changeRoleAndMenu( String roleId,
                                                   @RequestBody List<String> menuIdList) {
        String  res = userManageService.changeRoleAndMenu(roleId,menuIdList);
        return checkResult(res);
    }

    @PostMapping("/getRoleAndMenu")
    @ApiOperation("角色权限查询")
    @CrossOrigin
    @Transactional
    public HttpResponse< ResponseListModel<Role> > getRoleAndMenu(@RequestBody Role role,
                                                                  @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                  @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<Role> result = new ResponseListModel<>();
        List<Role>  res = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            res = userManageService.getRoleAndMenu(role,offset,pageSize);
        }
        result.setList(res);
        result.setTotalNum(res.size());
        return checkResult(result);
    }


    @PostMapping("/getMenu")
    @ApiOperation("菜单查询")
    @CrossOrigin
    @Transactional
    public HttpResponse< ResponseListModel<Menu> > getMenu() {
        ResponseListModel<Menu> result = new ResponseListModel<>();

        List<Menu> menus =  userManageService.getMenu();
        result.setList(menus);
        return checkResult(result);
    }
}
