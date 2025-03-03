package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.util.DiUtils;
import com.shr.translationtoolservice.util.HTTPUtils;
import com.shr.translationtoolservice.util.TsUtils;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                        ErrorCodeList.setErrorCodeList(ErrorCodeList.UPDATE_TS_ERROR + fileName);
                        return ErrorCodeList.UPDATE_TS_ERROR + fileName;
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


        return ConstantInterface.OK_STR;
    }

    private void processEntryInfo(List<EntryInfoEntity> entryInfoEntities, String translateType, boolean tag, boolean common, Map<String, List<Map<String, String>>> tsEntryInfoMap, Map<String, List<EntryInfoEntity>> diTypeMap) {
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


    private String getTransByType(EntryInfoEntity entryInfoEntity, String translateType) {
        String transState = "";
        String trans = "";
        TranslateEntity translateEntity = new TranslateEntity();
        switch (translateType) {
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
