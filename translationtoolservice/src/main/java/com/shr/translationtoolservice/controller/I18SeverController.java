package com.shr.translationtoolservice.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.HTTPUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.reflect.FieldInfo;

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
    @ApiOperation("获取文件词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getWords(@RequestBody List<String> fileNames,
                                                    @RequestParam String taskID,
                                                    @RequestParam String translateType) {

        List<TLanguage> languageList = languageMapper.selectLaguageByName(translateType);

        List<String> versionIDs = taskInfoMapper.getVersionIDByTaskID(taskID);
        String versionID = "";
        if (!CollectionUtils.isEmpty(versionIDs)) {
            versionID = versionIDs.get(0);
        }
        String language = languageList.get(0).getCode();
        ResponseListModel<EntryTempEntity> responseListModel = new ResponseListModel<>();

        ArrayList<EntryTempEntity> list = new ArrayList<>();
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
                    EntryTempEntity entryTempEntity = new EntryTempEntity();
                    entryTempEntity.setId(commonUtils.getUUID());
                    entryTempEntity.setSource(fileName);
                    entryTempEntity.setAuditState(0);
                    entryTempEntity.setTaskId(taskID);
                    entryTempEntity.setTranslateType(language);
                    entryTempEntity.setEntry(entry);
                    entryTempEntity.setTranslate(translate);
                    entryTempEntity.setTag(tag);
                    entryTempEntity.setVersionID(versionID);
                    entryTempEntity.setImportype(ConstantInterface.TS);
                    list.add(entryTempEntity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }


        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
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
                for (EntryTempEntity entryTempEntity : tsWordVO.getEntryTempEntities()) {
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("source", entryTempEntity.getEntry());
                    requestMap.put("translate", entryTempEntity.getTranslate());
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
                s = httpUtils.post(I18URL + ConstantInterface.DICTIONARY + "/" + type, null);
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


    @PostMapping("/setInfo")
    @ApiOperation("回写")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> setInfo(@RequestBody List<EntryTempEntity> tempEntities) {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        String importype = tempEntities.get(0).getImportype();
        String s = "";
        if ("TS".equals(importype)) {
            String fileName = "";
            ArrayList<Map<String, String>> requestList = new ArrayList<>();
            JSONObject jsonObject = new JSONObject();
            for (EntryTempEntity entryTempEntity : tempEntities) {
                fileName = entryTempEntity.getSource();
                //遍历单词
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("source", entryTempEntity.getEntry());
                requestMap.put("translate", entryTempEntity.getTranslate());
                requestList.add(requestMap);
            }
            jsonObject.put("entry", requestList);
            s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
            //写入词条库

            return checkResult(ConstantInterface.OK_STR);
        } else if ("DI".equals(importype)) {
            //TODO 等待字典写入接口结束
            return checkResult("暂未开放");
        } else if ("DB".equals(importype)) {
            return checkResult("暂未开放");
        } else {
            log.error(" importType is null ! ");
            return checkResult(" importType is null ! ");

        }
    }


    @GetMapping("/getDictionaryInfo")
    @ApiOperation("获取字典详情")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDictionaryInfo(@RequestParam String type,
                                                             @RequestParam String transType,
                                                             @RequestParam String versionID,
                                                             @RequestParam String taskID) {

        ResponseListModel<EntryTempEntity> responseListModel = new ResponseListModel<>();
        List<TLanguage> languageList = languageMapper.selectLaguageByName(transType);
        String code = "";
        if (CollectionUtils.isEmpty(languageList)) {
            code = languageList.get(0).getCode();
        }

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<EntryTempEntity> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + type);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                EntryTempEntity entryTempEntity = new EntryTempEntity();
                entryTempEntity.setId(commonUtils.getUUID());
                entryTempEntity.setEntry(dictionaryVo.getSource());
                entryTempEntity.setTranslateType(transType);
                entryTempEntity.setVersionID(versionID);
                entryTempEntity.setTaskId(taskID);
                entryTempEntity.setImportype("DI");
                entryTempEntity.setAuditState(0);
                entryTempEntity.setSource(type);
                for (Map<String, String> map : dictionaryVo.getTranslation()) {
                    if (StringUtils.isBlank(map.get(code))) {
                        entryTempEntity.setTranslate(map.get(code));
                    }
                    list.add(entryTempEntity);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
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
                                                        @RequestBody List<TDBFieldInfo> fieldInfos
    ) {
        ResponseListModel<EntryTempEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryTempEntity> entryTempEntities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<EntryTempEntity> fieldList = new ArrayList<>();

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


            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                for (String entry : tdbFieldInfo.getFieldDatas()){
                    EntryTempEntity fieldEntry = new EntryTempEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbFieldInfo.getFieldID());
                    fieldEntry.setEntry(entry);

                    fieldEntry.setSource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tbName + ConstantInterface.UNDERLINE + tdbFieldInfo.getFieldName());
                    fieldEntry.setAuditState(0);
                    fieldEntry.setVersionID(versionID);
                    fieldEntry.setTaskId(taskID);
                    fieldEntry.setTranslateType(translateType);
                    fieldEntry.setImportype(ConstantInterface.DB);
                    entryTempEntities.add(fieldEntry);
                }



             //   fieldList.add(fieldEntry);
            }
            //  tdbTableInfo.setFields(fieldList);

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }


        responseListModel.setList(entryTempEntities);
        responseListModel.setTotalNum(entryTempEntities.size());
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
                                                    @RequestParam String translateType
    ) {
        ResponseListModel<EntryTempEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryTempEntity> entryTempEntities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_ALIAS, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                //将表的别名写入词条
                EntryTempEntity entryTempEntity = new EntryTempEntity();
                entryTempEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                entryTempEntity.setEntry(tdbTableInfo.getAlias());

                entryTempEntity.setSource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName );
                entryTempEntity.setAuditState(0);
                entryTempEntity.setVersionID(versionID);
                entryTempEntity.setTaskId(taskID);
                entryTempEntity.setTranslateType(translateType);
                entryTempEntity.setImportype(ConstantInterface.DB);
                entryTempEntities.add(entryTempEntity);

                //写表下的别名
                 List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                 for (TDBFieldInfo fieldInfo : fields){
                     EntryTempEntity fieldEntryTemp = new EntryTempEntity();
                     fieldEntryTemp.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                     fieldEntryTemp.setEntry(fieldInfo.getAliasName());
                     fieldEntryTemp.setSource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName  + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName() );
                     fieldEntryTemp.setAuditState(0);
                     fieldEntryTemp.setVersionID(versionID);
                     fieldEntryTemp.setTaskId(taskID);
                     fieldEntryTemp.setTranslateType(translateType);
                     fieldEntryTemp.setImportype(ConstantInterface.DB);
                     entryTempEntities.add(fieldEntryTemp);
                 }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        responseListModel.setList(entryTempEntities);
        responseListModel.setTotalNum(entryTempEntities.size());
        return checkResult(responseListModel);
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
}