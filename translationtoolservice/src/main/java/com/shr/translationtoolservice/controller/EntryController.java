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
import com.shr.translationtoolservice.util.Translate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                                                       String entryState,
                                                       @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                       @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel result = new ResponseListModel<>();
        if (StringUtils.isBlank(entryEntity.getTableName())){
            return checkResult(result);
        }
        return checkResult(  entryManagementService.searchEntry(entryEntity, entryState,pageIndex, pageSize));
    }

    //新增词条
    @PostMapping("/getThesaurus")
    @ApiOperation("查询词库")
    @CrossOrigin
    @PassToken
    @Transactional
    public HttpResponse<ResponseListModel> getThesaurus() {
        ResponseListModel result = new ResponseListModel<>();
         List<Thesaurus> thesaurus = entryManagementService.getThesaurus();
        result.setList(thesaurus);
        result.setTotalNum(thesaurus.size());
        return checkResult(result);
    }

    //新增词条
    @PostMapping("/insertEntry")
    @ApiOperation("新增词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<EntryEntity> insertEntry(@RequestBody EntryEntity entryEntity,HttpServletRequest request) {
        if (StringUtils.isBlank(entryEntity.getTableName())) {

            return checkResult(null,ErrorCodeList.TBALE_IS_NULL);
        }
        return entryManagementService.insertEntry(entryEntity,request);
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
    @PassToken
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
    public HttpResponse<String> importEntry(MultipartFile file) {
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
    @PassToken
    @Transactional
    public HttpResponse<ResponseListModel> getEntryClassfy( ) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryClassify> entryClassifies = new ArrayList<>();
        entryClassifies = entryManagementService.getEntryClassfy();
        responseListModel.setList(entryClassifies);
        responseListModel.setTotalNum(entryClassifies.size());
        return checkResult(responseListModel);

    }


    @PostMapping("/updateEntryClassfy")
    @ApiOperation("词条分类修改")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateEntryClassfy(EntryClassify entryClassify) {


        return checkResult(entryManagementService.updateEntryClassfy(entryClassify));

    }

    @PostMapping("/deleteEntryClassfy")
    @ApiOperation("词条分类删除")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteEntryClassfy(@RequestBody List<String>  idList) {

        return checkResult(entryManagementService.deleteEntryClassfy(idList));

    }


    @PostMapping("/addEntryClassfy")
    @ApiOperation("词条分类新增")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> addEntryClassfy(EntryClassify  entryClassify) {


        return checkResult(entryManagementService.addEntryClassfy(entryClassify));

    }


    //新增词条
    @PostMapping("/deleteEntry")
    @ApiOperation("删除词条")
    @CrossOrigin
    public HttpResponse<String> deleteEntry(@RequestBody List<String> idList,String tableName) {
        if (CollectionUtils.isEmpty(idList) || StringUtils.isBlank(tableName)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }


        return checkResult(entryManagementService.deleteEntry(idList,tableName));

    }


    //批量审核
    @PostMapping("/bathAudit")
    @ApiOperation("批量审核")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> bathAudit(@RequestBody List<EntryGroupEntity> entryGroupEntities, int state, HttpServletRequest request,String note) {


        if (CollectionUtils.isEmpty(entryGroupEntities)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        String result = entryManagementService.bathAudit(entryGroupEntities,state,request,note);

        return checkResult(result);
    }


    @PostMapping("/getOperateByEntryId")
    @ApiOperation("操作记录查询")
    @PassToken
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> queryOperate(@RequestBody EntryOperate  entryOperate) {

        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryOperate> operate = entryManagementService.queryOperate(entryOperate);
        responseListModel.setList(operate);
        responseListModel.setTotalNum(operate.size());

        return checkResult(responseListModel);
    }



    @PostMapping("/translate")
    @ApiOperation("翻译词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<TranslateEntity> translate(EntryEntity entryEntity,String souce) {


        TranslateEntity translateEntity = entryManagementService.translate(entryEntity);
        return checkResult(translateEntity);
    }

    @PostMapping("/queryLabel")
    @ApiOperation("标签查询")
    @PassToken
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> queryLabel() {
        ResponseListModel responseListModel = new ResponseListModel();

        List<EntryLabel> entryLabels = entryManagementService.queryLabel();
        responseListModel.setList(entryLabels);
        responseListModel.setTotalNum(entryLabels.size());
        return checkResult(responseListModel);
    }



}
