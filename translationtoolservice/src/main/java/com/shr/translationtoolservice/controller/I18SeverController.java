package com.shr.translationtoolservice.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.util.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @ClassName I18SeverController
 * @Description i18Server
 * @USER: Cola
 * @Date 2023/12/13 0013 18:51
 **/


@RestController
@RequestMapping("/I18Sever")
@Api(tags = "i18Sever")
@Slf4j
@PropertySource("classpath:application.yml")
public class I18SeverController extends BaseController {


    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private HTTPUtils httpUtils;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private VersionTableMapper versionTableMapper;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private EntryInfoService entryInfoService;

    @Autowired
    private EntryInfoMapper entryInfoMapper;
    @Autowired
    private ProductTableMapper productTableMapper;

    @Autowired
    private EntryProcessUtils entryProcessUtils;

    @Autowired
    private I18nService i18nService;


    @GetMapping("/language")
    @ApiOperation("获取语言缩写")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> language() {
        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.LANGUAGE);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }




    @GetMapping("/getFileListByLang")
    @ApiOperation("获取文件列表")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFileListByLang(@RequestParam String language) {
        //获取语言code
        List<TLanguage> languageList = languageMapper.selectLaguageByName(language);
        ResponseListModel<String> responseListModel = new ResponseListModel<>();
        Map<String, String> headerParameters = new HashMap<>();
        headerParameters.put("laguage", languageList.get(0).getCode());
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
                s = httpUtils.get(I18URL + ConstantInterface.GET_FILE_LIST, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getWords")
    @ApiOperation("获取TS文件词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getWords(@RequestBody List<String> fileNames,
                                                    @RequestParam String taskID,
                                                    @RequestParam String translateType, HttpServletRequest request) {

        //List<TLanguage> languageList = languageMapper.selectLaguageByName(translateType);
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        List<String> versionIDs = taskInfoMapper.getVersionIDByTaskID(taskID);
        String versionID = "";
        if (!CollectionUtils.isEmpty(versionIDs)) {
            versionID = versionIDs.get(0);
        }
        String productId = taskInfoMapper.getTaskEntityByTaskID(taskID).getProductId();
        // String language = languageList.get(0).getName();
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();

        ArrayList<EntryInfoEntity> list = new ArrayList<>();
        for (String fileName : fileNames) {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("fileName", fileName);

            JSONArray jsonArray = new JSONArray();
            String s = "";

            try {
                s = httpUtils.get(I18URL + ConstantInterface.GET_WORDS, headerParameters);

                jsonArray = JSONArray.parseArray(s);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONArray jsonArray1 = JSONArray.parseArray(jsonArray.get(i).toString());
                    String entry = jsonArray1.get(0).toString();
                    String translate = jsonArray1.getString(1);
                    String tag = jsonArray1.getString(2);
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID());
                    entryInfoEntity.setEntrySource(fileName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setEntry(entry);
                    entryInfoEntity.setUpdate(userName);
                    entryInfoEntity.setUpdateTime(date);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntity.setEntryLabel(tag);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setImportType(ConstantInterface.TS);
                    entryInfoEntity.setIsDelete(0);
                    createNewTrans(entryInfoEntity, translateType, translate);
                    list.add(entryInfoEntity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        List<EntryInfoEntity> entryInfoEntities = buildRepeTempEntry(list, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryInfoEntities, taskID);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        return checkResult(responseListModel);
    }

    private void createNewTrans(EntryInfoEntity entryInfoEntity, String translateType, String translate) {
        //写入翻译字段
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setEnglish(translate);
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setSpanish(translate);
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setRussian(translate);
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setFrench(translate);
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
        }
    }

    @PostMapping("/saveWords")
    @ApiOperation("回写ts文件")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> saveWords(@RequestBody List<TSWordVO> tsWordVOS) {
        String s = "";
        try {
            //遍历文件
            for (TSWordVO tsWordVO : tsWordVOS) {
                String ffileName = tsWordVO.getFileName();
                ArrayList<Map<String, String>> requestList = new ArrayList<>();
                JSONObject jsonObject = new JSONObject();
                //遍历单词
                for (EntryInfoEntity entryInfoEntity : tsWordVO.getEntryInfoEntities()) {
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("source", entryInfoEntity.getEntry());
                    switch (tsWordVO.getTranslateType()) {
                        case ConstantInterface.ENGLISH:
                            requestMap.put("translate", entryInfoEntity.getEnglish());
                            break;
                        case ConstantInterface.RUSSIAN:
                            requestMap.put("translate", entryInfoEntity.getRussian());
                            break;
                        case ConstantInterface.SPANISH:
                            requestMap.put("translate", entryInfoEntity.getSpanish());
                            break;
                        case ConstantInterface.FRENCH:
                            requestMap.put("translate", entryInfoEntity.getFrench());
                            break;
                    }

                    requestList.add(requestMap);
                }
                jsonObject.put("entry", requestList);
                jsonObject.put("test", "test6");
                s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + ffileName, jsonObject);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return checkResult(s);
    }

    @GetMapping("/getDictionary")
    @ApiOperation("获取字典列表")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDictionary() {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {

                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/setDictionaryInfo")
    @ApiOperation("回写字典详情")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> setDictionaryInfo(@RequestBody List<String> types) {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            for (String type : types) {
                s = httpUtils.post(I18URL + ConstantInterface.DICTIONARY + "/" + type, new JSONObject());
            }

            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/createDic")
    @ApiOperation("创建辞典")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> createDic(@RequestParam String dicName) {

        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dictionary", dicName);
            System.out.println(I18URL + ConstantInterface.Create_DI);
            s = httpUtils.get(I18URL + ConstantInterface.Create_DI, headerParameters);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return checkResult(ConstantInterface.OK_STR);
    }




    @PostMapping("/setInfo")
    @ApiOperation("回写")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> setInfo(@RequestBody List<EntryInfoEntity> entryInfoEntities, @RequestParam String translateType,
                                        @RequestParam( required = false) String taskID,@RequestParam int isTag,@RequestParam int isComment) {
        boolean tag  =true;
        boolean comment = true;
        if (isTag ==0){
            tag = false;
        }
        if (isComment == 0 ){
            comment = false;
        }
        if (StringUtils.isNotBlank(taskID)){
            entryInfoEntities = entryInfoMapper.getWriteEntryByTaskID(taskID, "");
            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
             translateType = taskInfoEntity.getTranslateType();
        }
        return checkResult(i18nService.setInfoByEntryList(entryInfoEntities,translateType,tag,comment));
    }






    @GetMapping("/getDictionaryInfo")
    @ApiOperation("获取字典详情")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDictionaryInfo(@RequestParam String type,
                                                             @RequestParam String transType,
                                                             @RequestParam String versionID,
                                                             @RequestParam String taskID, HttpServletRequest request) {

        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        List<TLanguage> languageList = languageMapper.selectLaguageByName(transType);
        String code = "";
        if (CollectionUtils.isEmpty(languageList)) {
            code = languageList.get(0).getName();
        }
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<EntryInfoEntity> list = new ArrayList<>();
        String productId = taskInfoMapper.getTaskEntityByTaskID(taskID).getProductId();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + type);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID());
                entryInfoEntity.setEntry(dictionaryVo.getSource());
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                entryInfoEntity.setImportType(ConstantInterface.DI);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setEntrySource(type);
                entryInfoEntity.setDiFileName(type);
                entryInfoEntity.setProductID(productId);
                entryInfoEntity.setIsDelete(0);
                entryInfoEntity.setUpdateTime(date);
                entryInfoEntity.setUpdate(userName);
                Map<String, String> map = dictionaryVo.getTranslation();
                if (StringUtils.isBlank(map.get(code))) {
                    switch (code) {
                        case ConstantInterface.ENGLISH:
                            entryInfoEntity.setEnglish(map.get(code));
                            break;
                        case ConstantInterface.RUSSIAN:
                            entryInfoEntity.setRussian(map.get(code));
                            break;
                        case ConstantInterface.FRENCH:
                            entryInfoEntity.setFrench(map.get(code));
                            break;
                        case ConstantInterface.SPANISH:
                            entryInfoEntity.setSpanish(map.get(code));
                            break;
                    }
                }
                list.add(entryInfoEntity);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryInfoEntities = buildRepeTempEntry(list, transType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryInfoEntities, taskID);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getAllNode")
    @ApiOperation("获取节点信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getAllNode() {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.GET_ALL_NODE);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
            log.info(" start send http request : " + I18URL + ConstantInterface.GET_ALL_NODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/getAppByNode")
    @ApiOperation("获取应用信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getAppByNode(@RequestParam String nodeName) {
        ResponseListModel<TDBappVo> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<TDBappVo> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_APP_BYNODE, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBappVo tdBappVo = new TDBappVo();
                JSONArray jsonArray1 = JSONArray.parseArray(jsonArray.getString(i));
                String name = jsonArray1.getString(0);
                int type = jsonArray1.getInteger(1);
                tdBappVo.setName(name);
                tdBappVo.setType(type);
                list.add(tdBappVo);
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getdbByApp")
    @ApiOperation("获取数据库信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getdbByApp(@RequestParam String nodeName,
                                                      @RequestParam String appName,
                                                      @RequestParam String modeType) {
        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("nodeName", nodeName);
            headerParameters.put("appName", appName);
            headerParameters.put("modeType", modeType);
            s = httpUtils.get(I18URL + ConstantInterface.GET_DB_BYAPP, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {

                list.add(jsonArray.getString(i));
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/getTableByApp")
    @ApiOperation("获取表信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getTableByApp(@RequestParam String dbName,
                                                         @RequestParam String nodeName,
                                                         @RequestParam String appName) {
        ResponseListModel<TDBTableInfo> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<TDBTableInfo> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_TB_APP, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                list.add(tdbTableInfo);
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getFieldByTable")
    @ApiOperation("获取字段集合")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFieldByTable(@RequestParam String dbName,
                                                           @RequestParam String nodeName,
                                                           @RequestParam String appName,

                                                           @RequestParam String tbName
    ) {
        ResponseListModel<TDBFieldInfo> responseListModel = new ResponseListModel<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<TDBFieldInfo> fieldList = new ArrayList<>();

        try {

            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("tbName", tbName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_FIELD_TABLE, headerParameters);
            if ("0".equals(s)) {
                return checkResult(null, " request error ! ");
            }
            jsonArray = JSONArray.parseArray(s);


            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                fieldList.add(tdbFieldInfo);
            }
            //  tdbTableInfo.setFields(fieldList);

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }


        responseListModel.setList(fieldList);
        responseListModel.setTotalNum(fieldList.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getFieldData")
    @ApiOperation("获取字段内容")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFieldData(@RequestParam String dbName,
                                                        @RequestParam String nodeName,
                                                        @RequestParam String appName,
                                                        @RequestParam int tbID,
                                                        @RequestParam String tbName,
                                                        @RequestParam String versionID,
                                                        @RequestParam String taskID,
                                                        @RequestParam String translateType,
                                                        @RequestParam String diFileName,
                                                        @RequestBody List<TDBFieldInfo> fieldInfos, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        String productId = taskInfoMapper.getTaskEntityByTaskID(taskID).getProductId();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<EntryInfoEntity> fieldList = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("dbName", dbName);
            jsonObject.put("appName", appName);
            jsonObject.put("tbName", tbName);
            jsonObject.put("nodeName", nodeName);
            jsonObject.put("tbID", tbID);
            jsonObject.put("fieldInfos", fieldInfos);
            s = httpUtils.post(I18URL + ConstantInterface.GET_FIELD_DATA, jsonObject);
            if ("0".equals(s)) {
                return checkResult(null, " request error ! ");
            }
            jsonArray = JSONArray.parseArray(s);

            String source = nodeName + "_" + appName + "_" + dbName + "_" + tbName;
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                for (String entry : tdbFieldInfo.getFieldDatas()) {
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbFieldInfo.getFieldID());
                    fieldEntry.setEntry(entry);

                    // fieldEntry.setEntrySource(  nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tbName + ConstantInterface.UNDERLINE + tdbFieldInfo.getFieldName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setDiFileName(diFileName);
                    fieldEntry.setEntrySource(source);
                    fieldEntry.setVersionID(versionID);
                    fieldEntry.setTaskId(taskID);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setProductID(productId);
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                }


                //   fieldList.add(fieldEntry);
            }
            //  tdbTableInfo.setFields(fieldList);

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<EntryInfoEntity> entryInfoEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryInfoEntities, taskID);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getAlias")
    @ApiOperation("获取别名")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getAlias(@RequestParam String dbName,
                                                    @RequestParam String nodeName,
                                                    @RequestParam String appName,
                                                    @RequestParam String versionID,
                                                    @RequestParam String taskID,
                                                    @RequestParam String diFileName,
                                                    @RequestParam String translateType,
                                                    @RequestParam( required = false) Integer maxLength, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_ALIAS, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            String source = nodeName + "_" + appName + "_" + dbName;
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);
                if (StringUtils.isNotBlank(tdbTableInfo.getAliasName())){
                    //将表的别名写入词条
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                    entryInfoEntity.setEntry(tdbTableInfo.getAliasName());
                    entryInfoEntity.setDiFileName(diFileName);
                    entryInfoEntity.setEntrySource(source);
                    //  entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setIsDelete(0);
                    if (Objects.isNull(maxLength)){
                        entryInfoEntity.setMaxLength(0);
                    }else {
                        entryInfoEntity.setMaxLength(maxLength);
                    }
                    createNewTrans(entryInfoEntity, translateType, "");
                    entryInfoEntity.setImportType(ConstantInterface.DB);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntities.add(entryInfoEntity);
                }


                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    if (StringUtils.isBlank(fieldInfo.getAliasName())){
                        continue;
                    }
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                    fieldEntry.setEntry(fieldInfo.getAliasName());
                    fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setDiFileName(diFileName);
                    fieldEntry.setMaxLength(maxLength);
                    fieldEntry.setEntrySource(source + "_" + tdbTableInfo.getTableName());
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setProductID(productId);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getDBALLAliasEntryByNode")
    @ApiOperation("通过节点名取别名")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDBALLAliasEntryByNode(@RequestParam String nodeName,
                                                                    @RequestParam String versionID,
                                                                    @RequestParam String taskID,
                                                                    @RequestParam String diFileName,
                                                                    @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();

            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_DBALLENTRYBYNODE, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);
                if (StringUtils.isNotBlank(tdbTableInfo.getAliasName())){
                    //将表的别名写入词条
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                    entryInfoEntity.setEntry(tdbTableInfo.getAliasName());
                    entryInfoEntity.setDiFileName(diFileName);
                    entryInfoEntity.setEntrySource(tdbTableInfo.getCommon());
                    // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setIsDelete(0);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setTaskId(taskID);
                    createNewTrans(entryInfoEntity, translateType, "");
                    entryInfoEntity.setImportType(ConstantInterface.DB);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntities.add(entryInfoEntity);
                }


                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    if (StringUtils.isNotBlank(fieldInfo.getAliasName())){
                        EntryInfoEntity fieldEntry = new EntryInfoEntity();
                        fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                        fieldEntry.setEntry(fieldInfo.getAliasName());
                        fieldEntry.setDiFileName(diFileName);
                        // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                        fieldEntry.setEntryState(1);
                        fieldEntry.setEntrySource(fieldInfo.getCommon());
                        fieldEntry.setIsDelete(0);
                        fieldEntry.setUpdateTime(date);
                        fieldEntry.setUpdate(userName);
                        fieldEntry.setProductID(productId);
                        fieldEntry.setImportType(ConstantInterface.DB);
                        entryInfoEntities.add(fieldEntry);
                    }
                    if (!CollectionUtils.isEmpty(fieldInfo.getFieldDatas())){
                        for (String data : fieldInfo.getFieldDatas()) {
                            EntryInfoEntity dataEntry = new EntryInfoEntity();
                            dataEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                            dataEntry.setEntry(data);
                            // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                            dataEntry.setEntryState(1);
                            dataEntry.setDiFileName(diFileName);
                            dataEntry.setEntrySource(fieldInfo.getCommon() + "_" + fieldInfo.getFieldName());
                            dataEntry.setIsDelete(0);
                            dataEntry.setProductID(productId);
                            dataEntry.setUpdateTime(date);
                            dataEntry.setUpdate(userName);
                            dataEntry.setImportType(ConstantInterface.DB);
                            entryInfoEntities.add(dataEntry);
                        }
                    }

                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getDBALLEntryByNode")
    @ApiOperation("通过节点名取DB词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDBALLEntryByNode(@RequestBody List<I18nDBVO> i18nDBVO,
                                                               @RequestParam String versionID,
                                                               @RequestParam String taskID,
                                                               @RequestParam String diFileName,
                                                               @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();

        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();


            System.out.println("get request :" + I18URL + ConstantInterface.GET_DBALLENTRYBYNODE);
            s = httpUtils.post(I18URL + ConstantInterface.GET_DBALLENTRYBYNODE, JSONArray.toJSONString(i18nDBVO));

            jsonArray = JSONArray.parseArray(s);
            if (Objects.isNull(jsonArray)) {
                return checkResult(responseListModel);
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);
                if (StringUtils.isNotBlank(tdbTableInfo.getAliasName())){
                    //将表的别名写入词条
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                    entryInfoEntity.setEntry(tdbTableInfo.getAliasName());
                    entryInfoEntity.setDiFileName(diFileName);
                    entryInfoEntity.setEntrySource(tdbTableInfo.getCommon());
                    // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setIsDelete(0);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setTaskId(taskID);
                    createNewTrans(entryInfoEntity, translateType, "");
                    entryInfoEntity.setImportType(ConstantInterface.DB);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntities.add(entryInfoEntity);

                }


                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    if (StringUtils.isNotBlank(fieldInfo.getAliasName())){
                        EntryInfoEntity fieldEntry = new EntryInfoEntity();
                        fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                        fieldEntry.setEntry(fieldInfo.getAliasName());
                        fieldEntry.setDiFileName(diFileName);
                        // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                        fieldEntry.setEntryState(1);
                        fieldEntry.setEntrySource(fieldInfo.getCommon());
                        fieldEntry.setIsDelete(0);
                        fieldEntry.setProductID(productId);
                        fieldEntry.setUpdateTime(date);
                        fieldEntry.setUpdate(userName);
                        fieldEntry.setImportType(ConstantInterface.DB);
                        entryInfoEntities.add(fieldEntry);
                    }
                    if (!CollectionUtils.isEmpty(fieldInfo.getFieldDatas())){
                        for (String data : fieldInfo.getFieldDatas()) {
                            EntryInfoEntity dataEntry = new EntryInfoEntity();
                            dataEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                            dataEntry.setEntry(data);
                            // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                            dataEntry.setEntryState(1);
                            dataEntry.setDiFileName(diFileName);
                            dataEntry.setEntrySource(fieldInfo.getCommon() + "_" + fieldInfo.getFieldName());
                            dataEntry.setIsDelete(0);
                            dataEntry.setUpdateTime(date);
                            dataEntry.setUpdate(userName);
                            dataEntry.setProductID(productId);
                            dataEntry.setImportType(ConstantInterface.DB);
                            entryInfoEntities.add(dataEntry);
                        }
                    }

                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getDBALLEntryByApp")
    @ApiOperation("通过应用名取DB词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDBALLEntryByApp(@RequestBody List<I18nDBVO> i18nDBVO,
                                                              @RequestParam String versionID,
                                                              @RequestParam String taskID,
                                                              @RequestParam String diFileName,
                                                              @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        int sum =0;
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();


            System.out.println("get request :" + I18URL + ConstantInterface.GET_DBALLENTRYBYAPP);
            s = httpUtils.post(I18URL + ConstantInterface.GET_DBALLENTRYBYAPP, JSONArray.toJSONString(i18nDBVO));
            jsonArray = JSONArray.parseArray(s);
            if (Objects.isNull(jsonArray)) {
                return checkResult(responseListModel);
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                sum ++;
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);
                if (StringUtils.isNotBlank(tdbTableInfo.getAliasName())){
                    //将表的别名写入词条
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                    entryInfoEntity.setEntry(tdbTableInfo.getAliasName());
                    entryInfoEntity.setDiFileName(diFileName);
                    entryInfoEntity.setEntrySource(tdbTableInfo.getCommon());
                    // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setIsDelete(0);
                    entryInfoEntity.setTaskId(taskID);
                    createNewTrans(entryInfoEntity, translateType, "");
                    entryInfoEntity.setImportType(ConstantInterface.DB);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntities.add(entryInfoEntity);
                }


                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    if (StringUtils.isNotBlank(fieldInfo.getAliasName())){
                        sum ++;
                        EntryInfoEntity fieldEntry = new EntryInfoEntity();
                        fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                        fieldEntry.setEntry(fieldInfo.getAliasName());
                        fieldEntry.setDiFileName(diFileName);
                        // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                        fieldEntry.setEntryState(1);
                        fieldEntry.setEntrySource(fieldInfo.getCommon());
                        fieldEntry.setIsDelete(0);
                        fieldEntry.setUpdateTime(date);
                        fieldEntry.setUpdate(userName);
                        fieldEntry.setProductID(productId);
                        fieldEntry.setImportType(ConstantInterface.DB);
                        entryInfoEntities.add(fieldEntry);
                    }
                    if (!CollectionUtils.isEmpty(fieldInfo.getFieldDatas())){
                        for (String data : fieldInfo.getFieldDatas()) {
                            sum ++;
                            EntryInfoEntity dataEntry = new EntryInfoEntity();
                            dataEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                            dataEntry.setEntry(data);
                            // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                            dataEntry.setEntryState(1);
                            dataEntry.setDiFileName(diFileName);
                            dataEntry.setEntrySource(fieldInfo.getCommon() + "_" + fieldInfo.getFieldName());
                            dataEntry.setIsDelete(0);
                            dataEntry.setUpdateTime(date);
                            dataEntry.setUpdate(userName);
                            dataEntry.setProductID(productId);
                            dataEntry.setImportType(ConstantInterface.DB);
                            entryInfoEntities.add(dataEntry);
                        }
                    }

                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(sum);
        log.info(Integer.toString(sum) + " **** " + entryEntities1.size());

        return checkResult(responseListModel);
    }


    @PostMapping("/getDBALLEntryByDB")
    @ApiOperation("通过库名取DB词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDBALLEntryByDB(@RequestBody List<I18nDBVO> i18nDBVO,
                                                             @RequestParam String versionID,
                                                             @RequestParam String taskID,
                                                             @RequestParam String diFileName,
                                                             @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();

        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();


            System.out.println("get request :" + I18URL + ConstantInterface.GET_DBALLENTRYBYDB);
            s = httpUtils.post(I18URL + ConstantInterface.GET_DBALLENTRYBYDB, JSONArray.toJSONString(i18nDBVO));
            jsonArray = JSONArray.parseArray(s);
            if (Objects.isNull(jsonArray)) {
                return checkResult(responseListModel);
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                entryInfoEntity.setEntry(tdbTableInfo.getAliasName());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(tdbTableInfo.getCommon());
                // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setIsDelete(0);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.DB);
                entryInfoEntity.setProductID(productId);
                entryInfoEntities.add(entryInfoEntity);

                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                    fieldEntry.setEntry(fieldInfo.getAliasName());
                    // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setDiFileName(diFileName);
                    fieldEntry.setEntrySource(fieldInfo.getCommon());
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setProductID(productId);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                    for (String data : fieldInfo.getFieldDatas()) {
                        EntryInfoEntity dataEntry = new EntryInfoEntity();
                        dataEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                        dataEntry.setEntry(data);
                        // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                        dataEntry.setEntryState(1);
                        dataEntry.setDiFileName(diFileName);
                        dataEntry.setEntrySource(fieldInfo.getCommon() + "_" + fieldInfo.getFieldName());
                        dataEntry.setIsDelete(0);
                        dataEntry.setUpdateTime(date);
                        dataEntry.setUpdate(userName);
                        dataEntry.setProductID(productId);
                        dataEntry.setImportType(ConstantInterface.DB);
                        entryInfoEntities.add(dataEntry);
                    }

                }
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    public List<EntryInfoEntity> buildRepeTempEntry(List<EntryInfoEntity> entryInfoEntities, String translateType) {
        return entryProcessUtils.buildRepeEntry(entryInfoEntities, translateType);
    }

    //校验是否有重复词条
    private void caseExistEntry(List<EntryInfoEntity> newEntry, String taskID) {
        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        String productTableName = "t_entry_info";

        for (EntryInfoEntity entryInfoEntity : newEntry) {
            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                caseExistEntry(entryInfoEntity.getChildren(),taskID);
            }
            // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
            List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
            if (CollectionUtils.isEmpty(entryEntities)) {
                //创建新翻译
                entryInfoEntity.setIsExist(0);
                entryInfoEntity.setEntryVersion(0);
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            } else {
                entryInfoEntity.setIsExist(1);
                entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
            }
        }

    }







    @GetMapping("/getConfigEntry")
    @ApiOperation("读取配置文件词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getConfigEntry(@RequestParam String versionID,
                                                          @RequestParam String taskID,
                                                          @RequestParam String diFileName,
                                                          @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {

            s = httpUtils.get(I18URL + ConstantInterface.GET_CONGIF_ENTRY);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo fieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID());
                entryInfoEntity.setEntry(fieldInfo.getFieldName());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(fieldInfo.getCommon());
                entryInfoEntity.setEntryLabel(fieldInfo.getCommon());
                // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setUpdateTime(date);
                entryInfoEntity.setUpdate(userName);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                entryInfoEntity.setIsDelete(0);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.CONFIG);
                entryInfoEntity.setProductID(productId);
                entryInfoEntities.add(entryInfoEntity);


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/getEnumEntry")
    @ApiOperation("读取枚举文件词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEnumEntry(@RequestParam String versionID,
                                                          @RequestParam String taskID,
                                                          @RequestParam String diFileName,
                                                          @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {

            s = httpUtils.get(I18URL + ConstantInterface.GET_ENUM_ENTRY);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo fieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID());
                entryInfoEntity.setEntry(fieldInfo.getFieldName());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(fieldInfo.getCommon());
                entryInfoEntity.setEntryLabel(fieldInfo.getCommon());
                entryInfoEntity.setUpdateTime(date);
                entryInfoEntity.setUpdate(userName);
                // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.ENUM);
                entryInfoEntity.setProductID(productId);
                entryInfoEntity.setIsDelete(0);
                entryInfoEntities.add(entryInfoEntity);


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }





}


   /* @PostMapping("/getFieldData")
    @ApiOperation("获取字段内容")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFieldData(@RequestParam String dbName,
                                                        @RequestParam String nodeName,
                                                        @RequestParam String appName,
                                                        @RequestParam String versionID,
                                                        @RequestParam String taskID,
                                                        @RequestParam String translateType,
                                                        @RequestParam String type,
                                                        @RequestParam String tbName,
                                                        @RequestBody List<TDBTableInfo> tdbTableInfos
    ) {
        ResponseListModel<EntryTempEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryTempEntity> entryTempEntities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        for (TDBTableInfo tdbTableInfo : tdbTableInfos) {
            String tbName = tdbTableInfo.getTableName();
            try {
                ArrayList<TDBFieldInfo> fieldList = new ArrayList<>();
                Map<String, String> headerParameters = new HashMap<>();
                headerParameters.put("dbName", dbName);
                headerParameters.put("appName", appName);
                headerParameters.put("tbName", tbName);
                s = httpUtils.get(I18URL + ConstantInterface.GET_FIELD_TABLE, headerParameters);
                if ("0".equals(s)) {
                    return checkResult(null, " request error ! ");
                }
                jsonArray = JSONArray.parseArray(s);


                for (int i = 0; i < jsonArray.size(); i++) {
                    TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                    EntryTempEntity fieldEntry = new EntryTempEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbFieldInfo.getId());
                    if (type.equals("field")) {
                        fieldEntry.setEntry(tdbFieldInfo.getFieldName());

                    } else if (type.equals("alias")) {
                        fieldEntry.setEntry(tdbFieldInfo.getAliasName());
                    }
                    fieldEntry.setSource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tbName);
                    fieldEntry.setAuditState(0);
                    fieldEntry.setVersionID(versionID);
                    fieldEntry.setTaskId(taskID);
                    fieldEntry.setTranslateType(translateType);
                    fieldEntry.setImportype(ConstantInterface.DB);
                    entryTempEntities.add(fieldEntry);
                    fieldList.add(tdbFieldInfo);
                }
                tdbTableInfo.setFields(fieldList);

                log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        responseListModel.setList(entryTempEntities);
        responseListModel.setTotalNum(entryTempEntities.size());
        return checkResult(responseListModel);
    }*/
