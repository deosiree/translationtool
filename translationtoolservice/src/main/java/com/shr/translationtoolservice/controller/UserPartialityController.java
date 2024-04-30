package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.SecondClassify;
import com.shr.translationtoolservice.entity.UserPartiality;
import com.shr.translationtoolservice.service.UserPartialityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @title UserPartialityController
 * @create 2024/4/11 9:29
 * @description 用户偏好
 **/
@RestController
@RequestMapping("/userPartiality")
@Api(tags = "用户偏好")
@Slf4j
public class UserPartialityController extends BaseController {

    @Autowired
    private UserPartialityService userPartialityService;

    /**
     * 查询用户偏好
     *
     * @return
     */
    @PostMapping("/queryUserPartiality")
    @ApiOperation("查询用户偏好")
    @CrossOrigin
    public HttpResponse<ResponseListModel<UserPartiality>> queryUserPartiality(HttpServletRequest request) {
        List<UserPartiality> userPartiality = userPartialityService.queryUserPartiality(request);
        ResponseListModel<UserPartiality> result = new ResponseListModel<>();
        result.setList(userPartiality);
        result.setTotalNum(userPartiality.size());
        return checkResult(result);
    }

    /**
     * 修改 用户偏好
     *
     * @param userPartiality
     * @param request
     * @return
     */
    @PostMapping("/updateUserPartiality")
    @ApiOperation("编辑用户偏好")
    @CrossOrigin
    public HttpResponse<Integer> updateUserPartiality(@RequestBody UserPartiality userPartiality, HttpServletRequest request) {
        Integer update = userPartialityService.updateUserPartiality(userPartiality, request);
        return checkResult(update);
    }

}
