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
import com.shr.translationtoolservice.util.RedisUtil;

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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
       /* if (StringUtils.isBlank(entryEntity.getTableName())){
            return checkResult(result);
        }*/
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
      /*  if (StringUtils.isBlank(entryEntity.getTableName())) {

            return checkResult(null,ErrorCodeList.TBALE_IS_NULL);
        }*/
        return entryManagementService.insertEntry(entryEntity,request);
    }

    //新增词条
    @PostMapping("/updateEntry")
    @ApiOperation("编辑词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<EntryEntity> updateEntry(@RequestBody EntryEntity entryEntity,HttpServletRequest request) {
    /*    if (StringUtils.isBlank(entryEntity.getTableName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }*/
         ResultObject resultObject = entryManagementService.updateEntry(entryEntity, request);
        EntryEntity entryEntity1 = (EntryEntity)resultObject.getData();


        return checkResult(entryEntity1,resultObject.getMsg());
    }

    @PostMapping("/entryMerge")
    @ApiOperation("词条合并")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> entryMerge(@RequestBody List<EntryEntity> entryEntities,HttpServletRequest request) {

        return checkResult( entryManagementService.entryMerge(entryEntities));
    }

    @PostMapping("/getEntryNoMerge")
    @ApiOperation("未合并词条查询")
    @CrossOrigin
    @PassToken
    @Transactional
    public HttpResponse<ResponseListModel> getEntryMerge(String entry) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryEntity> entryEntities = new ArrayList<>();
        entryEntities = entryManagementService.selectNoMergeEntry(entry);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/getEntryMerge")
    @ApiOperation("已合并词条查询")
    @CrossOrigin
    @PassToken
    @Transactional
    public HttpResponse<ResponseListModel> getEntryNoMerge(String entry) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryEntity> entryEntities = new ArrayList<>();
        entryEntities = entryManagementService.selectMergeEntry(entry);
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
    public HttpResponse<String> deleteEntry(@RequestBody List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(entryManagementService.deleteEntry(idList));

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


    @Autowired
    RedisUtil redisUtil ;

    @PostMapping("/translate")
    @ApiOperation("翻译词条")
    @CrossOrigin
    @PassToken
    @Transactional
    public HttpResponse<TranslateEntities> translate(String name) {
        TranslateEntities translateEntity = entryManagementService.translate(name);
        return checkResult(translateEntity);
    }

    @PostMapping("/queryLabel")
    @ApiOperation("标签查询")
    @PassToken
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> queryLabel( @RequestBody EntryLabel entryLabel ,@RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel responseListModel = new ResponseListModel();

        return checkResult( entryManagementService.queryLabel(entryLabel,pageIndex,pageSize));
    }

    @PostMapping("/deleteLabel")
    @ApiOperation("标签删除")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteLabel(@RequestBody List<String> idList) {

        return checkResult(entryManagementService.deleteLabel(idList));
    }

    @PostMapping("/addLabel")
    @ApiOperation("标签新增")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> addLabel(@RequestBody EntryLabel entryLabel) {

        return checkResult(entryManagementService.addLabel(entryLabel));
    }

    @PostMapping("/updateLabel")
    @ApiOperation("标签更新")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateLabel(@RequestBody EntryLabel entryLabel) {

        return checkResult(entryManagementService.updateLabel(entryLabel));
    }

    @PostMapping("/mergerSplit")
    @ApiOperation("合并拆分")
    @CrossOrigin
    @PassToken
    @Transactional
    public HttpResponse<String> mergerSplit(@RequestBody List<String> idList) {

        return checkResult( entryManagementService.mergerSplit(idList));

    }


    @PostMapping("/getEntryProperty")
    @ApiOperation("词性查询")
    @PassToken
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryProperty(@RequestBody EntryProperty entryProperty) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryProperty> entryProperties = entryManagementService.queryEntryProperty(entryProperty);
        responseListModel.setList(entryProperties);
        responseListModel.setTotalNum(entryProperties.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/importExcle")
    @ApiOperation("导入excle")
    @PassToken
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> importExcle(@RequestBody MultipartFile multipartFile) {
        ResponseListModel responseListModel = new ResponseListModel();
         entryManagementService.importExcle(multipartFile);

        return checkResult(responseListModel);
    }



}
