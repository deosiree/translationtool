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
    public String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType,boolean tag,boolean common,String i18nUrl) {

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
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
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
        //写入i18 ts
        if (!CollectionUtils.isEmpty(tsEntryInfoMap)) {
           for (String fileName : tsEntryInfoMap.keySet()){
               JSONObject jsonObject = new JSONObject();
               jsonObject.put("entry", tsEntryInfoMap.get(fileName));
               String s = httpUtils.post(i18nUrl + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
           }
           // jsonObject.put("entry", tsEntryInfoMap);
           // String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
        }
        if (!CollectionUtils.isEmpty(diTypeMap)) {
            //按照分裂写入di
            for (String di_fileName : diTypeMap.keySet()) {
                //writeDiWords(di_fileName, translateType, dbEntrInfo);
                diUtils.writeDiEntry(diTypeMap.get(di_fileName),di_fileName,translateType,tag,common,i18nUrl);
            }

        }


        return ConstantInterface.OK_STR;
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
