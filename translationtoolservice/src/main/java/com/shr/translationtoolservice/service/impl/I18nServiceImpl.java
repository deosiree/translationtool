package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.util.DiUtils;
import com.shr.translationtoolservice.util.HTTPUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Override
    public String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType,boolean tag,boolean common) {


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
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            //取翻译状态为3的词条
            String trans = getTransByType(entryInfoEntity, translateType);
            if (StringUtils.isBlank(trans)) {
                continue;
            }
            if ("TS".equals(entryInfoEntity.getImportType())) {
                if (CollectionUtils.isEmpty(tsEntryInfoMap.get(entryInfoEntity.getEntrySource()))){
                    List<Map<String, String>> tsEntrys = new ArrayList<>();
                    //遍历单词
                    Map<String, String> requestMap  = new HashMap<>();
                    requestMap.put("source", entryInfoEntity.getEntry());
                    requestMap.put("tag", entryInfoEntity.getEntryLabel());
                    requestMap.put("translate", trans);
                    tsEntrys.add(requestMap);
                    tsEntryInfoMap.put(entryInfoEntity.getEntrySource(),tsEntrys);
                }else {
                    //遍历单词
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("source", entryInfoEntity.getEntry());
                    requestMap.put("tag", entryInfoEntity.getEntryLabel());
                    requestMap.put("translate", trans);
                    List<Map<String, String>> list = tsEntryInfoMap.get(entryInfoEntity.getEntrySource());
                    list.add(requestMap);
                }

             /*   //遍历单词
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("source", entryInfoEntity.getEntry());
                requestMap.put("tag", entryInfoEntity.getEntryLabel());
                requestMap.put("translate", trans);
                tsEntrys.add(requestMap);
                tsEntryInfoMap.put(entryInfoEntity.getEntrySource(),requestMap);*/
            } else if ("DI".equals(entryInfoEntity.getImportType())) {
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
                                    break;
                                }else {
                                    diEntryInfo.add(entryInfoEntity);
                                }
                            }
                        }else {
                            diEntryInfo.add(entryInfoEntity);
                        }
                    }

                }


            } else if ("DB".equals(entryInfoEntity.getImportType())) {
                //dB 来源处理
                //最后一位是写入DI 的文件名
                // String[] s = entryInfoEntity.getDiFileName().split("_");
                String diFileName = entryInfoEntity.getDiFileName();
                List<EntryInfoEntity> entities;
                //预处理
                if (!tag){
                    entryInfoEntity.setEntryLabel("");
                }
                if (common){
                    //库名
                    entryInfoEntity.setEntrySource("DB_" + entryInfoEntity.getEntrySource().split("_")[2]);
                }else {
                    entryInfoEntity.setEntrySource("");
                }
                if (CollectionUtils.isEmpty(dbTypeMap.get(diFileName))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    dbTypeMap.put(diFileName, entities);
                } else {
                    entities = dbTypeMap.get(diFileName);
                    for (EntryInfoEntity entryInfoEntity1 : entities){
                        if (entryInfoEntity1.getEntry().equals(entryInfoEntity.getEntry())){
                            if (tag){
                                if (!entryInfoEntity1.getTag().equals(entryInfoEntity.getTag())){
                                    diEntryInfo.add(entryInfoEntity);
                                }
                            }else if (common){
                                if (!entryInfoEntity1.getEntrySource().equals(entryInfoEntity.getEntrySource())){
                                    diEntryInfo.add(entryInfoEntity);
                                }
                            }
                        }else {
                            diEntryInfo.add(entryInfoEntity);
                        }
                    }
                    dbEntrInfo.add(entryInfoEntity);
                }



            } else if ("CONFIG".equals(entryInfoEntity.getImportType())) {
                //di 来源处理
                String diFileName = entryInfoEntity.getDiFileName();
                List<EntryInfoEntity> entities;
//预处理
                if (!tag){
                    entryInfoEntity.setEntryLabel("");
                }
                if (!common){
                    entryInfoEntity.setEntrySource("");
                }

                if (CollectionUtils.isEmpty(configTypeMap.get(diFileName))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    configTypeMap.put(diFileName, entities);
                } else {
                    entities = configTypeMap.get(diFileName);
                    entities.add(entryInfoEntity);
                }
                configEntryInfo.add(entryInfoEntity);

            }else if (ConstantInterface.ENUM.equals(entryInfoEntity.getImportType())) {
                //di 来源处理
                String diFileName = entryInfoEntity.getDiFileName();
                List<EntryInfoEntity> entities;
//预处理
                if (!tag){
                    entryInfoEntity.setEntryLabel("");
                }
                if (!common){
                    entryInfoEntity.setEntrySource("");
                }

                if (CollectionUtils.isEmpty(enumTypeMap.get(diFileName))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    enumTypeMap.put(diFileName, entities);
                } else {
                    entities = enumTypeMap.get(diFileName);
                    entities.add(entryInfoEntity);
                }
                enumEntryInfo.add(entryInfoEntity);
            }
        }
        //写入i18 ts
        if (!CollectionUtils.isEmpty(tsEntryInfoMap)) {
           for (String fileName : tsEntryInfoMap.keySet()){
               JSONObject jsonObject = new JSONObject();
               jsonObject.put("entry", tsEntryInfoMap.get(fileName));
               String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
           }
           // jsonObject.put("entry", tsEntryInfoMap);
           // String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
        }
        if (!CollectionUtils.isEmpty(diEntryInfo)) {
            //按照分裂写入di
            for (String di_fileName : diTypeMap.keySet()) {
                //writeDiWords(di_fileName, translateType, dbEntrInfo);
                diUtils.writeDiEntry(diTypeMap.get(di_fileName),di_fileName,translateType,tag,common);
            }

        }
        if (!CollectionUtils.isEmpty(dbEntrInfo)) {
            //按照分裂写入di
            for (String dbFileName : dbTypeMap.keySet()) {
                //没有的词条新增 已有的更新翻译
                // writeDbWords(dbFileName, translateType, dbEntrInfo, tag, common);
                diUtils.writeDiEntry(dbTypeMap.get(dbFileName),dbFileName,translateType,tag,common);
            }

        }
        if (!CollectionUtils.isEmpty(configEntryInfo)) {

            //按照分裂写入di
            for (String cfFileName : configTypeMap.keySet()) {
                //没有的词条新增 已有的更新翻译
                //writeConfigWords(cfFileName, translateType, configEntryInfo, tag, common);
                diUtils.writeDiEntry(configEntryInfo,cfFileName,translateType,tag,common);
            }
        }
        if (!CollectionUtils.isEmpty(enumEntryInfo)) {

            //按照分裂写入di
            for (String enumFileName : enumTypeMap.keySet()) {
                //没有的词条新增 已有的更新翻译
                //writeConfigWords(cfFileName, translateType, configEntryInfo, tag, common);
                diUtils.writeDiEntry(enumEntryInfo,enumFileName,translateType,tag,common);
            }
        }

        return ConstantInterface.OK_STR;
    }

    @Override
    public List<DictionaryVo> getDictory(String entry, String tag, String common,String fileName) {

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<DictionaryVo> list = new ArrayList<>();

        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + fileName);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                boolean a = false;
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                if (StringUtils.isNotBlank(entry) ){
                    if (! dictionaryVo.getSource().equals(entry)){
                      continue;
                    }
                }
                if (StringUtils.isNotBlank(tag)){
                    if (! dictionaryVo.getTag().equals(tag)){
                        continue;
                    }
                }
                if (StringUtils.isNotBlank(common)){
                    if (! dictionaryVo.getComments().equals(common)){
                        continue;
                    }
                }
                list.add(dictionaryVo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    private String getTransByType(EntryInfoEntity entryInfoEntity, String translateType) {
        String transState = "";
        String trans = "";
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                transState = entryInfoEntity.getEnglishTranslateState();
                trans = entryInfoEntity.getEnglish();
                break;
            case ConstantInterface.RUSSIAN:
                transState = entryInfoEntity.getRussianTranslateState();
                trans = entryInfoEntity.getRussian();
                break;
            case ConstantInterface.SPANISH:
                transState = entryInfoEntity.getSpanishTranslateState();
                trans = entryInfoEntity.getSpanish();
                break;
            case ConstantInterface.FRENCH:
                transState = entryInfoEntity.getFrenchTranslateState();
                trans = entryInfoEntity.getFrench();
                break;
        }
        if (!"3".equals(transState)) {
            return "";
        }
        return trans;
    }
}
