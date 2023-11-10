package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.RoleMapper;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.entity.vo.RoleUserVO;
import com.shr.translationtoolservice.service.UserManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

}
