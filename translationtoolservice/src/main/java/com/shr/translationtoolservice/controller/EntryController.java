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
import com.shr.translationtoolservice.util.JWTTokenUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/entry")
@Api(tags = "词条管理")
@Slf4j
public class EntryController extends BaseController {
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
    public HttpResponse<ResponseListModel> searchEntry(@RequestBody EntryEntity entryEntity,
                                                       @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                       @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {

        return checkResult(  entryManagementService.searchEntry(entryEntity, pageIndex, pageSize));
    }


    //新增词条
    @PostMapping("/insertEntry")
    @ApiOperation("新增词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> insertEntry(@RequestBody EntryEntity entryEntity,HttpServletRequest request) {
        if (StringUtils.isBlank(entryEntity.getTableName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(entryManagementService.insertEntry(entryEntity,request));
    }

    //新增词条
    @PostMapping("/updateEntry")
    @ApiOperation("编辑词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateEntry(@RequestBody EntryEntity entryEntity,HttpServletRequest request) {
        if (StringUtils.isBlank(entryEntity.getTableName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(entryManagementService.updateEntry(entryEntity,request));
    }

    @PostMapping("/entryMerge")
    @ApiOperation("词条合并")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> entryMerge(@RequestBody List<EntryEntity> entryEntities,HttpServletRequest request) {

        return checkResult( entryManagementService.entryMerge(entryEntities));
    }

    @PostMapping("/getReEntry")
    @ApiOperation("重复词条查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> entryMerge(String mergeState) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryEntity> entryEntities = new ArrayList<>();
        entryEntities = entryManagementService.selectRepeEntry(mergeState);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/importEntry")
    @ApiOperation("导入")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> importEntry(File file) {
        ResponseListModel responseListModel = new ResponseListModel();

        return checkResult(null);
    }

    @PostMapping("/outEntry")
    @ApiOperation("导出")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> outEntry(List<String>  entryIds) {
        ResponseListModel responseListModel = new ResponseListModel();

        return checkResult(null);

    }

    @PostMapping("/getEntryClassfy")
    @ApiOperation("词条分类查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryClassfy(  @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                             @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryClassify> entryClassifies = new ArrayList<>();
        entryClassifies = entryManagementService.getEntryClassfy(pageIndex,pageSize);
        responseListModel.setList(entryClassifies);
        responseListModel.setTotalNum(entryClassifies.size());
        return checkResult(responseListModel);

    }


    @PostMapping("/updateEntryClassfy")
    @ApiOperation("词条分类修改")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateEntryClassfy(EntryClassify entryClassify) {

        return checkResult(null);

    }

    @PostMapping("/deleteEntryClassfy")
    @ApiOperation("词条分类删除")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteEntryClassfy(List<String>  ids) {

        return checkResult(null);

    }


    @PostMapping("/addEntryClassfy")
    @ApiOperation("词条分类新增")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> addEntryClassfy(EntryClassify  entryClassifies) {

        return checkResult(null);

    }


    //新增词条
    @PostMapping("/deleteEntry")
    @ApiOperation("删除词条")
    @CrossOrigin
    public HttpResponse<String> deleteEntry(@RequestBody List<EntryEntity> entryEntities,String tableName) {
        if (CollectionUtils.isEmpty(entryEntities) || StringUtils.isBlank(tableName)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }


        return checkResult(entryManagementService.deleteEntry(entryEntities,tableName));

    }


    //批量审核
    @PostMapping("/bathAudit")
    @ApiOperation("批量审核")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> bathAudit(@RequestBody List<EntryGroupEntity> entryGroupEntities, int state, HttpServletRequest request) {


        if (CollectionUtils.isEmpty(entryGroupEntities)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        String result = entryManagementService.bathAudit(entryGroupEntities,state,request);

        return checkResult(result);
    }


    @PostMapping("/getOperateByEntryId")
    @ApiOperation("操作记录查询")
    @PassToken
    @CrossOrigin
    @Transactional
    public HttpResponse<EntryOperate> queryOperate(String  entryId) {


        EntryOperate operate = entryManagementService.queryOperate(entryId);

        return checkResult(operate);
    }



    @PostMapping("/translate")
    @ApiOperation("翻译词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<TranslateEntity> translate(EntryEntity entryEntity) {


        TranslateEntity translateEntity = entryManagementService.translate(entryEntity);
        return checkResult(translateEntity);
    }


}
