package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    public void writeTSEntry(List<EntryInfoEntity> entryInfoEntities, String fileName,boolean tag) {
        JSONObject jsonObject = new JSONObject();
        for (EntryInfoEntity entryInfoEntity1 : entryInfoEntities){
            if (!tag) {
                entryInfoEntity1.setEntryLabel("");
            }
        }
        jsonObject.put("entry", entryInfoEntities);
        String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);

    }
}
