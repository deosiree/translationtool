package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.exception.i18nServerConnectException;
import com.shr.translationtoolservice.exception.i18nServerParseException;

import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @ClassName TsUtils

 * @USER: Cola
 * @Date 2024/3/28 0028 10:26
 **/
@Component
public class TsUtils {
    private static final Logger log = LoggerFactory.getLogger(TsUtils.class);
    @Autowired
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private EntryUtils entryUtils;

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
                case ConstantInterface.CHINESE:
                    trans = entryInfoEntity1.getChinese();
                    break;
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

    public static class TsEntryResult<E>{
        List<EntryInfoEntity> result = new ArrayList<>();

        List<String> exceptionMessage =  new ArrayList<>();

        public Map<Exception,Set<E>> exceptionCollection = new HashMap<>();

        public TsEntryResult(){}

        public List<EntryInfoEntity> getResult() {
            return result;
        }

        public List<String> getExceptionMessage() {
            return exceptionMessage;
        }

        public Map<Exception, Set<E>> getExceptionCollection() {
            return exceptionCollection;
        }


    }

    public TsEntryResult<String> getTsEntry(String i18nUrl, Set<String> fileNameSet,
                                            String taskID,String userName,String productId,String versionID,String translateType) throws Exception {
        TsEntryResult<String> result = new TsEntryResult<>();
        List<TLanguage> tLanguages = languageMapper.selectList(new QueryWrapper<>());
        List<EntryInfoEntity> list = result.getResult();
        List<String> exceptionMessage = result.getExceptionMessage();
        Map<Exception,Set<String>> exceptionCollection = result.getExceptionCollection();
        if (CollectionUtils.isEmpty(fileNameSet)) {
            return result;
        }
        Date date = new Date(System.currentTimeMillis());
        for (String fileName : fileNameSet) {
            String entrySource = fileName;
            for (TLanguage tLanguage : tLanguages) {
                if (translateType.equals(tLanguage.getName())) {
                    fileName =fileName + "_" + tLanguage.getCode() + ".ts";
                }
            }
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("fileName", fileName);

            JSONArray jsonArray = new JSONArray();
            String s = "";

                log.info("getTsEntry: " + i18nUrl + ConstantInterface.GET_WORDS + " fileName : " + fileName);

                try {
                    try {
                        s = httpUtils.get(i18nUrl + ConstantInterface.GET_WORDS, headerParameters);
                    } catch (Exception e) {
                        // TODO: handle exception
                        throw new i18nServerConnectException();
                    }
                    try {
                        jsonArray = JSONArray.parseArray(s);                         
                    } catch (Exception e) {
                        // TODO: handle exception
                        throw new i18nServerParseException();
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    // result.
                    if(!exceptionCollection.containsKey(e)){
                        exceptionCollection.put(e, new HashSet<>());
                    }
                    exceptionCollection.get(e).add(fileName);
                    
                    // exceptionCollection.put(e.getClass().getName(), fileName);
                    // exceptionMessage.add("当前读取文件: \"" + fileName +"\"" + "时发生异常,请检查网络服务是否正常, 该文件是否存在, 文件内容是否存在异常");
                    continue;
                }
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONArray jsonArray1 = JSONArray.parseArray(jsonArray.get(i).toString());
                    String entry = jsonArray1.get(0).toString();
                    if (entry.length() > 1024) {
                       continue;
                    }
                    String translate = jsonArray1.getString(1);
                    String tag = jsonArray1.getString(2);
                    String comment = jsonArray1.getString(3);
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID());
                    //为了支持多语言，fileName截取前面一段作为entrySource  ,例：db_selector_comp_en_US.ts 截取db_selector_comp


                    entryInfoEntity.setEntrySource(entrySource);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setEntry(entry);
                    entryInfoEntity.setUpdate(userName);
                    entryInfoEntity.setUpdateTime(date);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntity.setTag(tag);
                    entryInfoEntity.setComment(comment);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setImportType(ConstantInterface.TS);
                    entryInfoEntity.setWriteType(ConstantInterface.TS);
                    entryInfoEntity.setIsDelete(0);
                    entryUtils.createNewTrans(entryInfoEntity, translateType, translate);
                    list.add(entryInfoEntity);
                }



        }
        return result;
    }
}
