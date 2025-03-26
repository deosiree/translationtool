package com.shr.translationtoolservice.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.service.*;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.ExcelUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;

import io.github.resilience4j.core.lang.Nullable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @ClassName EntryInfoController
 * @USER: Cola
 * @Date 2023/11/20 0020 11:26
 **/
@RestController
@RequestMapping("/entryInfo")
@Api(tags = "词条管理（新）")
@Slf4j
public class EntryInfoController extends BaseController {


    @Autowired
    private EntryPublicService entryPublicService;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private ProductService productService;
    @Autowired
    private EntryClassifyService entryClassifyService;
    @Autowired
    private EntryInfoService entryInfoService;

    @Autowired
    private TranslateService translateService;

    private ThreadPoolExecutor threadPoolExecutor =  new ThreadPoolExecutor(5, 20, 120, TimeUnit.SECONDS, new LinkedBlockingDeque<>());


    //查询词条信息
    @PostMapping("/getPublicEntry")
    @ApiOperation("查询公共库")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel<TranslateEntity>> getPublicEntry(@RequestBody TranslateEntity translateEntity,
                                                                           @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                           @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TranslateEntity> result = new ResponseListModel<>();
        List<TranslateEntity> entryPublicEntities = new ArrayList<>();

        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryPublicEntities = entryInfoService.getPublicEntry(translateEntity, offset, pageSize);
        }
        result.setList(entryPublicEntities);
        result.setTotalNum(entryInfoService.getPublicEntryTotal(translateEntity));
        return checkResult(result);
    }


    @PostMapping("/updatePublicEntry")
    @ApiOperation("修改公共库")
    @Token
    @CrossOrigin
    public HttpResponse<String> updatePublicEntry(@RequestBody TranslateEntity translateEntity) {


        String result = entryInfoService.updatePublicEntry(translateEntity);

        return checkResult(result);
    }

    @PostMapping("/addPublicEntry")
    @ApiOperation("新增公共库")
    @Token
    @CrossOrigin
    public HttpResponse<String> addPublicEntry(@RequestBody List<TranslateEntity> translateEntity) {


        String result = entryInfoService.addPublicEntry(translateEntity);

        return checkResult(result);
    }

    @PostMapping("/deletePublicEntry")
    @ApiOperation("删除公共库")
    @Token
    @CrossOrigin
    public HttpResponse<String> deletePublicEntry(@RequestBody List<String> idlist) {


        String result = entryInfoService.deletePublicEntry(idlist);

        return checkResult(result);
    }

    @PostMapping("/getClassTree")
    @ApiOperation("查询分类树")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel> getClassTree(String department, String className, HttpServletRequest request) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryClassify> entryClassifies = new ArrayList<>();

        //department 空 为管理员，可查看所有分类
        entryClassifies = entryClassifyService.getEntryClassfy(department, className, request);
        responseListModel.setList(entryClassifies);
        responseListModel.setTotalNum(entryClassifies.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/updateEntryClassfy")
    @ApiOperation("词条分类修改")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> updateEntryClassfy(@RequestBody EntryClassify entryClassify) {


        return checkResult(entryClassifyService.updateEntryClassfy(entryClassify));

    }

    @PostMapping("/deleteEntryClassfy")
    @ApiOperation("词条分类删除")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> deleteEntryClassfy(@RequestBody List<String> idList) {

        return checkResult(entryClassifyService.deleteEntryClassfy(idList));

    }


    @PostMapping("/addEntryClassfy")
    @ApiOperation("词条分类新增")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> addEntryClassfy(@RequestBody EntryClassify entryClassify, HttpServletRequest request) {


        return checkResult(entryClassifyService.addEntryClassfy(entryClassify, request));

    }


    @PostMapping("/getEntryByVersion")
    @ApiOperation("获取产品版本词条")
    @CrossOrigin
    @Token
    public HttpResponse<ResponseListModel<EntryInfoEntity>> getEntryByVersion(@RequestBody EntryInfoEntity entryInfoEntity,
                                                                              @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                              @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<EntryInfoEntity>();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();

        entryInfoEntities = entryInfoService.getEntryByVersion(entryInfoEntity, pageIndex, pageSize);

        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoService.getEntryByVersionTotal(entryInfoEntity));
        return checkResult(responseListModel);
    }

    @PostMapping("/getEntryByClassfy")
    @ApiOperation("获取分类词条")
    @CrossOrigin
    @Token
    public HttpResponse<ResponseListModel<EntryInfoEntity>> getEntryByClassfy(@RequestBody EntryInfoEntity entryInfoEntity,
                                                                              @RequestParam(value = "classfyID") String classfyID,
                                                                                 String translateType,
                                                                              @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                              @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<EntryInfoEntity>();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();

        entryInfoEntities = entryInfoService.getEntryByClassfy(entryInfoEntity,classfyID, pageIndex, pageSize,translateType);



        responseListModel.setList(entryInfoEntities);
        //全选跳过汇总
        if (pageIndex != -1 && pageSize != -1) {
            responseListModel.setTotalNum(entryInfoService.getEntryByClassfTotal(entryInfoEntity,classfyID,translateType));
        }else {
            responseListModel.setTotalNum(entryInfoEntities.size());
        }

        return checkResult(responseListModel);
    }

    @PostMapping("/addEntryByVersion")
    @ApiOperation("新增版本词条(导入)")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<HttpResponse<String>> addEntryByVersion(@RequestBody List<EntryVO> entryVOS,
                                                                HttpServletRequest request) {

        return checkResult(entryInfoService.addEntryByVersion(entryVOS, request));
    }


    @PostMapping("/entryImportExcle")
    @ApiOperation("词条excle导入(更新翻译)")
    @CrossOrigin
    @Transactional
    //new
    public HttpResponse<String> entryImportExcle(@RequestParam("file") MultipartFile multipartFile,@RequestParam("transType") String transType,
                                                          HttpServletRequest httpServletRequest
    ) {
        String re = entryInfoService.importTransExcle(multipartFile, httpServletRequest,transType);

        return checkResult(re);
    }

    @PostMapping("/workImportExcleTrans")
    @ApiOperation("词条excle导入(工作台更新翻译)")
    @CrossOrigin
    @Transactional
    //new
    public HttpResponse<ResponseListModel<EntryInfoEntity>> workImportExcleTrans(@RequestParam("file") MultipartFile multipartFile,@RequestParam("taskID") String taskID
    ) {

        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<EntryInfoEntity>();
        //entryInfoEntities 中存在的词条 更新翻译
        List<EntryInfoEntity> entryInfoEntities;
        entryInfoEntities = entryInfoService.workImportExcleTrans(multipartFile,taskID);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/addEntryAudit")
    @ApiOperation("词条送审")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> addEntryAudit(@RequestBody List<EntryInfoEntity> entryInfoEntities,
                                              @RequestParam String taskID,
                                              HttpServletRequest request) {

        return checkResult(entryInfoService.addEntryAudit(entryInfoEntities, taskID, request));
    }


    @PostMapping("/createVersionByEntry")
    @ApiOperation("词条生成版本")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> createVersionByEntry(@RequestBody List<EntryInfoEntity> entryInfoEntities,
                                                     @RequestParam String productID,
                                                     @RequestParam String common,
                                                     @RequestParam String versionName,
                                                     HttpServletRequest request) {

        return checkResult(entryInfoService.createVersionByEntry(entryInfoEntities, productID, common,versionName,request));
    }

    @PostMapping("/addEntryInfo")
    @ApiOperation("新增词条(单条无翻译)")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> addEntryInfo(@RequestBody EntryInfoEntity entryInfoEntity,
                                             String tableName,
                                             HttpServletRequest request) {
        //tableName = "t_version_202311";
        return checkResult(entryInfoService.addEntryInfo(entryInfoEntity, request, tableName));
    }

    @PostMapping("/addSingleEntry")
    @ApiOperation("新增单条词条")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<EntryInfoEntity> addSingleEntry(@RequestBody EntryInfoEntity entryInfoEntity,
                                               HttpServletRequest request) {
        //tableName = "t_version_202311";
        return checkResult(entryInfoService.addSingleEntry(entryInfoEntity, request));
    }

    @PostMapping("/updateEntryTemp")
    @ApiOperation("临时表更新")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> updateEntryTemp(@RequestBody List<EntryTempEntity> entryTempEntities,
                                                HttpServletRequest request) {
        //tableName = "t_version_202311";
        return checkResult(entryInfoService.updateEntryTemp(entryTempEntities, request));
    }

    //编辑词条
    @PostMapping("/updateEntryInfo")
    @ApiOperation("编辑词条")
    @CrossOrigin
    @Transactional(propagation = Propagation.NESTED)
    @Token
    public HttpResponse<String> updateEntryInfo(@RequestBody EntryInfoEntity entryInfoEntity, HttpServletRequest request, String notes) {

        String result = entryInfoService.updateEntryInfo(entryInfoEntity, request, notes);

        return checkResult(result);
    }


    //编辑词条
    @PostMapping("/updateEntryInfoList")
    @ApiOperation("批量更新词条")
    @CrossOrigin
    @Transactional(propagation = Propagation.NESTED)
    @Token
    public HttpResponse<String> updateEntryInfoList(@RequestBody List<EntryInfoEntity> entryInfoEntities, HttpServletRequest request, String notes) {

        String result = entryInfoService.updateEntryInfoList(entryInfoEntities, request, notes);

        return checkResult(result);
    }

    //编辑词条
    @PostMapping("/deleteEntryInfo")
    @ApiOperation("删除词条")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> deleteEntryInfo(@RequestBody List<String> idList, String tableName) {

        String result = entryInfoService.deleteEntryInfo(idList, tableName);

        return checkResult(result);
    }


    //编辑词条
    @PostMapping("/upgrade")
    @ApiOperation("升级（废弃）")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> upgrade(@RequestBody UpgradeVO upgradeVO, HttpServletRequest request) {

        String result = entryInfoService.upgrade(upgradeVO, request);

        return checkResult(result);
    }

    //编辑翻译
    @PostMapping("/updateTranslation")
    @ApiOperation("编辑翻译")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> updateTranslation(@RequestBody List<TranslateEntity> translateEntityList) {

        String result = translateService.updateTranslation(translateEntityList);

        return checkResult(result);
    }


    @PostMapping("/translate")
    @ApiOperation("翻译词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<TranslateEntities> translate(@RequestParam String name, @RequestParam String type, @RequestParam String department) {
        if (StringUtils.isBlank(type)) {
            checkResult(null, " 入参 type 不能为空 ！");
        }

        TranslateEntities translateEntity = entryInfoService.translate(name, type, department);
        return checkResult(translateEntity);
    }

    @PostMapping("/versionExport")
    @ApiOperation("版本导出")
    @CrossOrigin
    @Transactional
    public void versionExport(@RequestParam String versionID,
                              @RequestParam String translateType,
                              HttpServletResponse response) {
        entryInfoService.versionExport(versionID, response, translateType);
    }

    @PostMapping("/entryExportByCondition")
    @ApiOperation("导出词条（版本，任务，产品）")
    @CrossOrigin
    @Transactional
    public void entryExportByCondition(@RequestBody ExcelExportVO excelExportVO,
                                       HttpServletResponse response,  String taskID) {
        entryInfoService.   entryExportByCondition(excelExportVO,response,taskID);
    }

    @PostMapping("/getClassfy")
    @ApiOperation("分类查询")
    @CrossOrigin
    @Transactional
    public   HttpResponse<ResponseListModel<EntryClassify> > getClassfy(@RequestParam String parentId,@RequestParam String type) {
        ResponseListModel<EntryClassify> responseListModel = new ResponseListModel<>();
        List<EntryClassify> entryClassifies = entryInfoService.getClassfy(parentId,type);
        responseListModel.setList(entryClassifies);
        responseListModel.setTotalNum(entryClassifies.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/addProductRelation")
    @ApiOperation("关系表新增")
    @CrossOrigin
    @Transactional
    public   HttpResponse<String> addProductRelation(@RequestBody List<EntryInfoEntity> relationEntity ,HttpServletRequest request) {
        String relation = entryInfoService.addProductRelation(relationEntity,request);

        return checkResult(relation);
    }


    private Map<String,String> cacheForCheckNotUseEntry = new LinkedHashMap<>();


    @PostMapping("/checkNotUseEntry")
    @ApiOperation("校验未使用的词条")
    @CrossOrigin
    @Transactional
    public   HttpResponse checkNotUseEntry(@RequestParam String i18nUrl ,@RequestParam(required = false) String classfyID,
        HttpServletRequest request,
        @RequestParam Integer command,@RequestParam(required = false) String taskId,
        @RequestBody(required = false) CheckNotUseEntryVO params,
        @RequestParam(required = false,defaultValue = "-1") Integer from,@RequestParam(required = false,defaultValue = "-1") Integer offset) {

        
        if(command == 0){
            Callable<Boolean> callable = new Callable<Boolean>() {
                
                @Override
                public Boolean call() throws Exception {
                    // TODO Auto-generated method stub
                    Map<String, String> condition = params != null ? params.getParams() : null;
                    Map<String, List<String>> sources = params != null ? params.getSources() : null;
                    List<EntryInfoEntity> entities;
                    try {
        
                        if(condition != null && classfyID != null){
                            entities = entryInfoService.checkNotUseEntry(i18nUrl, classfyID, request,sources, condition);
                        }else if(condition == null && classfyID != null){
                            entities = entryInfoService.checkNotUseEntry(i18nUrl,classfyID,request,sources);
                        }else if(condition != null && classfyID == null){
                            entities = entryInfoService.checkNotUseEntry(i18nUrl, request, sources, condition);
                        }else{
                            entities = entryInfoService.checkNotUseEntry(i18nUrl, request, sources);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        return false;
                    }
                    /*
                    * 先写数据库，后写缓存
                    */
                    try {
                        Gson gson = new Gson();
                        BufferedWriter writer = null;
                        String resultText = gson.toJson(entities);
                        try {
                            String filePath = taskId;
                            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath),false),StandardCharsets.UTF_8));
                            writer.write(resultText);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }finally{
                            try {
                                writer.close();
                            } catch (Exception e) {
                                // TODO: handle exception
                                e.printStackTrace();
                                return false;
                            }
                        }
                        cacheForCheckNotUseEntry.put(taskId, resultText);    // 数据库写入成功，缓存生效
                        return true;                        
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                        return false;
                    }

                }
                
                
            };
            try {
                threadPoolExecutor.submit(callable); 
                return ok("已经提交任务，正在执行中");
            } catch (Exception e) {
                // TODO: handle exception
                // 没有放入线程池
                return error(null, "无法执行该任务，服务异常: " + e.getMessage());
            }
        }else{
            // 读结果
            if(cacheForCheckNotUseEntry.containsKey(taskId)){
                String result = cacheForCheckNotUseEntry.get(taskId);
                return ok(result);  // 已经写到缓存里面了
            }else{
                String filePath = taskId;
                File file = new File(filePath);
                if(!file.exists()){
                    return error(null, "服务异常，未查询到该任务"); // 缓存清理了，但结果可能持久化到存储里面了，尝试查询，没有，代表异常连文件都没写入
                }
                // 可能存在还没有写完的情况
                BufferedReader reader = null;
                try {
                    try {
                        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        // 可能在写文件，没有读取的权限
                        String result = cacheForCheckNotUseEntry.get(taskId);
                        if(result != null){
                            return ok(result);  // 尝试读缓存， 如果写进去了，就用这个
                        }
                        return error(null, "服务正在执行，请稍后再试");
                    }
                    String result = cacheForCheckNotUseEntry.get(taskId);   
                    if(result != null){
                        return ok(result);     // 文件写进去了，缓存有写好了，从缓存读
                    }   
                    // 文件写进去了，但还没来得及写缓存
                    String nextLine;
                    StringBuilder builder = new StringBuilder();
                    while ((nextLine = reader.readLine()) != null) {
                        builder.append(nextLine);
                    }
                    result = cacheForCheckNotUseEntry.get(taskId);
                    if(result != null){
                        return ok(cacheForCheckNotUseEntry.get(taskId));    // double-check，看是否已经写入缓存了
                    }
                    // 如果还没写到缓存，就直接从文件里读取
                    return ok(builder.toString());
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    return error(null, "服务异常，提取任务结果的过程失败"); 
                } finally{
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    @PostMapping("/checkNewEntryByClassfy")
    @ApiOperation("查询分类中新增的词条")
    @CrossOrigin
    @Transactional
    public   HttpResponse<ResponseListModel> checkNewEntryByClassfy(@RequestParam String i18nUrl ,@RequestParam String classfyID,HttpServletRequest request) {
        ResponseListModel<TaskEntryVO> responseListModel = new ResponseListModel<>();
        List<TaskEntryVO> taskEntryVOS = entryInfoService.checkNewEntryByClassfy(i18nUrl,classfyID,request);
        if (null == taskEntryVOS){
            return checkResult(null,"i18n 服务异常 ！");
        }
        responseListModel.setList(taskEntryVOS);
        responseListModel.setTotalNum(taskEntryVOS.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/getEntrysourceListByClassfy")
    @ApiOperation("查询分类中新增的词条来源")
    @CrossOrigin
    @Transactional
    public   HttpResponse< ResponseListModel<SourceEntryVO>> getEntrysourceListByClassfy(@RequestParam String classfyID,@RequestParam String i18nUrl,HttpServletRequest request) {
        ResponseListModel<SourceEntryVO> responseListModel = new ResponseListModel<>();
        List<SourceEntryVO> sourceEntryVOS = entryInfoService.getEntrysourceListByClassfy(classfyID, i18nUrl,request);
        if (null == sourceEntryVOS){
            return checkResult(null,"i18n 服务异常 ！");
        }
        responseListModel.setList(sourceEntryVOS);
        responseListModel.setTotalNum(sourceEntryVOS.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/updateEntryByClassfy")
    @ApiOperation("更新来源中新增的词条")
    @CrossOrigin
    @Transactional
    public   HttpResponse< ResponseListModel<String>> updateEntryByClassfy(@RequestBody List<SourceEntryVO> sourceEntryVOS,HttpServletRequest request ) {
        ResponseListModel<String> responseListModel = new ResponseListModel<>();
        List<String> result  = entryInfoService.updateEntryByEntrySource(sourceEntryVOS,request);
        if (null == result){
            return checkResult(null,"i18n 服务异常 ！");
        }
        responseListModel.setList(result);
        responseListModel.setTotalNum(result.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/doWeight")
    @ApiOperation("去除重复的词条")
    @CrossOrigin
    @Transactional
    public   void doWeight() {

       entryInfoService.doWeight();

    }




    @PostMapping("/setInfo")
    @ApiOperation("回写")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> setInfo(@RequestBody List<EntryInfoEntity> entryInfoEntities, @RequestParam String translateType,
                                        @RequestParam String writeType,@RequestParam int isTag,
                                        @RequestParam int isComment,   String fileName, @RequestParam  String i18nUrl) {
        boolean tag  =true;
        boolean comment = true;
        if (isTag ==0){
            tag = false;
        }
        if (isComment == 0 ){
            comment = false;
        }

        return checkResult(entryInfoService.setInfoByEntryList(entryInfoEntities,translateType,writeType,tag,comment,fileName,i18nUrl));
    }

    @Autowired
    TranslateMapper translateMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    EntryInfoMapper entryInfoMapper;
    //编辑词条
    @PostMapping("/test")
    @ApiOperation("恢复匹配错部门的数据")
    @CrossOrigin
    @Transactional(propagation = Propagation.NESTED)
    @Token
    public HttpResponse<String> test() {

        List<EntryInfoEntity> entities = entryInfoMapper.getbppEntrys();
        for (EntryInfoEntity e : entities) {
            User user = userMapper.selectByName(e.getUpdate());

            String enTransId = e.getEnTransId();
            if (StringUtils.isNotBlank(enTransId)) {
                TranslateEntity translateEntity = translateMapper.selectById(enTransId);
                if (translateEntity != null) {
                    QueryWrapper<TranslateEntity> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("entry", translateEntity.getEntry());
                    queryWrapper.eq("visual_range",user.getDepartment() );
                    queryWrapper.eq("translate_state","3" );
                    queryWrapper.eq("delete_state","0" );
                    List<TranslateEntity> translateEntities = translateMapper.selectList(queryWrapper);
                    if (translateEntities.size() > 0) {
                        e.setEnTransId(translateEntities.get(0).getId());
                    }else {
                        e.setEnTransId("");
                    }
                    entryInfoMapper.updateById(e);
                }
            }
        }


        return checkResult("result");
    }

}
