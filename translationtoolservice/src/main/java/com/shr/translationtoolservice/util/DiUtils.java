package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.entity.vo.TDBFieldInfo;
import com.shr.translationtoolservice.exception.i18nServerConnectException;
import com.shr.translationtoolservice.exception.i18nServerParseException;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

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

    @Autowired
    private CommonUtils commonUtils;

    //辞典更新 不需要tag 和comment 的更新和查询传入前先清空字段内容
    public String writeDiEntry(List<EntryInfoEntity> entryInfoEntities, String fileName, String translateType,boolean tag,boolean common,String i18nUrl) {
        //获取字典所有词条，将对应词条的对应翻译插入其中 ，再全量写入
        String s = "";
        String s2 = "";
        JSONArray jsonArray;
        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("name", fileName);
            s = httpUtils.get(i18nUrl + ConstantInterface.GET_INIT_DICTIONARY ,headerParameters);

            if (s.equals("501") || StringUtils.isBlank(s) || s.equals("null")){
                ErrorCodeList.setErrorCodeList(ErrorCodeList.GET_DIC_ERROR +fileName);
                return ErrorCodeList.GET_DIC_ERROR + fileName;
            }
            jsonArray = JSONArray.parseArray(s);
            Set<DictionaryVo> dictionaryVos = new HashSet<>();
            constructDIVO(jsonArray, dictionaryVos);
            String langCode = languageMapper.selectLaguageByName(translateType).get(0).getCode();
            int add = 0;
            int update = 0 ;
            int sum =0;
            for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
                if (StringUtils.isBlank(entryInfoEntity.getTag())){
                    entryInfoEntity.setTag("");
                }
                if (StringUtils.isBlank(entryInfoEntity.getComment())){
                    entryInfoEntity.setComment("");
                }
                //回写两份 带tag 和不带tag 的
                if (StringUtils.isNotBlank(entryInfoEntity.getTag())) {
                    EntryInfoEntity entryInfoEntity1 = new EntryInfoEntity();
                    BeanUtils.copyProperties(entryInfoEntity, entryInfoEntity1);
                    entryInfoEntity1.setTag("");
                    boolean isExist = updateTransToDiVo(tag,common,langCode, entryInfoEntity1, translateType, dictionaryVos);
                    //如果不存在 则新增词条
                    if (!isExist) {
                        add ++;
                        addEntryToDIVo(langCode, entryInfoEntity1, translateType, dictionaryVos);
                    }
                }
            /*
                if (!tag) {
                    entryInfoEntity.setTag("");
                }
                if (!common) {
                    entryInfoEntity.setComment("");
                }*/
                update++;
                boolean isExist = updateTransToDiVo(tag,common,langCode, entryInfoEntity, translateType, dictionaryVos);
                //如果不存在 则新增词条
                if (!isExist) {
                    add ++;
                    addEntryToDIVo(langCode, entryInfoEntity, translateType, dictionaryVos);
                }
            }
            //dictionaryVos 根据词条、翻译、tag、common进行去重
            String dictionaryVosStr = JSONObject.toJSONString(dictionaryVos);
            headerParameters.put("dictionaryInfo", dictionaryVosStr);
            headerParameters.put("dictionary", fileName);
            String req =  JSONObject.toJSONString(headerParameters);
             sum = add+update;
            //10.16.193.63:18099/dictionary/user
            s2 = httpUtils.post(i18nUrl + ConstantInterface.UPDTAE_DICTIONARY , req);
            if (StringUtils.isBlank(s2) ){
                ErrorCodeList.setErrorCodeList(ErrorCodeList.UPDATE_DIC_ERROR +fileName);
                return ErrorCodeList.UPDATE_DIC_ERROR + fileName;
            }
            log.info(" ==== " + i18nUrl +" 辞典 " + fileName + " 新增词条 ： " + add + " **** 更新词条 ：" + update + " ****  sum is :" + sum + " ==== ");

        } catch (Exception e) {
            log.error(" 请求失败 URL ： " + i18nUrl + ConstantInterface.UPDTAE_DICTIONARY + ConstantInterface.SPRIT);
            log.error(e.getMessage(),e);
            return ErrorCodeList.I18N_SERVER_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    private void addEntryToDIVo(String langCode, EntryInfoEntity entryInfoEntity, String translateType, Set<DictionaryVo> dictionaryVos) {
        DictionaryVo dictionaryVo = new DictionaryVo();
        dictionaryVo.setSource(entryInfoEntity.getEntry());
        dictionaryVo.setComments(entryInfoEntity.getComment());

        dictionaryVo.setTag(entryInfoEntity.getTag());

        // dictionaryVo.setComments(entryInfoEntity.getEntrySource().split("_")[2]);
        // dictionaryVo.setTag(entryInfoEntity.getEntryLabel());
        Map<String, String> transMap = new HashMap<>();
        switch (translateType) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
                    transMap.put(langCode, entryInfoEntity.getChinese());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
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

    private boolean updateTransToDiVo(boolean tag,boolean common,String langCode, EntryInfoEntity entryInfoEntity, String translateType, Set<DictionaryVo> dictionaryVos) {
        boolean isExist = false;

        for (DictionaryVo dictionaryVo : dictionaryVos) {
            if (dictionaryVo.getSource().equals(entryInfoEntity.getEntry())) {

                if (!entryInfoEntity.getTag().equals(dictionaryVo.getTag())) {
                    continue;
                }

                if (!entryInfoEntity.getComment().equals(dictionaryVo.getComments())) {
                    continue;
                }
                isExist = true;


                Map<String, String> transMap = dictionaryVo.getTranslation();
                if (CollectionUtils.isEmpty(transMap)) {

                    transMap = new HashMap<>();
                }
                switch (translateType) {
                    case ConstantInterface.CHINESE:
                        if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
                            transMap.put(langCode, entryInfoEntity.getChinese());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
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

    private void constructDIVO(JSONArray jsonArray, Set<DictionaryVo> dictionaryVos) {
        for (int i = 0; i < jsonArray.size(); i++) {
            DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
            dictionaryVos.add(dictionaryVo);
        }
    }

    public static class DiEntryResult<E>{

        List<EntryInfoEntity> result = new ArrayList<>();

        List<String> exceptionMessage =  new ArrayList<>();

        public Map<Exception,Set<E>> exceptionCollection = new HashMap<>();


        public DiEntryResult(){}

        DiEntryResult(List<EntryInfoEntity> result){
            this(result,null);
        }

        DiEntryResult(List<EntryInfoEntity> result,List<String> exceptionMessage){
            this.result.addAll(result);
            if(exceptionMessage != null)
                this.exceptionMessage.addAll(exceptionMessage);
        }

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


    public DiEntryResult<String> getDiEntry(String i18nUrl, Set<String> difileSet, String taskID, String userName, String productId, String versionID, String translateType) throws Exception {
        DiEntryResult<String> result = new DiEntryResult<>();
        List<EntryInfoEntity> list = result.getResult();
        List<String> exceptionList = result.getExceptionMessage();
        Map<Exception,Set<String>> exceptionCollection = result.getExceptionCollection();
        
        if (CollectionUtils.isEmpty(difileSet)) {
            return result;
        }

        for (String dic : difileSet) {
            JSONArray jsonArray = new JSONArray();
            Date date = new Date(System.currentTimeMillis());
            String dicName = "";
            Map<String, String> headerParameters = new HashMap<>();
            if (dic.contains("平台")){
                dicName = dic.replaceAll("平台","pt");
            }else if (dic.contains("监控")){
                dicName = dic.replaceAll("监控","jk");
            }else {
                dicName = dic;
            }
            headerParameters.put("dictionary", dicName);
            String dicName1 =dicName;
            log.info("getDiEntry: " + i18nUrl + ConstantInterface.IMPORT_DICTIONARY + " fileName : " + dicName);
            if (dicName.contains("/")){
                dicName1 = dicName.split("/")[1];
            }
            try {
                String s = "";
                try {
                    s = httpUtils.get(i18nUrl + ConstantInterface.IMPORT_DICTIONARY, headerParameters);                    
                } catch (Exception e) {
                    throw new i18nServerConnectException();
                }
                try {
                    jsonArray = JSONArray.parseArray(s);   
                } catch (Exception e) {
                    throw new i18nServerParseException();
                }
            } catch (Exception e) {
                if(!exceptionCollection.containsKey(e)){
                    exceptionCollection.put(e, new HashSet<>());
                }
                exceptionCollection.get(e).add(dicName);
                
                // exceptionList.add("当前读取文件: \"" + dicName +"\"" + "时发生异常,请检查网络服务是否正常, 该文件是否存在, 文件内容是否存在异常");
                continue;
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                if (Objects.isNull(dictionaryVo.getTranslation())) {
                    continue;
                }
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID());
                entryInfoEntity.setEntry(dictionaryVo.getSource());
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                entryInfoEntity.setTag(dictionaryVo.getTag());
                entryInfoEntity.setComment(dictionaryVo.getComments());
                entryInfoEntity.setImportType(ConstantInterface.DI);
                entryInfoEntity.setWriteType(ConstantInterface.DI);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setEntrySource(dicName);
                entryInfoEntity.setDiFileName("tr/" + dicName1);
                entryInfoEntity.setProductID(productId);
                entryInfoEntity.setIsDelete(0);
                entryInfoEntity.setUpdateTime(date);
                entryInfoEntity.setUpdate(userName);
                Map<String, String> map = dictionaryVo.getTranslation();
                if (StringUtils.isBlank(map.get(translateType))) {
                    switch (translateType) {
                        case ConstantInterface.CHINESE:
                            entryInfoEntity.setChinese(map.get(translateType));
                            break;
                        case ConstantInterface.ENGLISH:
                            entryInfoEntity.setEnglish(map.get(translateType));
                            break;
                        case ConstantInterface.RUSSIAN:
                            entryInfoEntity.setRussian(map.get(translateType));
                            break;
                        case ConstantInterface.FRENCH:
                            entryInfoEntity.setFrench(map.get(translateType));
                            break;
                        case ConstantInterface.SPANISH:
                            entryInfoEntity.setSpanish(map.get(translateType));
                            break;
                    }
                }
                list.add(entryInfoEntity);
            }
        }
        

        return  result;
    }

    
    
    
    public  DiEntryResult<String> getEnumEntry(String i18nUrl, Set<String> enumfileSet, String taskID, String userName, String productId, String versionId, String translateType) throws Exception {
        DiEntryResult<String> result = new DiEntryResult<>();
        List<EntryInfoEntity> entryInfoEntities = result.getResult();
        List<String> exceptionMessage = result.getExceptionMessage();
        Map<Exception,Set<String>> exceptionCollection = result.getExceptionCollection();

        if (CollectionUtils.isEmpty(enumfileSet)) {
            return result;
        }
        Date date = new Date(System.currentTimeMillis());


        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }



        for (String fileName : enumfileSet) {
            JSONArray jsonArray = new JSONArray();
            String s = "";
            Map<String, String> headerParameters = new HashMap<>();
            //fileName 为/分割最后一个元素
            if (fileName.contains("/")){
                fileName = fileName.split("/")[1];
            }
            headerParameters.put("fileName", fileName);
            try {
                try {
                    s = httpUtils.get(i18nUrl + ConstantInterface.GET_ENUM_ENTRY, headerParameters);
                } catch (Exception e) {
                    throw new i18nServerConnectException();
                }
                
                try {
                    jsonArray = JSONArray.parseArray(s);      
                } catch (Exception e) {
                    throw new i18nServerParseException();
                }
            } catch (Exception e) {
                if(!exceptionCollection.containsKey(e)){
                    exceptionCollection.put(e, new HashSet<>());
                }
                exceptionCollection.get(e).add(fileName);
                
                // exceptionMessage.add("当前读取文件: \"" + fileName +"\"" + "时发生异常,请检查网络服务是否正常, 该文件是否存在, 文件内容是否存在异常");
                continue;
            }

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
                entryInfoEntity.setVersionID(versionId);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.ENUM);
                entryInfoEntity.setWriteType(ConstantInterface.DI);
                entryInfoEntity.setProductID(productId);
                entryInfoEntity.setIsDelete(0);
                entryInfoEntities.add(entryInfoEntity);


            }
        }
        log.info(" start send http request : " + i18nUrl + ConstantInterface.GET_ENUM_ENTRY);

        return result;
    }

    private void createNewTrans(EntryInfoEntity entryInfoEntity, String translateType, String translate) {
        //写入翻译字段
        switch (translateType) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setChinese(translate);
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
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

    public DiEntryResult<String> getConfigEntry(String i18nUrl, Set<String> configfileSet, String taskID, String userName, String productID, String versionId, String translateType) throws Exception {
        DiEntryResult<String> result = new DiEntryResult<>();
        List<EntryInfoEntity> entryInfoEntities = result.getResult();
        List<String> exceptionMessage = result.getExceptionMessage();
        Map<Exception,Set<String>> exceptionCollection = result.getExceptionCollection();

        if (CollectionUtils.isEmpty(configfileSet)) {
            return result;
        }

        Date date = new Date(System.currentTimeMillis());
        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }

        for (String fileName : configfileSet){
            JSONArray jsonArray = new JSONArray();
            String s = "";
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("fileName", fileName);
            try {
                try {
                    s = httpUtils.get(i18nUrl + ConstantInterface.GET_CONGIF_ENTRY,headerParameters);   
                } catch (Exception e) {
                    throw new i18nServerConnectException();
                }
                try {
                    jsonArray = JSONArray.parseArray(s);
                } catch (Exception e) {
                    throw new i18nServerParseException();
                }
            } catch (Exception e) {
                if(!exceptionCollection.containsKey(e)){
                    exceptionCollection.put(e, new HashSet<>());
                }
                exceptionCollection.get(e).add(fileName);
                
                // exceptionMessage.add("当前读取文件: \"" + fileName +"\"" + "时发生异常,请检查网络服务是否正常, 该文件是否存在, 文件内容是否存在异常");
                continue;
            }
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
                entryInfoEntity.setVersionID(versionId);
                entryInfoEntity.setTaskId(taskID);
                entryInfoEntity.setIsDelete(0);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.CONFIG);
                entryInfoEntity.setWriteType(ConstantInterface.DI);
                entryInfoEntity.setProductID(productID);
                entryInfoEntities.add(entryInfoEntity);


            }
        }
        log.info(" start send http request : " + i18nUrl + ConstantInterface.GET_APP_BYNODE);

        return result;
    }
}
