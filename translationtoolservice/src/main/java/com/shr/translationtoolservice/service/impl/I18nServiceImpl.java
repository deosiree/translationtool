package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.entity.vo.TDBFieldInfo;
import com.shr.translationtoolservice.entity.vo.TDBTableInfo;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.util.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @ClassName I18nServiceImpl
 * @Description i18
 * @USER: Cola
 * @Date 2024/3/20 0020 15:18
 **/
@Service
@Slf4j
public class I18nServiceImpl implements I18nService {

    @Autowired
    private DiUtils diUtils;
    @Autowired
    private TsUtils tsUtils;

    @Autowired
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;
    @Autowired
    private TranslateMapper translateMapper;
    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TLanguageMapper languageMapper;

    @Override
    public String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities1, String translateType,boolean tag,boolean common,String i18nUrl) {

        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }
        //先将词条分类，写到不同的 地方
        List<EntryInfoEntity> dbEntrInfo = new ArrayList<>();
        List<EntryInfoEntity> diEntryInfo = new ArrayList<>();
        List<EntryInfoEntity> enumEntryInfo = new ArrayList<>();
        List<EntryInfoEntity> configEntryInfo = new ArrayList<>();
       // String fileName = "";
          Map<String,List<Map<String, String>>> tsEntryInfoMap = new HashMap<>();

        //di分组   fileName -> list
        Map<String, List<EntryInfoEntity>> diTypeMap = new HashMap<>();
        Map<String, List<EntryInfoEntity>> dbTypeMap = new HashMap<>();
        Map<String, List<EntryInfoEntity>> configTypeMap = new HashMap<>();
        Map<String, List<EntryInfoEntity>> enumTypeMap = new HashMap<>();
        if (CollectionUtils.isEmpty(entryInfoEntities1)) {
            return ConstantInterface.OK_STR;
        }
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities1) {
            if (StringUtils.isBlank(entryInfoEntity.getTag())) {
                entryInfoEntity.setTag("");
            }
            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                entryInfoEntities.addAll(entryInfoEntity.getChildren());
            }
            entryInfoEntities.add(entryInfoEntity);
        }

        //开线程
        processEntryInfo(entryInfoEntities, translateType, tag, common, tsEntryInfoMap, diTypeMap);

        /*for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            //取翻译状态为3的词条

            String trans = getTransByType(entryInfoEntity, translateType);
            if (StringUtils.isBlank(trans)) {
                continue;
            }
            if ("TS".equals(entryInfoEntity.getWriteType())) {
                if (CollectionUtils.isEmpty(tsEntryInfoMap.get(entryInfoEntity.getEntrySource()))){
                    List<Map<String, String>> tsEntrys = new ArrayList<>();
                    //遍历单词
                    Map<String, String> requestMap  = new HashMap<>();
                    requestMap.put("source", entryInfoEntity.getEntry());
                    String tagStr =  entryInfoEntity.getTag();
                   *//* if (StringUtils.isNotBlank(entryInfoEntity.getTag())){
                        tagStr =  entryInfoEntity.getTag();
                    }*//*
                    requestMap.put("tag", tagStr);
                    requestMap.put("comment", entryInfoEntity.getComment());
                    requestMap.put("translate", trans);
                    tsEntrys.add(requestMap);
                    tsEntryInfoMap.put(entryInfoEntity.getEntrySource(),tsEntrys);
                }else {
                    //遍历单词
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("source", entryInfoEntity.getEntry());
                    String tagStr =  entryInfoEntity.getTag();
                    *//*if (StringUtils.isNotBlank(entryInfoEntity.getTag())){
                        tagStr =  entryInfoEntity.getTag();
                    }*//*
                    requestMap.put("tag", tagStr);
                    requestMap.put("comment", entryInfoEntity.getComment());
                    requestMap.put("translate", trans);
                    List<Map<String, String>> list = tsEntryInfoMap.get(entryInfoEntity.getEntrySource());
                    list.add(requestMap);
                }

            } else if ("DI".equals(entryInfoEntity.getWriteType())) {
                //di 来源处理
                List<EntryInfoEntity> entities;
                if (CollectionUtils.isEmpty(diTypeMap.get(entryInfoEntity.getDiFileName()))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    diTypeMap.put(entryInfoEntity.getDiFileName(), entities);
                } else {
                    entities = diTypeMap.get(entryInfoEntity.getDiFileName());
                    for (EntryInfoEntity entryInfoEntity1 : entities){
                        if (entryInfoEntity1.getEntry().equals(entryInfoEntity.getEntry())){
                            if (tag){
                                if (entryInfoEntity1.getTag().equals(entryInfoEntity.getTag())){
                                    continue;
                                }else {
                                    entities.add(entryInfoEntity);
                                    break;
                                }
                            }
                        }else {
                            entities.add(entryInfoEntity);
                            break;
                        }
                    }
                }
            }
        }*/
        StringBuilder builder = new StringBuilder();
            //写入i18 ts
            if (!CollectionUtils.isEmpty(tsEntryInfoMap)) {
                for (String fileName : tsEntryInfoMap.keySet()) {
                    List<TLanguage> tLanguages = languageMapper.selectLaguageByName(translateType);
                    //tsUtils.writeTSEntry(entryInfoEntities, fileName, tag,i18nUrl,translateType);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("entry", tsEntryInfoMap.get(fileName));
                    fileName = fileName + "_" + tLanguages.get(0).getCode() + ".ts";
                    String s = httpUtils.post(i18nUrl + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
                    if (StringUtils.isBlank(s)) {
                        return ErrorCodeList.I18N_SERVER_ERROR + fileName;
                    } else if (!s.equals("true")) {
                        builder.append("文件: " + fileName + "更新失败");
                        // ErrorCodeList.setErrorCodeList(ErrorCodeList.UPDATE_TS_ERROR + fileName);
                        // return ErrorCodeList.UPDATE_TS_ERROR + fileName;
                    }
                }
                // jsonObject.put("entry", tsEntryInfoMap);
                // String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
            }
            if (!CollectionUtils.isEmpty(diTypeMap)) {
                //按照分裂写入di
                for (String di_fileName : diTypeMap.keySet()) {
                    //writeDiWords(di_fileName, translateType, dbEntrInfo);
                     String res = diUtils.writeDiEntry(diTypeMap.get(di_fileName), di_fileName, translateType, tag, common, i18nUrl);
                    if (!res.equals(ConstantInterface.OK_STR)) {
                        return res;
                    }
                }

            }

        String extrasInfos = builder.toString();
        return extrasInfos.isEmpty()? ConstantInterface.OK_STR  : "部分更新成功" + "额外信息: " + extrasInfos;
    }

    private void processEntryInfo(List<EntryInfoEntity> entryInfoEntities, String translateType, boolean tag
            , boolean common, Map<String, List<Map<String, String>>> tsEntryInfoMap, Map<String, List<EntryInfoEntity>> diTypeMap) {
        int numberOfThreads = 32; // Number of threads to use
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        int chunkSize = entryInfoEntities.size() / numberOfThreads;
        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numberOfThreads - 1) ? entryInfoEntities.size() : (i + 1) * chunkSize;
            List<EntryInfoEntity> sublist = entryInfoEntities.subList(start, end);

            new Thread(() -> {
                try {
                    // Process the sublist
                    for (EntryInfoEntity entryInfoEntity : sublist) {
                        // Your processing logic here
                        //取翻译状态为3的词条

                        String trans = getTransByType(entryInfoEntity, translateType);
                        if (StringUtils.isBlank(trans)) {
                            continue;
                        }
                        if ("TS".equals(entryInfoEntity.getWriteType())) {
                            if (CollectionUtils.isEmpty(tsEntryInfoMap.get(entryInfoEntity.getEntrySource()))){
                                List<Map<String, String>> tsEntrys = new ArrayList<>();
                                //遍历单词
                                Map<String, String> requestMap  = new HashMap<>();
                                requestMap.put("source", entryInfoEntity.getEntry());
                                String tagStr =  entryInfoEntity.getTag();
                   /* if (StringUtils.isNotBlank(entryInfoEntity.getTag())){
                        tagStr =  entryInfoEntity.getTag();
                    }*/
                                requestMap.put("tag", tagStr);
                                requestMap.put("comment", entryInfoEntity.getComment());
                                requestMap.put("translate", trans);
                                tsEntrys.add(requestMap);
                                tsEntryInfoMap.put(entryInfoEntity.getEntrySource(),tsEntrys);
                            }else {
                                //遍历单词
                                Map<String, String> requestMap = new HashMap<>();
                                requestMap.put("source", entryInfoEntity.getEntry());
                                String tagStr =  entryInfoEntity.getTag();
                    /*if (StringUtils.isNotBlank(entryInfoEntity.getTag())){
                        tagStr =  entryInfoEntity.getTag();
                    }*/
                                requestMap.put("tag", tagStr);
                                requestMap.put("comment", entryInfoEntity.getComment());
                                requestMap.put("translate", trans);
                                List<Map<String, String>> list = tsEntryInfoMap.get(entryInfoEntity.getEntrySource());
                                list.add(requestMap);
                            }

                        } else if ("DI".equals(entryInfoEntity.getWriteType())) {
                            String diFileName = entryInfoEntity.getDiFileName();
                            if(StringUtils.isBlank(diFileName)){
                                continue;
                            }
                            //di 来源处理
                            List<EntryInfoEntity> entities;
                            if (CollectionUtils.isEmpty(diTypeMap.get(diFileName))) {
                                entities = new ArrayList<>();
                                entities.add(entryInfoEntity);
                                diTypeMap.put(diFileName, entities);
                            } else {
                                entities = diTypeMap.get(diFileName);
                                for (EntryInfoEntity entryInfoEntity1 : entities){
                                    if (entryInfoEntity1.getEntry().equals(entryInfoEntity.getEntry())){
                                            if(StringUtils.isBlank(entryInfoEntity1.getTag())){
                                                entryInfoEntity1.setTag("");
                                            }
                                            if(StringUtils.isBlank(entryInfoEntity1.getComment())){
                                                entryInfoEntity1.setComment("");
                                            }

                                            if (entryInfoEntity1.getTag().equals(entryInfoEntity.getTag()) && entryInfoEntity1.getComment().equals(entryInfoEntity.getComment())){
                                                continue;
                                            }else {
                                                entities.add(entryInfoEntity);
                                                break;
                                            }
                                    }else {
                                        entities.add(entryInfoEntity);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(); // Wait for all threads to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }






    }

    @Override
    public List<DictionaryVo> getDictory(String entry, String tag, String common,String fileName,String i18nUrl) {
        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<DictionaryVo> list = new ArrayList<>();
        Map<String, String> requestMap  = new HashMap<>();
        requestMap.put("name", fileName);
        try {
            s = httpUtils.get(i18nUrl + ConstantInterface.GET_INIT_DICTIONARY ,requestMap);
            if (StringUtils.isBlank(s)){
                return list;
            }
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                boolean a = false;
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                if (dictionaryVo.getSource().equals("初始化词条")){
                    continue;
                }
                if (StringUtils.isNotBlank(entry) ){
                    if (! dictionaryVo.getSource().contains(entry)){
                      continue;
                    }
                }
                if (StringUtils.isNotBlank(tag)){
                    if (! dictionaryVo.getTag().contains(tag)){
                        continue;
                    }
                }
                if (StringUtils.isNotBlank(common)){
                    if (! dictionaryVo.getComments().contains(common)){
                        continue;
                    }
                }
                list.add(dictionaryVo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return list;
        }
        return list;
    }

    @Override
    public List<EntryInfoEntity> getDBEntryBySource(String i18nUrl, Set<String> dbSet, String taskID, String userName, String productId, String versionID, String translateType) {
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        Date date = new Date(System.currentTimeMillis());
        for (String dbSec : dbSet) {
            String[] split = dbSec.split(ConstantInterface.UNDERLINE);
            if (split.length != 4) {
                continue;
            }
            String nodeName = dbSec.split(ConstantInterface.UNDERLINE)[0];
            String appName = dbSec.split(ConstantInterface.UNDERLINE)[1];
            String dbName = dbSec.split(ConstantInterface.UNDERLINE)[2];
            String tbName = dbSec.split(ConstantInterface.UNDERLINE)[3];
            if (StringUtils.isBlank(i18nUrl)) {
                i18nUrl = I18URL;
            }
            JSONArray jsonArray = new JSONArray();
            String s = "";
            try {
                Map<String, String> headerParameters = new HashMap<>();
                headerParameters.put("dbName", dbName);
                headerParameters.put("appName", appName);
                headerParameters.put("nodeName", nodeName);
                s = httpUtils.get(i18nUrl + ConstantInterface.GET_ALIAS, headerParameters);
                jsonArray = JSONArray.parseArray(s);

                for (int i = 0; i < jsonArray.size(); i++) {
                    String source = nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName;
                    TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);
                    source = source + ConstantInterface.UNDERLINE  + tdbTableInfo.getCommon();
                    if (StringUtils.isNotBlank(tdbTableInfo.getAliasName())) {
                        //将表的别名写入词条
                        EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                        entryInfoEntity.setId(commonUtils.getUUID());
                        entryInfoEntity.setEntry(tdbTableInfo.getAliasName());
                        entryInfoEntity.setDiFileName("db/" + dbName);
                        entryInfoEntity.setEntrySource(source);
                        //  entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                        entryInfoEntity.setEntryState(1);
                        entryInfoEntity.setVersionID(versionID);
                        entryInfoEntity.setTaskId(taskID);
                        entryInfoEntity.setIsDelete(0);
                        entryInfoEntity.setImportType(ConstantInterface.DB);
                        entryInfoEntity.setWriteType(ConstantInterface.DI);
                        entryInfoEntity.setProductID(productId);
                        entryInfoEntities.add(entryInfoEntity);
                    }


                    //写表下的别名
                    List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                    for (TDBFieldInfo fieldInfo : fields) {
                        if (StringUtils.isBlank(fieldInfo.getAliasName())) {
                            continue;
                        }
                        EntryInfoEntity fieldEntry = new EntryInfoEntity();
                        fieldEntry.setId(commonUtils.getUUID());
                        fieldEntry.setEntry(fieldInfo.getAliasName());
                        fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                        fieldEntry.setEntryState(1);
                        fieldEntry.setDiFileName("db/" + dbName);
                        fieldEntry.setEntrySource(source + "_" + tdbTableInfo.getTableName());
                        fieldEntry.setIsDelete(0);
                        fieldEntry.setUpdateTime(date);
                        fieldEntry.setUpdate(userName);
                        fieldEntry.setProductID(productId);
                        fieldEntry.setImportType(ConstantInterface.DB);
                        fieldEntry.setWriteType(ConstantInterface.DI);
                        entryInfoEntities.add(fieldEntry);
                    }
                }

                log.info(" start send http request : " + i18nUrl + ConstantInterface.GET_APP_BYNODE);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

           // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
            //entryInfoEntities.addAll(entryInfoMapper.getDBEntryBySource(i18nUrl, dbName, taskID, userName, productId, versionID, translateType));
        }

        return entryInfoEntities;
       // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
    }

    @Override
    public List<EntryInfoEntity> getENUMEntryBySource(String i18nUrl, Set<String> fileSet,  String taskID, String userName, String productId, String versionID, String translateType) throws Exception {
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();

        Date date = new Date(System.currentTimeMillis());


        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }

            for (String fileName : fileSet) {
                if (fileName.contains("/")){
                    fileName = fileName.split("/")[1];
                }
                JSONArray jsonArray = new JSONArray();
                String s = "";
                Map<String, String> headerParameters = new HashMap<>();
                headerParameters.put("fileName", fileName);
                s = httpUtils.get(i18nUrl + ConstantInterface.GET_ENUM_ENTRY, headerParameters);
                jsonArray = JSONArray.parseArray(s);
                for (int i = 0; i < jsonArray.size(); i++) {
                    TDBFieldInfo fieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);

                    //将表的别名写入词条
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID());
                    entryInfoEntity.setEntry(fieldInfo.getFieldName());
                    entryInfoEntity.setDiFileName("enum/" + fieldInfo.getDb_name());
                    entryInfoEntity.setEntrySource(fieldInfo.getCommon());
                    //entryInfoEntity.setTag(fieldInfo.getCommon());
                    entryInfoEntity.setUpdateTime(date);
                    entryInfoEntity.setUpdate(userName);
                    // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setImportType(ConstantInterface.ENUM);
                    entryInfoEntity.setWriteType(ConstantInterface.DI);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntity.setIsDelete(0);
                    entryInfoEntities.add(entryInfoEntity);


                }
            }
            log.info(" start send http request : " + i18nUrl + ConstantInterface.GET_ENUM_ENTRY);

        return entryInfoEntities;
    }

    @Override
    public List<EntryInfoEntity> getCONFIGEntryBySource(String i18nUrl, Set<String> configfileSet, String taskID, String userName, String productId, String versionID, String translateType) throws Exception {

        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();

        Date date = new Date(System.currentTimeMillis());

        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }

            for (String fileName : configfileSet){
                JSONArray jsonArray = new JSONArray();
                String s = "";
                Map<String, String> headerParameters = new HashMap<>();
                headerParameters.put("fileName", fileName);
                s = httpUtils.get(i18nUrl + ConstantInterface.GET_CONGIF_ENTRY,headerParameters);
                jsonArray = JSONArray.parseArray(s);
                for (int i = 0; i < jsonArray.size(); i++) {
                    TDBFieldInfo fieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);

                    //将表的别名写入词条
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID());
                    entryInfoEntity.setEntry(fieldInfo.getFieldName());
                    //String[] split = fieldInfo.getDb_name().split("/");
                    String replace = fieldInfo.getDb_name().replace("/", "_");
                    entryInfoEntity.setDiFileName("config/" + replace);
                    //entryInfoEntity.setDiFileName("config/" + fieldInfo.getDb_name().split("\\.")[0]);
                    entryInfoEntity.setEntrySource(fieldInfo.getCommon());
                    // entryInfoEntity.setTag(fieldInfo.getCommon());
                    // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setUpdateTime(date);
                    entryInfoEntity.setUpdate(userName);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setIsDelete(0);

                    entryInfoEntity.setImportType(ConstantInterface.CONFIG);
                    entryInfoEntity.setWriteType(ConstantInterface.DI);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntities.add(entryInfoEntity);


                }
            }
            log.info(" start send http request : " + i18nUrl + ConstantInterface.GET_APP_BYNODE);



        return entryInfoEntities;
    }


    private String getTransByType(EntryInfoEntity entryInfoEntity, String translateType) {
        String transState = "";
        String trans = "";
        TranslateEntity translateEntity = new TranslateEntity();
        switch (translateType) {
            case ConstantInterface.CHINESE:
                translateEntity = translateMapper.selectById(entryInfoEntity.getZhTransId());
                break;
            case ConstantInterface.ENGLISH:
                 translateEntity = translateMapper.selectById(entryInfoEntity.getEnTransId());
                break;
            case ConstantInterface.RUSSIAN:
                translateEntity = translateMapper.selectById(entryInfoEntity.getRuTransId());
                break;
            case ConstantInterface.SPANISH:
                translateEntity = translateMapper.selectById(entryInfoEntity.getSpaTransId());
                break;
            case ConstantInterface.FRENCH:
                translateEntity = translateMapper.selectById(entryInfoEntity.getFraTransId());
                break;
        }
        if (translateEntity == null) {
            return "";
        }
        transState = translateEntity.getTranslateState();
        trans = translateEntity.getTranslate();
        if (!"3".equals(transState)) {
            return "";
        }
        return trans;
    }
}
