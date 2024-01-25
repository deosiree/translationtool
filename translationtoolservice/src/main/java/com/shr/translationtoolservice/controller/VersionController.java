package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.service.VersionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @ClassName VersionController
 * @USER: Cola
 * @Date 2023/11/9 0009 15:03
 **/


@RestController
@RequestMapping("/version")
@Api(tags = "版本管理")
@Slf4j
public class VersionController  extends BaseController {

    @Autowired
    private VersionService versionService;

    @PostMapping("/getVersion")
    @ApiOperation("版本查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<  ResponseListModel<VersionEntity>> getVersion(@RequestBody VersionEntity versionEntity) {
        ResponseListModel<VersionEntity> result = new ResponseListModel<>();

        List<VersionEntity> versionEntities = versionService.getVersion(versionEntity);
        result.setList(versionEntities);
        int total = versionService.getVersionTotal(versionEntity);
        result.setTotalNum(total);
        return checkResult(result);

    }


    @PostMapping("/createVersion")
    @ApiOperation("版本创建")
    @CrossOrigin
    @Transactional
    public HttpResponse< String> createVersion(@RequestBody VersionEntity versionEntity, HttpServletRequest request) {


        String result = versionService.createVersion(versionEntity,request);

        return checkResult(result);

    }



    @PostMapping("/updateVersion")
    @ApiOperation("版本修改")
    @CrossOrigin
    public HttpResponse< String> updateVersion(@RequestBody VersionEntity versionEntity) {


        String result = versionService.updateVersion(versionEntity);

        return checkResult(result);

    }


    @PostMapping("/deleteVersion")
    @ApiOperation("版本删除")
    @CrossOrigin
    public HttpResponse< String> deleteVersion(@RequestBody List<String> idList) {
        String result = versionService.deleteVersion(idList);

        return checkResult(result);

    }


    @GetMapping("/getVersionByName")
    @ApiOperation("版本名查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<  ResponseListModel<VersionEntity>> getVersionByName( String versionName,
                                                                             @RequestParam String productID) {
        ResponseListModel<VersionEntity> result = new ResponseListModel<>();

        List<VersionEntity> versionEntities = versionService.getVersionByName(versionName,productID);
        result.setList(versionEntities);
        int total = versionEntities.size();
        result.setTotalNum(total);
        return checkResult(result);

    }

}
