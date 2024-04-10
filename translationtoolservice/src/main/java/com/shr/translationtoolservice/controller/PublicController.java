package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.PublicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jdk.internal.dynalink.linker.LinkerServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @title PublicController
 * @create 2024/4/8 14:23
 * @description 公共接口
 **/
@RestController
@RequestMapping("/public")
@Api(tags = "公共接口")
@Slf4j
public class PublicController extends BaseController {

    @Autowired
    private PublicService publicService;


    @PostMapping("/queryTranslate")
    @ApiOperation("查询翻译")
    @ApiImplicitParam(name = "targetLang",value = "目标语言：english、french、russian、spanish")
    @CrossOrigin
    public HttpResponse<ResponseListModel<PublicEntryEntity>> queryTranslate(@RequestBody EntryInfoEntity entity, @RequestParam("targetLang") String targetLang){
        List<PublicEntryEntity> list = publicService.queryTranslate(entity,targetLang);
        ResponseListModel<PublicEntryEntity> result = new ResponseListModel<>();
        result.setList(list);
        result.setTotalNum(list.size());
        return checkResult(result);
    }

    @PostMapping("/realTimeTranslate")
    @ApiOperation("实时翻译")
    @ApiImplicitParam(name = "targetLang",value = "目标语言：english、french、russian、spanish")
    @CrossOrigin
    public HttpResponse<ResponseListModel<PublicEntryEntity>> realTimeTranslate(@RequestBody List<String> entityList, @RequestParam("targetLang") String targetLang){
        List<PublicEntryEntity> translateEntitys = publicService.realTimeTranslate(entityList,targetLang);
        ResponseListModel<PublicEntryEntity> result = new ResponseListModel<>();
        result.setList(translateEntitys);
        result.setTotalNum(translateEntitys.size());
        return checkResult(result);
    }
}
