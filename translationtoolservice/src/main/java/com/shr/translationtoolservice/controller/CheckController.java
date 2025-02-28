package com.shr.translationtoolservice.controller;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.vo.TaskEntryVO;
import com.shr.translationtoolservice.service.CheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/checkManage")
@Api(tags = "校验管理")
@Slf4j
public class CheckController  extends BaseController{

    @Autowired
    private CheckService checkService;


    @GetMapping("/getModuleNames")
    @ApiOperation("查询数据库中所有的模块名称")
    @CrossOrigin
    @Transactional
    public HttpResponse<Map<String, Object>> getModuleNames( @RequestParam String url ) {
        JsonObject res = checkService.getModuleNames(url);
        if (!Objects.isNull(res)) {
            Gson gson = new Gson();
            Map<String, Object> resMap = gson.fromJson(res, Map.class);
            return checkResult(resMap);
        } else {
            return checkResult(null,"服务异常！");
        }
    }

    @GetMapping("/getQuestionTypes")
    @ApiOperation("查询数据库中所有的问题类型")
    @CrossOrigin
    @Transactional
    public HttpResponse< Map<String, Object>> getQuestionTypes( @RequestParam String url ) {
        JsonObject res = checkService.getQuestionTypes(url);
        if (!Objects.isNull(res)) {
            Gson gson = new Gson();
            Map<String, Object> resMap = gson.fromJson(res, Map.class);
            return checkResult(resMap);
        } else {
            return checkResult(null,"服务异常！");
        }
    }

    @GetMapping("/searchCheckInfo")
    @ApiOperation("根据模块名称和问题名称进⾏校验")
    @CrossOrigin
    @Transactional
    public HttpResponse<Map<String, Object>> searchCheckInfo( @RequestParam String url, @RequestParam String checkUrl ) {
        JsonObject res = checkService.searchCheckInfo(url,checkUrl);
        if (!Objects.isNull(res)) {
            Gson gson = new Gson();
            Map<String, Object> resMap = gson.fromJson(res, Map.class);
            return checkResult(resMap);
        } else {
            return checkResult(null,"服务异常！");
        }
    }
}
