package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.dao.EntryCommonEntityMapper;
import com.shr.translationtoolservice.dao.EntryProductEntityMapper;
import com.shr.translationtoolservice.dao.EntryProjectEntityMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.EntryCommonEntityService;
import com.shr.translationtoolservice.service.EntryManagementService;

import com.shr.translationtoolservice.service.EntryProductEntityService;
import com.shr.translationtoolservice.service.EntryProjectEntityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/entry")
@Api(tags = "词条管理")
@Slf4j
public class EntryController extends BaseController{
    @Autowired
    EntryManagementService entryManagementService;

    @Autowired
    EntryProductEntityMapper entryProductEntityMapper;
    @Autowired
    EntryProjectEntityMapper entryProjectEntityMapper;
    @Autowired
    EntryCommonEntityMapper entryCommonEntityMapper;
    @Autowired
    EntryProjectEntityService entryProjectEntityService;
    @Autowired
    EntryCommonEntityService entryCommonEntityService;
    @Autowired
    EntryProductEntityService entryProductEntityService;

    //查询词条信息
    @PostMapping("/searchEntry")
    @ApiOperation("词条查询")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel> searchEntry(@RequestBody EntryReqEntity entryReqEntity,
                                                         @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                         @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize){

        ResponseListModel result = new ResponseListModel<>();
        entryManagementService.searchEntry(entryReqEntity,pageIndex,pageSize);
         return checkResult(result);
    }


    //新增词条
    @PostMapping("/insertEntry")
    @ApiOperation("新增词条")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> insertEntry(@RequestBody EntryParentEntity entryParentEntity,String type){
        return checkResult( entryManagementService.insertEntry(entryParentEntity,type));
    }

    //新增词条
    @PostMapping("/updateEntry")
    @ApiOperation("编辑词条")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> updateEntry(@RequestBody EntryParentEntity entryParentEntity,String type){

        return checkResult( entryManagementService.updateEntry(entryParentEntity,type));
    }

    //新增词条
    @PostMapping("/deleteEntry")
    @ApiOperation("删除词条")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> deleteEntry(@RequestBody EntryParentEntity entryParentEntity,String type){


        return checkResult( entryManagementService.deleteEntry(entryParentEntity,type));

    }




    //批量审核
    @PostMapping("/bathAudit")
    @ApiOperation("批量审核")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> bathAudit(  @RequestParam(value = "idList") List<String> idList){
        if (CollectionUtils.isEmpty(idList)){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        String result = entryManagementService.bathAudit(idList);

        return checkResult(result);
    }





}
