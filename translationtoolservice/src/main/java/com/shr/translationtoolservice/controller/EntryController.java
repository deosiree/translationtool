package com.shr.translationtoolservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.*;

import com.shr.translationtoolservice.util.*;

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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/entry")
@Api(tags = "词条管理")
@Slf4j
public class EntryController extends BaseController {
    @Autowired
    private EntryManagementService entryManagementService;

    @Autowired
    private TLanguageMapper tLanguageMapper;

    @Autowired
    private VersionTableMapper versionTableMapper;

    @Autowired
    private VersionTableService versionTableService;

    //查询词条信息
    @PostMapping("/searchEntry")
    @ApiOperation("词条查询")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel> searchEntry(@RequestBody EntryCommonEntity entryEntity,
                                                       String entryState,
                                                       @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                       @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel result = new ResponseListModel<>();
       /* if (StringUtils.isBlank(entryEntity.getTableName())){
            return checkResult(result);
        }*/
        return checkResult(  entryManagementService.searchEntry(entryEntity, entryState,pageIndex, pageSize));
    }

    //查询词库
    @PostMapping("/getThesaurus")
    @ApiOperation("查询词库")
    @CrossOrigin
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
    @Token
    public HttpResponse<EntryCommonEntity> insertEntry(@RequestBody EntryCommonEntity entryEntity,HttpServletRequest request) {
      /*  if (StringUtils.isBlank(entryEntity.getTableName())) {

            return checkResult(null,ErrorCodeList.TBALE_IS_NULL);
        }*/
        String insertType = ConstantInterface.OPERATION_TYPE_INSERT;
        return entryManagementService.insertEntry(entryEntity,request,insertType);
    }

    //编辑词条
    @PostMapping("/updateEntry")
    @ApiOperation("编辑词条")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<EntryCommonEntity> updateEntry(@RequestBody EntryCommonEntity entryEntity,HttpServletRequest request,String notes) {
    /*    if (StringUtils.isBlank(entryEntity.getTableName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }*/
         ResultObject resultObject = entryManagementService.updateEntry(entryEntity, request,notes);
        EntryCommonEntity entryEntity1 = (EntryCommonEntity)resultObject.getData();


        return checkResult(entryEntity1,resultObject.getMsg());
    }

    @PostMapping("/entryMerge")
    @ApiOperation("词条合并")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> entryMerge(@RequestBody List<EntryEntity> entryEntities) {

        return checkResult( entryManagementService.entryMerge(entryEntities));
    }

    @PostMapping("/getEntryNoMerge")
    @ApiOperation("未合并词条查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryMerge(String chinese) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryCommonEntity> entryEntities = new ArrayList<>();
        entryEntities = entryManagementService.selectNoMergeEntry(chinese);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/getEntryMerge")
    @ApiOperation("已合并词条查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryNoMerge(String chinese) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryCommonEntity> entryEntities = new ArrayList<>();
        entryEntities = entryManagementService.selectMergeEntry(chinese);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/importEntry")
    @ApiOperation("导入")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> importEntry(MultipartFile file) {
        ResponseListModel responseListModel = new ResponseListModel();

        return checkResult(null);
    }

    @PostMapping("/outEntry")
    @ApiOperation("导出")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> outEntry(List<String>  entryIds) {
        ResponseListModel responseListModel = new ResponseListModel();

        return checkResult(null);

    }

    @PostMapping("/getEntryClassfy")
    @ApiOperation("词条分类查询")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryClassfy( String department , HttpServletRequest request) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryClassify> entryClassifies = new ArrayList<>();

        //department 空 为管理员，可查看所有分类
        entryClassifies = entryManagementService.getEntryClassfy(department);
        responseListModel.setList(entryClassifies);
        responseListModel.setTotalNum(entryClassifies.size());
        return checkResult(responseListModel);

    }


    @PostMapping("/updateEntryClassfy")
    @ApiOperation("词条分类修改")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> updateEntryClassfy(EntryClassify entryClassify) {


        return checkResult(entryManagementService.updateEntryClassfy(entryClassify));

    }

    @PostMapping("/deleteEntryClassfy")
    @ApiOperation("词条分类删除")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> deleteEntryClassfy(@RequestBody List<String>  idList) {

        return checkResult(entryManagementService.deleteEntryClassfy(idList));

    }


    @PostMapping("/addEntryClassfy")
    @ApiOperation("词条分类新增")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> addEntryClassfy(EntryClassify  entryClassify,HttpServletRequest request) {


        return checkResult(entryManagementService.addEntryClassfy(entryClassify,request));

    }


    //删除词条
    @PostMapping("/deleteEntry")
    @ApiOperation("删除词条")
    @CrossOrigin
    @Token
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
    @Token
    public HttpResponse<String> bathAudit(@RequestBody List<EntryGroupEntity> entryGroupEntities, int state, HttpServletRequest request,String note) {


        if (CollectionUtils.isEmpty(entryGroupEntities)) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        String result = entryManagementService.bathAudit(entryGroupEntities,state,request,note);

        return checkResult(result);
    }


    @PostMapping("/getOperateByEntryId")
    @ApiOperation("操作记录查询")
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
    @Transactional
    public HttpResponse<TranslateEntities> translate(String name,String type) {
        if (StringUtils.isBlank(type)){
            checkResult(null," 入参 type 不能为空 ！");
        }
        TranslateEntities translateEntity = entryManagementService.translate(name,type);
        return checkResult(translateEntity);
    }

    @PostMapping("/queryLabel")
    @ApiOperation("标签查询")
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
    @Token
    public HttpResponse<String> deleteLabel(@RequestBody List<String> idList) {

        return checkResult(entryManagementService.deleteLabel(idList));
    }

    @PostMapping("/addLabel")
    @ApiOperation("标签新增")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> addLabel(@RequestBody EntryLabel entryLabel) {

        return checkResult(entryManagementService.addLabel(entryLabel));
    }

    @PostMapping("/updateLabel")
    @ApiOperation("标签更新")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> updateLabel(@RequestBody EntryLabel entryLabel) {

        return checkResult(entryManagementService.updateLabel(entryLabel));
    }

    @PostMapping("/mergerSplit")
    @ApiOperation("合并拆分")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> mergerSplit(@RequestBody List<String> idList) {

        return checkResult( entryManagementService.mergerSplit(idList));

    }


    @PostMapping("/getEntryProperty")
    @ApiOperation("词性查询")
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
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> importExcle(@RequestBody MultipartFile multipartFile) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryCommonEntity> entryEntities = entryManagementService.importExcle(multipartFile);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());

