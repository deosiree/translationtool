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
import com.shr.translationtoolservice.service.processor.groupby.DefaultEntryGroupbyStrategy;
import com.shr.translationtoolservice.service.processor.groupby.GeneralReplicatedVOType;
import com.shr.translationtoolservice.util.*;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private EntryUtils entryUtils;

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
        Map<String,List<Map<String, String>>> tsEntryInfoMap = new HashMap<>();

        //di分组   fileName -> list
        Map<String, List<EntryInfoEntity>> diTypeMap = new HashMap<>();
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
        int result = processEntryInfo(entryInfoEntities, translateType, tsEntryInfoMap, diTypeMap);
        if(result < 0){
            return "处理词条信息时发生异常, 检查日志信息";
        }

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
                }
            }
        }
        if (!CollectionUtils.isEmpty(diTypeMap)) {
            //按照分裂写入di
            for (String di_fileName : diTypeMap.keySet()) {
                String res = diUtils.writeDiEntry(diTypeMap.get(di_fileName), di_fileName, translateType, tag, common, i18nUrl);
                if (!res.equals(ConstantInterface.OK_STR)) {
                    return res;
                }
            }

        }

        String extrasInfos = builder.toString();
        return extrasInfos.isEmpty()? ConstantInterface.OK_STR  : "部分更新成功" + "额外信息: " + extrasInfos;
    }

    private int processEntryInfo(
        List<EntryInfoEntity> entryInfoEntities, 
        String translateType, 
        Map<String, List<Map<String, String>>> tsEntryInfoMap, 
        Map<String, List<EntryInfoEntity>> diTypeMap) {


        /* 设定对应语言的翻译状态为3 */
        Map<String, String> entryInfoEntityTranslateStateFieldMap = ConstantInterface.entryInfoEntityTranslateStateFieldMap();

        String translateStateFieldName = entryInfoEntityTranslateStateFieldMap.get(translateType);

        if(StringUtils.isBlank(translateStateFieldName)){
            log.error(String.format("未找到翻译状态的属性名称: %s", translateType));
            return -1;
        }

        Map<String, String> translateFieldMap = ConstantInterface.translateFieldMap();

        String translateField = translateFieldMap.get(translateType);

        if(StringUtils.isBlank(translateField)){
            log.error(String.format("未找到翻译属性名称: %s", translateType));
            return -1;
        }
        
        MethodEntity getTranslateMethod = new MethodEntity(MethodUtils.acquireMethod(EntryInfoEntity.class, translateField,MethodUtils.DEFAULT_GET_METHOD_NAME_GENERATOR));

        MethodEntity getTranslateStateMethod = new MethodEntity(MethodUtils.acquireMethod(EntryInfoEntity.class, translateStateFieldName, MethodUtils.DEFAULT_GET_METHOD_NAME_GENERATOR));
        

        /* 过滤词条翻译状态不为已审核的词条，这部分词条不回写 */
        /* 过滤出可回写词条 */ 
        List<EntryInfoEntity> filteredEntryInfos = entryInfoEntities.stream().filter(new Predicate<EntryInfoEntity>() {


            @Override
            public boolean test(EntryInfoEntity t) {
                // TODO Auto-generated method stub
                // return filteredEntryIDs.contains(t.getId()) && getTranslateMethod.invoke(t);
                try {
                    Object translateStateValue = getTranslateStateMethod.invoke(t);
                    String translateState = translateStateValue == null ? "" : String.valueOf(translateStateValue);

                    Object translateValue = getTranslateMethod.invoke(t);
                    String translate = translateValue == null ? "" : String.valueOf(translateValue); 

                    return (!translateState.isEmpty() && translateState.equals("3")) && !StringUtils.isBlank(translate); // 翻译不为空，并且翻译状态为3
                } catch (Exception e) {
                    log.error(String.format("异常信息: %s", e.getMessage()), e);
                    return false;
                }
            }
            
        }).collect(Collectors.toList());

        if(filteredEntryInfos.size() < entryInfoEntities.size()){
            log.info(String.format("请求回写词条共%d个, 经过滤后可回写%d个",entryInfoEntities.size(), filteredEntryInfos.size()));
        }

        String writeTypeFieldName = "writeType";

        DefaultEntryGroupbyStrategy entryWriteTypeGroupbyStrategy = new DefaultEntryGroupbyStrategy();

        entryWriteTypeGroupbyStrategy.addTargetAttribute(writeTypeFieldName);

        /* 将词条信息根据回写类型划分到不同的组下 */
        Map<GeneralReplicatedVOType, List<EntryInfoEntity>> writeTypeGroupedEntryInfos = entryUtils.makeGroupMapForEntryInfoEntities(filteredEntryInfos, entryWriteTypeGroupbyStrategy);

        String attributeKey = String.valueOf(entryWriteTypeGroupbyStrategy.getFieldColumnNumber(writeTypeFieldName));
        GeneralReplicatedVOType tsType = new GeneralReplicatedVOType();
        tsType.addAttribute(attributeKey, ConstantInterface.TS);

        GeneralReplicatedVOType dicType = new GeneralReplicatedVOType();
        dicType.addAttribute(attributeKey, ConstantInterface.DI);

        List<EntryInfoEntity> tsEntryInfosList = writeTypeGroupedEntryInfos.get(tsType);
        if(tsEntryInfosList != null && !tsEntryInfosList.isEmpty()){

            /* 处理TS词条 */
            for(EntryInfoEntity entryInfoEntity : tsEntryInfosList){
                String entrySource = entryInfoEntity.getEntrySource();
                if(entrySource.isEmpty()){
                    log.warn(String.format("警告, TS词条的词条来源为空, entry: [%s], tag: [%s], comment: [%s]", entryInfoEntity.getEntry(), entryInfoEntity.getTag(), entryInfoEntity.getComment()));
                    continue;
                }
                Map<String, String> tsEntryInfo = new HashMap<>();
                tsEntryInfo.put("source", entryInfoEntity.getEntry());
                String tagStr =  entryInfoEntity.getTag();
                tsEntryInfo.put("tag", tagStr);
                tsEntryInfo.put("comment", entryInfoEntity.getComment());

                Object translateValue = getTranslateMethod.invoke(entryInfoEntity);
                String translate = translateValue == null ? "" : String.valueOf(translateValue); 
                tsEntryInfo.put("translate", translate);

                if(CollectionUtils.isEmpty(tsEntryInfoMap.get(entrySource))){
                    List<Map<String, String>> tsEntrys = new ArrayList<>();
                    tsEntrys.add(tsEntryInfo);
                    tsEntryInfoMap.put(entrySource, tsEntrys);

                }else{
                    List<Map<String, String>> list = tsEntryInfoMap.get(entrySource);
                    list.add(tsEntryInfo);
                }
            }
        }else{
            log.info("回写请求无可回写的TS词条, 跳过TS词条的处理过程");
        }

        List<EntryInfoEntity> dicEntryInfosList = writeTypeGroupedEntryInfos.get(dicType);
        if(dicEntryInfosList != null && !dicEntryInfosList.isEmpty()){

            

            for(EntryInfoEntity entryInfoEntity : dicEntryInfosList){
                String diFileName = entryInfoEntity.getDiFileName();
                if(diFileName.isEmpty()){
                    log.warn(String.format("警告, DIC词条的diFileName为空, entry: [%s], tag: [%s], comment: [%s]", entryInfoEntity.getEntry(), entryInfoEntity.getTag(), entryInfoEntity.getComment()));
                    continue;
                }

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
        }else{
            log.info("回写请求无可回写的DIC词条, 跳过DIC词条的处理过程");
        }

        return 0;


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

}
