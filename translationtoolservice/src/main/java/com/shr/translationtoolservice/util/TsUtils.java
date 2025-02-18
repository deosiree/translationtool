package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName TsUtils

 * @USER: Cola
 * @Date 2024/3/28 0028 10:26
 **/
@Component
public class TsUtils {
    @Autowired
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private TLanguageMapper languageMapper;

    //辞典更新 不需要tag 和comment 的更新和查询传入前先清空字段内容
    public String writeTSEntry(List<EntryInfoEntity> entryInfoEntities, String fileName,boolean tag,String i18nUrl,String transType){
        JSONObject jsonObject = new JSONObject();
        List<Map<String, String>> list = new ArrayList<>();
        String trans = "";
        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }
        for (EntryInfoEntity entryInfoEntity1 : entryInfoEntities){
            Map<String, String> requestMap = new HashMap<>();
            /*if (!tag) {
                entryInfoEntity1.setTag("");
            }*/
            requestMap.put("source", entryInfoEntity1.getEntry());
            requestMap.put("tag", entryInfoEntity1.getTag());
            requestMap.put("comment", entryInfoEntity1.getComment());
            List<TLanguage> tLanguages = languageMapper.selectLaguageByName(transType);
            fileName = fileName + "_" + tLanguages.get(0).getCode() + ".ts";
            switch (transType) {
                case ConstantInterface.ENGLISH:
                    trans = entryInfoEntity1.getEnglish();
                    break;
                case ConstantInterface.SPANISH:
                    trans = entryInfoEntity1.getSpanish();
                    break;
                case ConstantInterface.FRENCH:
                    trans = entryInfoEntity1.getFrench();
                    break;
                case ConstantInterface.RUSSIAN:
                    trans = entryInfoEntity1.getRussian();
                    break;
            }
            requestMap.put("translate", trans);
            list.add(requestMap);
        }


        jsonObject.put("entry", list);
        String s = httpUtils.post(i18nUrl + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
        if (StringUtils.isBlank(s) || !s.equals("true")){
            ErrorCodeList.setErrorCodeList(ErrorCodeList.UPDATE_TS_ERROR +fileName);
            return ErrorCodeList.UPDATE_TS_ERROR +fileName;
        }
        return ConstantInterface.OK_STR;
    }
}