        return checkResult(responseListModel);
    }

    @PostMapping("/bachAddEntry")
    @ApiOperation("批量插入词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> bachAddEntry(@RequestBody List<EntryCommonEntity> entryEntities) {

        return checkResult( entryManagementService.bachAddEntry(entryEntities));
    }


    @PostMapping("/createVersionTable")
    @ApiOperation("生成版本库")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> createVersionTable(@RequestBody List<EntryCommonEntity> entryEntities,
                                                   String department,HttpServletRequest request,
                                                   String version,String remark) {
        String versionTable ="";

        try {
              versionTable = entryManagementService.createVersionTable(entryEntities, version, remark,department,request);
        }catch (ExceptionUtils e){
           return checkResult(null,e.getMessage());
        }
        return checkResult(versionTable );
    }

    @PostMapping("/getVersionTableInfo")
    @ApiOperation("查看版本关系库信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getVersionTableInfo() {
        ResponseListModel responseListModel = new ResponseListModel();
        List<VersionTable> versionInfoByVersion = versionTableMapper.getVersionInfoByVersion("");
        responseListModel.setList(versionInfoByVersion);
        return checkResult(responseListModel );
    }

  /*  @PostMapping("/getVersionTableByCondition")
    @ApiOperation("查看版本库信息(条件查询)")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getVersionTableByCondition(@RequestBody VersionTable versionTable,
                                                                      @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize){
        return checkResult(versionTableService.getVersionTableByCondition(versionTable,pageIndex,pageSize) );
    }*/

/*    @PostMapping("/batchDeleteVersionTable")
    @ApiOperation("批量删除版本库")
    @CrossOrigin
    public HttpResponse<String> batchDeleteVersionTable(@RequestBody List<String> ids){
        if (ids.size() == 0){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        return checkResult(versionTableService.batchDeleteVersionTable(ids));
    }*/

    @PostMapping("/getEntryToVersion")
    @ApiOperation("查看词条（模糊）")
    @CrossOrigin
    @Transactional
    public HttpResponse<EntryResponse> getEntryToVersion(String version,
                                                   @RequestBody List<String> classfy,String tag,String creator) {

        EntryResponse entryResponse = entryManagementService.getEntryToVersion(version, classfy, tag, creator);

        return checkResult(entryResponse);
    }


    @PostMapping("/getVersionTable")
    @ApiOperation("查看版本库")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getVersionTable(String version, String department,
                                                           @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                           @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<VersionTable> versionInfoByVersion = versionTableMapper.getVersionInfoByVersion(version);
        if (CollectionUtils.isEmpty(versionInfoByVersion)){
            log.info( " ==== 不存在version 为 " + version + " 的版本库 ！ ==== ");
            return checkResult(null," 不存在version 为 " + version + " 的版本库 ！");
        }
        String tableName = versionInfoByVersion.get(0).getVersionTableName();
        responseListModel.setList( entryManagementService.getVersionTable(tableName,version,pageIndex,pageSize,department));
        responseListModel.setTotalNum(versionTableMapper.getVersionTableTotal(tableName,version));
        return checkResult(responseListModel);
    }


    @PostMapping("/getLanguage")
    @ApiOperation("查看语言代码")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getLanguage() {


        ResponseListModel responseListModel = new ResponseListModel();
        List<TLanguage> tLanguages = tLanguageMapper.selectList(new QueryWrapper<>());

        responseListModel.setList( tLanguages);
        responseListModel.setTotalNum(tLanguages.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getTranslatedEntry")
    @ApiOperation("查看已翻译词条")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getTranslatedEntry(@RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                               @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryCommonEntity> entryCommonEntities = entryManagementService.getTranslatedEntry(pageIndex,pageSize);

        responseListModel.setList( entryCommonEntities);
        responseListModel.setTotalNum(entryCommonEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/upgradeEntry")
    @ApiOperation("词条升级")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<HttpResponse<EntryCommonEntity> > upgradeEntry( @RequestBody EntryCommonEntity entryEntity,HttpServletRequest request) {
        //实体ID 是旧词条id

        String insertType = ConstantInterface.OPERATION_TYPE_UPGRATE;
        return checkResult( entryManagementService.upgradeEntry(entryEntity,request,insertType));
    }

    @PostMapping("/getKindEntryVersion")
    @ApiOperation("查询同种词条版本")
    @CrossOrigin
    public HttpResponse<ResponseListModel>  getKindEntryVersion( String typeID) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<String> entryCommonEntities = entryManagementService.getKindEntryVersion(typeID);

        responseListModel.setList( entryCommonEntities);
        responseListModel.setTotalNum(entryCommonEntities.size());
        return checkResult(responseListModel);

    }



}
