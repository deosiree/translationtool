package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName DiUtils
 * @Description 辞典工具
 * @USER: Cola
 * @Date 2024/3/20 0020 14:49
 **/
@Component
@Slf4j
public class DiUtils {
    @Autowired
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private TLanguageMapper languageMapper;

    //辞典更新 不需要tag 和comment 的更新和查询传入前先清空字段内容
    public void writeDiEntry(List<EntryInfoEntity> entryInfoEntities, String fileName, String translateType,boolean tag,boolean common) {
        //获取字典所有词条，将对应词条的对应翻译插入其中 ，再全量写入
        String s = "";
        String s2 = "";
        JSONArray jsonArray;
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + fileName);
            jsonArray = JSONArray.parseArray(s);
            List<DictionaryVo> dictionaryVos = new ArrayList<>();
            constructDIVO(jsonArray, dictionaryVos);
            String langCode = languageMapper.selectLaguageByName(translateType).get(0).getCode();
            int add = 0;
            int update = 0 ;
            int sum =0;
            for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
                if (!tag) {
                    entryInfoEntity.setEntryLabel("");
                }
                if (!common) {
                    entryInfoEntity.setEntrySource("");
                }
                update++;
                boolean isExist = updateTransToDiVo(langCode, entryInfoEntity, translateType, dictionaryVos);
                //如果不存在 则新增词条
                if (!isExist) {
                    add ++;
                    addEntryToDIVo(langCode, entryInfoEntity, translateType, dictionaryVos);
                }
            }

            String dictionaryVosStr = JSONObject.toJSONString(dictionaryVos);
             sum = add+update;
            //10.16.193.63:18099/dictionary/user
            s2 = httpUtils.post(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + fileName, dictionaryVosStr);
            log.info(" ==== 辞典 " + fileName + " 新增词条 ： " + add + " **** 更新词条 ：" + update + " ****  sum is :" + sum + " ==== ");

        } catch (Exception e) {
            log.error(" 请求失败 URL ： " + I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT);
            e.printStackTrace();
        }

    }

    private void addEntryToDIVo(String langCode, EntryInfoEntity entryInfoEntity, String translateType, List<DictionaryVo> dictionaryVos) {
        DictionaryVo dictionaryVo = new DictionaryVo();
        dictionaryVo.setSource(entryInfoEntity.getEntry());
        dictionaryVo.setComments(entryInfoEntity.getEntrySource());

        dictionaryVo.setTag(entryInfoEntity.getEntryLabel());

        // dictionaryVo.setComments(entryInfoEntity.getEntrySource().split("_")[2]);
        // dictionaryVo.setTag(entryInfoEntity.getEntryLabel());
        Map<String, String> transMap = new HashMap<>();
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                    transMap.put(langCode, entryInfoEntity.getEnglish());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                    transMap.put(langCode, entryInfoEntity.getRussian());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                    transMap.put(langCode, entryInfoEntity.getSpanish());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                    transMap.put(langCode, entryInfoEntity.getFrench());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
        }

        dictionaryVos.add(dictionaryVo);
    }

    private boolean updateTransToDiVo(String langCode, EntryInfoEntity entryInfoEntity, String translateType, List<DictionaryVo> dictionaryVos) {
        boolean isExist = false;

        for (DictionaryVo dictionaryVo : dictionaryVos) {
            if (dictionaryVo.getSource().equals(entryInfoEntity.getEntry())) {
                //是否需要tag 查询
                if (!entryInfoEntity.getEntryLabel().equals(dictionaryVo.getTag())) {
                   continue;
                }

                //是否需要common 查询

                if (!entryInfoEntity.getEntrySource().equals(dictionaryVo.getComments())) {
                    continue;
                }
                isExist = true;

                Map<String, String> transMap = dictionaryVo.getTranslation();
                if (CollectionUtils.isEmpty(transMap)) {
                    transMap = new HashMap<>();
                }
                switch (translateType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                            transMap.put(langCode, entryInfoEntity.getEnglish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                            transMap.put(langCode, entryInfoEntity.getRussian());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                            transMap.put(langCode, entryInfoEntity.getSpanish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                            transMap.put(langCode, entryInfoEntity.getFrench());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                }
            }


        }
        return isExist;
    }

    private void constructDIVO(JSONArray jsonArray, List<DictionaryVo> dictionaryVos) {
        for (int i = 0; i < jsonArray.size(); i++) {
            DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
            dictionaryVos.add(dictionaryVo);
        }
    }
}
