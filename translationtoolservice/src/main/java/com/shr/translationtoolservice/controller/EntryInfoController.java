package com.shr.translationtoolservice.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.service.*;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.ExcelUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.LocalTimeUtils;

import io.github.resilience4j.core.lang.Nullable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.formula.functions.T;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

        // return checkResult(entryClassifyService.deleteEntryClassfy(idList));
        try {
            String result = entryClassifyService.deleteEntryClassfy(idList);   
            if(result.equals(ErrorCodeList.UPDATE_ERROR)){
                return ok("删除失败,该词条分类下的产品存在未完成的任务");
            }
            return ok("删除词条分类成功");
        } catch (Exception e) {
            // TODO: handle exception
            // log.error(e.getMessage(), e);
            return error(null, e.getMessage());
        }

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
    public HttpResponse<ResponseListModel<EntryInfoEntity>> getEntryByClassfy(@RequestBody EntryInfoEntityQO entryInfoEntityTemplate,
                                                                              @RequestParam(value = "classfyID") String classfyID,
                                                                                 String translateType,
                                                                              @RequestParam(value = "accurate[]",required = false) Set<String> totalMatchSet,
                                                                              @RequestParam(value = "startTime",required = false)  String startTime,
                                                                              @RequestParam(value = "endTime",required = false) String endTime,
                                                                              @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                              @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<EntryInfoEntity>();
        List<EntryInfoEntity> originalEntryInfoEntities = new ArrayList<>();
        if(totalMatchSet == null) totalMatchSet = new HashSet<>();
        // entryInfoEntities = entryInfoService.getEntryByClassfy(entryInfoEntity,classfyID, pageIndex, pageSize,translateType);
        originalEntryInfoEntities = entryInfoService.getEntryByClassfy(entryInfoEntityTemplate,totalMatchSet, classfyID,startTime,endTime);
        List<EntryInfoEntity> entryInfoEntities = entryInfoService.filterEntryInfoByTransInfo(originalEntryInfoEntities, entryInfoEntityTemplate, translateType);
        // responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        //全选跳过汇总
        if (pageIndex != -1 && pageSize != -1) {
            // responseListModel.setTotalNum(entryInfoService.getEntryByClassfTotal(entryInfoEntity,classfyID,translateType));
            List<EntryInfoEntity> results = entryInfoService.onPage(entryInfoEntities, pageIndex, pageSize);
            for(EntryInfoEntity result: results){
                if(StringUtils.isBlank(result.getEnTransId())){
                    result.setEnglishTranslateState("0");
                }
                if(StringUtils.isBlank(result.getRuTransId())){
                    result.setRussianTranslateState("0");
                }
                if(StringUtils.isBlank(result.getSpaTransId())){
                    result.setSpanishTranslateState("0");   
                }
                if(StringUtils.isBlank(result.getFraTransId())){
                    result.setFrenchTranslateState("0");
                }
            }
            responseListModel.setList(results);
        }else {
            // responseListModel.setTotalNum(entryInfoEntities.size());
            List<EntryInfoEntity> results = entryInfoEntities;
            responseListModel.setList(results);
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
    public HttpResponse<ResponseListModel<EntryInfoEntity>> entryImportExcle(@RequestParam("file") MultipartFile multipartFile,@RequestParam("transType") String transType,
                                                          HttpServletRequest httpServletRequest
    ) {
        try {
            Collection<EntryInfoEntity> collection = entryInfoService.importTransExcle(multipartFile, httpServletRequest,transType);
            ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
            responseListModel.setList(collection);
            responseListModel.setTotalNum(collection.size());
            HttpResponse<ResponseListModel<EntryInfoEntity>> responseResult = new HttpResponse<>();
            responseResult.setCode(HttpResponse.Type.OK.getVal());
            responseResult.setData(responseListModel);
            if(collection != null && !collection.isEmpty()){
                responseResult.setType(HttpResponse.Type.ERROR);
                responseResult.setMessage("词条翻译更新成功,部分翻译字符数超过设定的最大外文字符数,请检查核对后自行修改");
            }else{
                responseResult.setType(HttpResponse.Type.OK);
                responseResult.setMessage("词条翻译更新成功");
            }

            return responseResult;
        } catch (Exception e) {
            return error(null, e.getMessage());
            // TODO: handle exception
        }
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
    public HttpResponse<EntryInfoEntity> addSingleEntry(@RequestBody EntryInfoEntity entryInfoEntity,HttpServletRequest request) {
                        
        String department = JWTTokenUtils.getDepartment(request.getHeader("token"));

        boolean isLegal = entryInfoService.checkBeforeAddSingleEntry(entryInfoEntity,department);
        if(!isLegal){
            return error(entryInfoEntity, "当前词条不符合规范,请检查词条的二级分类是否为空字符串");
        }
        EntryInfoEntity savedEntity = entryInfoService.addSingleEntry(entryInfoEntity, request);
        return ok(savedEntity);
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
        try {
            String result = entryInfoService.updateEntryInfo(entryInfoEntity, request, notes);
            if(result.equals("不允许二级分类更新为空字符串")){
                HttpResponse<String> httpResponse = new HttpResponse<>();
                httpResponse.setCode(HttpResponse.Type.OK.getVal());
                httpResponse.setType(HttpResponse.Type.ERROR);
                httpResponse.setData(result);
                return httpResponse;
            }
            return checkResult(result);            
        } catch (Exception e) {
            // TODO: handle exception
            return error(null, e.getMessage());
        }

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
    @PostMapping("/checkBeforeExportEntry")
    @ApiOperation("在导出词条前进行检查")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<EntryInfoEntityForExcel>> checkBeforeExportEntry(@RequestBody ExcelExportVO<EntryInfoEntityForExcel> excelExportVO,HttpServletRequest request){
        /* 校验是否有不符合规定的词条，如果存在，就返回异常提示不能导出，并提供有异常的词条 */
        String department = JWTTokenUtils.getDepartment(request.getHeader("token"));
        if(department.equals("装置开发部")){
            excelExportVO.setPredicate(new Predicate<EntryInfoEntityForExcel>() {

                @Override
                public boolean test(EntryInfoEntityForExcel t) {
                    // TODO Auto-generated method stub
                    return t.getEntryState() < 3;
                }
                
            });
        }
        List<EntryInfoEntityForExcel> problematicEntryInfos = entryInfoService.filterEntryInfoBeforeExport(excelExportVO);
        ResponseListModel<EntryInfoEntityForExcel> responseListModel = new ResponseListModel<>();
        if(problematicEntryInfos != null){
            responseListModel.setList(problematicEntryInfos);
            responseListModel.setTotalNum(problematicEntryInfos.size());
        }else{
            responseListModel.setList(new ArrayList<>());
            responseListModel.setTotalNum(0);
        }

        return ok(responseListModel);
    }

    @PostMapping("/entryExportByCondition")
    @ApiOperation("导出词条（版本，任务，产品）")
    @CrossOrigin
    @Transactional
    public void entryExportByCondition(@RequestBody ExcelExportVO<EntryInfoEntityForExcel> excelExportVO,HttpServletRequest request,
                                       HttpServletResponse response,  String taskID) {
        ByteArrayOutputStream buffer = null;
        String charsetName = "UTF-8";
        HttpResponse<List<EntryInfoEntityForExcel>> exceptionResponse = new HttpResponse<>();
        try {
            String excelNamePrefix = URLEncoder.encode(excelExportVO.getExcelName(), charsetName);
            String exportFileType = request.getParameter("exportType");
            if(exportFileType == null){
                exportFileType = "excel";
            }
            Date date = new Date(); 
            String excelName = excelNamePrefix + "_" + LocalTimeUtils.formatForExportFile.format(date);
            /* 设定导出的文件类型 */
            if(exportFileType.equals("excel")){
                response.setHeader("Content-disposition", "attachment;filename=" + excelName + ".xlsx");
            }else if(exportFileType.equals("csv")){
                response.setHeader("Content-disposition", "attachment;filename=" + excelName + ".csv");
            }else{
                throw new RuntimeException("提供的exportFileType不支持: " + exportFileType);
            }
            excelExportVO.setExportFileType(exportFileType);
            /* 判断是否没有提供导出的词条,如果没有，则根据提供的词条查询条件，在库里查询要导出的词条 */
            entryInfoService.getEntryInfoEntityForExport(excelExportVO);
            /* 预处理将要导出的词条 */
            entryInfoService.postProcessEntryInfoForExport(excelExportVO);   
            /* 导出词条 */
            buffer = new ByteArrayOutputStream();
            entryInfoService.entryExportByCondition(buffer,excelExportVO,taskID); 
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");  
            buffer.writeTo(response.getOutputStream());
        } catch (Exception e) {
            log.error("导出文件功能报错", e);
            if(!response.isCommitted()){
                /* 保证原子性 */
                response.reset();
                response.setHeader("content-type", "application/json");
                exceptionResponse.setCode(HttpResponse.Type.ERROR.getVal());
                exceptionResponse.setType(HttpResponse.Type.ERROR);
                exceptionResponse.setMessage(e.getMessage());
                Gson gson = new Gson();
                try {
                    response.getOutputStream().write(gson.toJson(exceptionResponse).getBytes(charsetName));
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    log.error("向响应体中写入数据时报错", e1);
                }
            }else{
                log.error("已经开始向响应中写响应体时发生异常", e);
            }
        } finally{
            if(buffer != null){
                try {
                    buffer.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.error("关闭缓存字节流时报错", e);
                }
            }
        }
        
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
        String relation = null;
        try {
             relation = entryInfoService.addProductRelation(relationEntity,request);
        }catch (Exception e){
            e.printStackTrace();
            return error(null, e.getMessage());
        }

        return checkResult(relation);
    }


    private Map<String,List<EntryInfoEntity>> cacheForCheckNotUseEntry = new HashMap<>();
    
    private AtomicReference<String> currentCachedId = new AtomicReference<>(null);
    
    @Autowired
    private ResponseMapper responseMapper;

    private ReentrantLock lock = new ReentrantLock(true);

    @PostMapping("/getCheckNotUseEntry")
    @ApiOperation("获取冗余校验结果")
    @CrossOrigin
    @Transactional
    public  HttpResponse getCheckNotUseEntryResult(
        @RequestParam String i18nURL,
        @RequestParam String classfyID,
        @RequestParam(required = false,defaultValue = "-1") Integer pageIndex,
        @RequestParam(required = false,defaultValue = "-1") Integer pageSize) {
        
        String storeID = classfyID + i18nURL;

        Map<String,Object> dataResult = new HashMap<>(); 
        try {
            if(currentCachedId.get() == null){
                // 没有正在执行的冗余校验任务,或者尚未执行
                dataResult.put("state", 0);
                dataResult.put("list", null);
                return ok(dataResult);
            }else if(!currentCachedId.get().equals(storeID)){
                // 当前存在新的冗余校验执行任务,如果需要,请重新执行
                dataResult.put("state", 0); // 正在执行
                dataResult.put("list", null);
                return ok(dataResult);
            }
            // 弱一致性
            // 可能是还在执行中(null),或者执行失败(没找到)，也可能新的任务执行，但不影响
            Gson gson = new Gson();
            if(cacheForCheckNotUseEntry.containsKey(storeID)){
                // 当前任务执行中，在执行的过程中可能返回结果
                List<EntryInfoEntity> result = cacheForCheckNotUseEntry.get(storeID);
                if(result != null){
                    List results = result;
                    int size = results.size();
                    List<EntryInfoEntity> resultList = null;
                    int fromIndex = (pageIndex - 1) * pageSize;
                    int toIndex = fromIndex + pageSize;
                    if(!results.isEmpty() && fromIndex >= 0 && toIndex >= 0 && fromIndex <= toIndex){
                        if(fromIndex >= results.size()){
                            return error(null, "页码和页数设定不合理,超出查询范围");
                        }
                        resultList = results.subList(fromIndex, toIndex >= size ? size : toIndex);
                        // model.setList(results.subList(fromIndex, toIndex >= results.size() ? results.size() : toIndex));
                    }else{
                        // model.setList(results);
                        resultList = results;
                    }
                    dataResult.put("list", resultList);
                    dataResult.put("totalNum", results.size());
                    dataResult.put("state", 1);
                    return ok(dataResult);
                }else{
                    // 冗余校验正在执行中
                    dataResult.put("state", 3);
                    dataResult.put("list", null);
                    return ok(dataResult);
                }
            }else{
                // 当前没有对应的任务,任务执行失败，或者还没执行
                Thread.sleep(2000); // 可能线程没来的及执行，再次进行判断
                if(!cacheForCheckNotUseEntry.containsKey(storeID)){
                    if(currentCachedId.get() == null){
                        dataResult.put("state", 2); // 执行失败
                    }else if(!currentCachedId.get().equals(storeID)){
                        dataResult.put("state", 0);  // 正在执行新的任务
                        dataResult.put("list", null);
                    }
                    
                }else{
                    dataResult.put("state", 3); // 正在执行中
                    dataResult.put("list", null);
                }
                return ok(dataResult);
                // ResponseEntity entity = new ResponseEntity();
                // entity.setTask("checkNotUseEntry");
                // entity.setDataType("json");
                // entity.setTaskId(storeID);
                // List<ResponseEntity> responseEntities = responseMapper.getResponseByTaskId(entity);
                // ResponseListModel model = new ResponseListModel<>();
                // if(!responseEntities.isEmpty()){
                //     // 数据库里面有了
                //     String content = responseEntities.get(0).getContent() ;
                //     List resultList = gson.fromJson(content, List.class);
                //     if(fromIndex >= resultList.size()){
                //         return error(null, "fromIndex大于等于数据总数");
                //     }
                //     if(fromIndex >= 0 && toIndex >= 0){
                //         model.setList(resultList.subList(fromIndex, toIndex >= resultList.size() ? resultList.size() : toIndex));
                //     }else{
                //         model.setList(resultList);
                //     }
                //     model.setTotalNum(model.getList().size());
                //     // 缓存没有，数据库有，写入缓存中
                //     cacheForCheckNotUseEntry.put(storeID, content);
                    
                //     return ok(model);    
                // }else{
                    // return error(null, "程序运行异常");
                // }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return error(null, "执行失败,无法获取数据,internalError");
        } finally{
            
        }

    }

    @PostMapping("/deleteNotUseEntry")
    @ApiOperation("删除冗余词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteNotUseEntry(
        @RequestParam String i18nURL,
        @RequestParam String classfyID,
        @RequestBody List<String> entryList){

        try {
            String storeID = classfyID + i18nURL;
            if(currentCachedId.get() == null){
                return error(null, " 没有正在执行的冗余校验任务,或者尚未执行");
            }
            if(!currentCachedId.get().equals(storeID)){
                return error(null, "因为现在有新的任务,所以无法删除");
            }
            if(!cacheForCheckNotUseEntry.containsKey(storeID)){
                return error(null, "没有获取到冗余校验的数据,无法进行删除");
            }
            List<EntryInfoEntity> data = cacheForCheckNotUseEntry.get(storeID); // 默认当前存在
            if(data == null){
                return ok("当前有新任务在进行执行,无法进行词条删除操作");
            }
            List<EntryInfoEntity> removedList = new LinkedList<>();
            for(EntryInfoEntity entity : data){
         
                if(!entryList.contains(entity.getId())){
                    removedList.add(entity);
                }
                
            }
            if(!currentCachedId.get().equals(storeID)){
                return error(null, "因为现在有新的任务,所以无法删除");
            }
            if(!cacheForCheckNotUseEntry.containsKey(storeID) || cacheForCheckNotUseEntry.get(storeID) == null){
                return error(null, "有新的冗余校验任务,无法进行删除");
            }
            /*
             * 删除的时候加锁
             */
            try {
                lock.lock();
                String result = entryInfoService.deleteEntryInfo(entryList, "t_entry_info");
                cacheForCheckNotUseEntry.put(storeID, removedList);      
                return ok(result);
            } catch (Exception e) {
                // TODO: handle exception
                throw e;
            } finally{
                lock.unlock();
            }
      

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return error(null, e.getMessage());
        }
    }

    @PostMapping("/checkNotUseEntry")
    @ApiOperation("校验未使用的词条")
    @CrossOrigin
    @Transactional
    public  HttpResponse checkNotUseEntry(@RequestParam String i18nURL ,@RequestParam String classfyID,
        HttpServletRequest request,
        @RequestBody(required = false) CheckNotUseEntryVO params) {

        String storeID = classfyID + i18nURL;
        currentCachedId.set(storeID);
        cacheForCheckNotUseEntry.clear();
        
        Callable<Boolean> callable = new Callable<Boolean>() {
            // HttpServletRequest requestCopied = request;
            @Override
            public Boolean call() throws Exception {
                // TODO Auto-generated method stub
                Map<String, String> condition = params != null ? params.getParams() : null;
                Map<String, List<String>> sources = params != null ? params.getSources() : null;
                List<EntryInfoEntity> entities;
                try {
                    if(condition == null){
                        condition = new HashMap<>();
                    }
                    // condition.put("department", department);
                    // if(!classfyID.equals(FULL_SEARCH)){
                    try {
                        lock.lock();
                        entities = entryInfoService.checkNotUseEntry(i18nURL, classfyID,sources, condition);   
                    } catch (Exception e) {
                        // TODO: handle exception
                        throw e;
                    } finally{
                        lock.unlock();
                    }
                    
                    // }else{
                        // entities = entryInfoService.checkNotUseEntry(i18nUrl, sources, condition);
                    // } 
                }catch (Exception e){
                    if(!currentCachedId.compareAndSet(storeID, null)){

                    }

                    cacheForCheckNotUseEntry.remove(storeID);
                    e.printStackTrace();
                    return false;
                }
                /*
                * 写缓存
                */
                try {
                    try {
                        Gson gson = new Gson();
                        // String resultText = gson.toJson(entities);
                    //     ResponseEntity responseEntity = new ResponseEntity();
                    //     responseEntity.setContent(resultText);
                    //     responseEntity.setTaskId(storeID);
                    //     responseEntity.setDataType("json");
                    //     responseEntity.setTask("checkNotUseEntry");

                    //     List<ResponseEntity> responseByTaskId = responseMapper.getResponseByTaskId(responseEntity);
                    //     if(responseByTaskId.isEmpty()){
                    //         responseMapper.insertResponse(responseEntity);  // 以前没执行过这个任务，新插入数据
                    //     }else{
                    //         responseMapper.updateResponse(responseEntity); // 以前执行过更新结果
                    //     }
                        /*
                         * 弱一致性的结果，
                         * 比如后执行一次checkNotUseEntry方法的结果先于前一次执行checkNotUseEntry方法的结果存储在map中
                         * 不影响: 因为用户可以重新查询
                         */
                        cacheForCheckNotUseEntry.put(storeID, entities); 
                        return true;   
                    } catch (Exception e) {
                        if(!currentCachedId.compareAndSet(storeID, null)){
                            
                        }
                        cacheForCheckNotUseEntry.remove(storeID);
                        e.printStackTrace();
                        return false;
                    }                     
                } catch (Exception e) {
                    // TODO: handle exception
                    if(!currentCachedId.compareAndSet(storeID, null)){
                        
                    }
                    cacheForCheckNotUseEntry.remove(storeID);
                    e.printStackTrace();
                    return false;
                }
            }
        };
        try {
       
            cacheForCheckNotUseEntry.put(storeID, null);
            threadPoolExecutor.submit(callable);
            // return ok("已经提交任务，正在执行中");
            Map<String,String> resultMap = new HashMap<>();
            resultMap.put("state", "0") ;
            return ok(resultMap);
        } catch (Exception e) {
            // TODO: handle exception
            // 没有放入线程池
            if(!currentCachedId.compareAndSet(storeID, null)){
                // 说明执行到这一步前有新的checkNotUseEntry请求
                
            }
            cacheForCheckNotUseEntry.remove(storeID);
            return error("无法执行该任务，服务异常",e.getMessage());
        } finally{
          
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
        List<String> exceptionMessages = new ArrayList<>();
        List<SourceEntryVO> sourceEntryVOS = null;
        try {
            sourceEntryVOS = entryInfoService.getEntrysourceListByClassfy(classfyID, i18nUrl,request,exceptionMessages);
            if (null == sourceEntryVOS){
                return checkResult(null,"i18n 服务异常 ！");
            }
            HttpResponse<ResponseListModel<SourceEntryVO>> httpResponse = new HttpResponse<>();
            responseListModel.setList(sourceEntryVOS);
            responseListModel.setTotalNum(sourceEntryVOS.size());
            httpResponse.setData(responseListModel);
            if(!exceptionMessages.isEmpty()){
                StringBuilder builder = new StringBuilder();
                for(String message : exceptionMessages){
                    builder.append(message);
                    builder.append(";");
                }
                httpResponse.setMessage(builder.toString()); 
                
            }
            HttpResponse.Type responseType = exceptionMessages.isEmpty() ? HttpResponse.Type.OK : HttpResponse.Type.INTERNAL_ERROR;
            httpResponse.setType(responseType);
            httpResponse.setCode(responseType.getVal());
            return httpResponse;
        } catch (Exception e) {
            // TODO: handle exception
            log.error(null, e);
            return checkResult(null,e.getMessage());
        }
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

    @PostMapping("/forrbiddenEntry")
    @ApiOperation("禁用词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> forrbiddenEntry(@RequestBody List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request){


        boolean isSuccess = entryInfoService.forrbiddenEntry(entryInfoEntities,request);
        if(isSuccess){
            return ok("已成功禁用" + entryInfoEntities.size() + "个词条");
        }else{
            return error(null,"禁用词条失败");
        }

    }

    // public HttpResponse

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
