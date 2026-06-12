package com.shr.translationtoolservice.service.entry;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.ProductRelationMapper;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ProductRelationEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.entity.vo.TranslateEntitiyReplicateCheckVO;
import com.shr.translationtoolservice.service.processor.EntryImportProcessor;
import com.shr.translationtoolservice.service.processor.converter.DICDictionaryVOConverter;
import com.shr.translationtoolservice.service.processor.converter.TSDictionaryVOConverter;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.EntryProcessUtils;
import com.shr.translationtoolservice.util.EntryUtils;
import com.shr.translationtoolservice.util.HTTPUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.LocalTimeUtils;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BatchInsertEntryHandler {

    @Autowired
    protected HTTPUtils httpUtils;

    @Autowired
    protected TLanguageMapper languageMapper;

    @Autowired
    protected TaskInfoMapper taskInfoMapper;

    @Autowired
    protected CommonUtils commonUtils;

    @Autowired
    protected EntryInfoMapper entryInfoMapper;

    @Autowired
    protected TranslateMapper translateMapper;

    @Autowired
    protected EntryProcessUtils entryProcessUtils;

    @Autowired
    @Lazy
    protected ProductRelationMapper productRelationMapper;

    @Autowired
    private BackendTaskInfoHandler backendTaskInfoHandler;

    @Autowired  
    private LocalTimeUtils localTimeUtils;

    @Autowired
    private EntryUtils entryUtils;

    @Autowired
    protected EntryImportProcessor entryImportProcessor;

    Map<String,String> languageSetTranslateMethodMap =  ConstantInterface.entryInfoEntitySetterTranslateMap();

    Map<String,String> languageGetTranslateMethodMap = ConstantInterface.entryInfoEntityGetterTranslateMap();

    Map<String,String> setTranslateIDMethodMap = ConstantInterface.entryInfoEntitySetTranslateIDMethodMap();

    int batchSize = 10;

    ThreadPoolExecutor executor = new ThreadPoolExecutor(80, 120, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());


    // 1. 当地时间东八区
    protected SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // 2. 格式化：转为西六区（America/Chicago）时间
    protected SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    

    public BatchInsertEntryHandler(){
        this.parser.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai")); // 指定解析时的时区
        this.formatter.setTimeZone(TimeZone.getTimeZone("America/Chicago")); // 目标时区
    }




    private Consumer<EntryInfoEntity> consumerForImportEntitiesFromLangDirDirectly = new Consumer<EntryInfoEntity>() {

        @Override
        public void accept(EntryInfoEntity t) {
            // TODO Auto-generated method stub
            t.setEntryState(3);
            String currentImportType = t.getImportType();
            if(currentImportType.equals(ConstantInterface.TS)){
                return;
            }else if(currentImportType.equals(ConstantInterface.DI)){
                String writeFileName = t.getWriteType();
                if(writeFileName.startsWith("config/")){
                    t.setImportType(ConstantInterface.CONFIG);
                }else if(writeFileName.startsWith("db/")){
                    t.setImportType(ConstantInterface.DB);
                }else if(writeFileName.startsWith("db/meta/")){
                    t.setImportType(ConstantInterface.DB);  // 目前是DB类型
                }else if(writeFileName.startsWith("tr/")){
                    t.setImportType(ConstantInterface.DI);
                }else{

                }
            }else{

            }
        }
    };

    private Consumer<TranslateEntity> consumerForImportTranslatesFromLangDirDirectly = new Consumer<TranslateEntity>() {

        @Override
        public void accept(TranslateEntity t) {
            // TODO Auto-generated method stub
            t.setTranslateState("3");
            
        }
    };


    protected Date convertCurrentTime(){
        Date date = new Date(System.currentTimeMillis());
        String timeSequence = parser.format(date);
        try {
            // return this.formatter.parse(timeSequence);
            return date;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
            return null;
        }
    }


    /**
     * 获取/run/lang/dic下的文件列表
     * @param i18nUrl
     * @param department
     * @return
     */
    public List<String> getTRFileListUsingI18nServer(String i18nUrl,String department){
        JSONArray jsonArray = new JSONArray();
        String s = "";
        List<String> fileNameList = new ArrayList<>();
        try {
            s = httpUtils.get(i18nUrl + ConstantInterface.DICTIONARY);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                fileNameList.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return fileNameList;    // tr/gui_i18n_tool,enum/xxxx
    }

    /**
     * 获取指定语言文件夹下所有的ts文件名，
     * @param i18nUrl
     * @param targetLanguageTypes
     * @return
     */
    public Set<String> getTSFileListUsingI18nServer(String i18nUrl,List<String> targetLanguages){
   
        Set<String> tsFileList = new HashSet<>();    // {"gui_i18n_tool_en_US.ts","xxx"}
        for(String targetLanguage : targetLanguages){
            TLanguage tLanguage = new TLanguage();
            tLanguage.setName(targetLanguage);
            List<TLanguage> languages = languageMapper.getLanguages(tLanguage);
            if(languages.isEmpty()){
                throw new RuntimeException("警告,没有找到对应语言的信息: " + targetLanguage);
            }
            /* 获取下一种语言所有的ts文件列表 */
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("laguage", languages.get(0).getCode());
            JSONArray jsonArray = new JSONArray();
            try {
                String s = httpUtils.get(i18nUrl + ConstantInterface.GET_FILE_LIST, headerParameters);
                jsonArray = JSONArray.parseArray(s);
                for (int i = 0; i < jsonArray.size(); i++) {
                    tsFileList.add(jsonArray.get(i).toString());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;    
            }
        }
        return tsFileList;
    }

    // /**
    //  * 设定{@code source},{@code tag},{@code comment}以及{@code translation}（例如english,russian等） 
    //  * 针对由辞典导入的词条(由dic导入的), 根据导入的词条已有的翻译,设定{@code entryInfoEntity}对应语种的翻译信息，以及source,tag,commment(通用)
    //  * @param entryInfoEntity
    //  * @param dictionaryVo
    //  * @param tLanguages
    //  */
    // protected void constructCriticalAttributesForEntryInfoEntitiy(EntryInfoEntity entryInfoEntity,DictionaryVO dictionaryVo,List<TLanguage> tLanguages){
    //     /* 添加source,tag,comment */
    //     entryInfoEntity.setEntry(dictionaryVo.getSource());
    //     entryInfoEntity.setTag(dictionaryVo.getTag());
    //     entryInfoEntity.setComment(dictionaryVo.getComments());
    //     /* 添加所有的语种的翻译 */
    //     Map<String, String> translationMap = dictionaryVo.getTranslation(); // {"en_US" : "xxx","zh_CN": "xxx", "ru_RU" : "xxxx"}

    //     for(TLanguage tLanguage : tLanguages){
    //         String langCode = tLanguage.getCode();
    //         String name = tLanguage.getName();
    //         String setMethodName = this.languageSetTranslateMethodMap.get(name);
    //         if(!translationMap.containsKey(langCode)){
    //             continue;
    //         }
    //         try {
    //             /* 设定语言属性 */
    //             Method method = entryInfoEntity.getClass().getMethod(setMethodName, String.class);
    //             String translate = translationMap.get(langCode);
    //             method.invoke(entryInfoEntity, translate == null ? "" : translate);
    //         } catch (Exception e) {
    //             // TODO Auto-generated catch block
    //             log.error(e.getMessage(), e);
    //             continue;
    //         }
    //     }
    // }
    // /**
    //  * 针对由辞典导入的词条(由dic导入的)，设定通用基本属性，
    //  * 并会调用{@link constructCriticalAttributesForEntryInfoEntitiy}设定{@code source},{@code tag} {@code comment}和翻译信息(通用)
    //  * @param dictionaryVo
    //  * @param tLanguages
    //  * @return
    //  */
    // protected EntryInfoEntity constructBasicAttributesForEntryInfoEntitiy(
    //     DictionaryVO dictionaryVo,
    //     List<TLanguage> tLanguages
    // ){

    //     EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
    //     entryInfoEntity.setId(commonUtils.getUUID());
    //     this.constructCriticalAttributesForEntryInfoEntitiy(entryInfoEntity, dictionaryVo, tLanguages);        
    //     entryInfoEntity.setEntryState(0);
    //     entryInfoEntity.setIsDelete(0);

    //     return entryInfoEntity;
    // }


    /**
     * 构建EntryInfoEntity，请求i18n获取指定路径文件的词条, 路径是相对于/run/lang/dic的，例如dicList: {tr/${fileName}},{db/${fileName}},
     * 创建EntryInfoEntity，并设定相应属性(通用)
     * @param dicList   ["tr/gui_i18n_tool","tr/i18n_test","pt/i18n_test"]
     * @param i18nUrl
     * @param transType
     * @param taskID
     * @param userName
     * @return
     */
    @Transactional
    protected List<EntryInfoEntity> buildEntryFromTRFilesUsingI18nServer(
        List<String> dicList,
        String i18nUrl,
        TaskInfoEntity taskInfoEntity,
        String userName,
        List<TLanguage> languageList){

        
        if(StringUtils.isBlank(i18nUrl)){
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        String s = "";
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        // String taskID = taskInfoEntity.getId();
        // String productID = taskInfoEntity.getProductId();
        // String versionID = taskInfoEntity.getVersionId();

        for (String dicRelPath : dicList) {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dictionary", dicRelPath);
            try {
                log.info("准备获取dic的词条数据: fileName: " + dicRelPath);

                s = httpUtils.get(i18nUrl + ConstantInterface.IMPORT_DICTIONARY, headerParameters);                    
            } catch (Exception e) {
                throw new RuntimeException(e); // 网络异常等非正常情况，直接报错返回
            }
            try {
                jsonArray = JSONArray.parseArray(s);    
            } catch (Exception e) {
                try {
                    Gson gson = new Gson();
                    Map<String,String> messages =  gson.fromJson(s, Map.class);
                    if(messages.containsKey("message")){
                        log.info(String.format("未能获取到文件: %s的词条, 详细信息: %s", dicRelPath,messages.get("message")));
                    }else{
                        log.info("没有找到对应的message属性,出现了其他异常");
                        throw new RuntimeException(e);
                    }                    
                } catch (Exception parseError) {
                    log.error(parseError.getMessage(), parseError);
                    throw new RuntimeException(e);  // 其他异常, 直接报错返回
                }
                continue;
            }
            log.info("获取到dic的词条数据, 准备创建对象: fileName: " + dicRelPath);

            List<DictionaryVO> dictionaryVOs = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                dictionaryVOs.add(new DictionaryVO(dictionaryVo));
                // EntryInfoEntity entryInfoEntity = this.constructBasicAttributesForEntryInfoEntitiy(new DictionaryVO(dictionaryVo), languageList);
                // entryInfoEntity.setEntrySource(dicRelPath);
                // entryInfoEntity.setDiFileName(dicRelPath);
                // entryInfoEntity.setWriteType(ConstantInterface.DI);
                // entryInfoEntity.setImportType(ConstantInterface.DI);
                // entryInfoEntity.setEntryState(1);
                // entryInfoEntity.setTaskId(taskID);
                // entryInfoEntity.setProductID(productID);
                // entryInfoEntity.setVersionID(versionID);
                // entryInfoEntity.setUpdateTime(this.convertCurrentTime());
                // entryInfoEntity.setUpdate(userName);
                // entryInfoEntities.add(entryInfoEntity);
            }
            DICDictionaryVOConverter dicDictionaryVOConverter = new DICDictionaryVOConverter(dicRelPath, taskInfoEntity.getId(), taskInfoEntity.getProductId(), taskInfoEntity.getVersionId(), commonUtils, languageList, userName, dicRelPath);
            // entryInfoEntities.addAll(convertFromDictionaryVOToDICEntry(dictionaryVOs, dicRelPath, dicRelPath, taskInfoEntity, userName, languageList));
            entryInfoEntities.addAll(entryImportProcessor.convertToEntryInfos(dicDictionaryVOConverter, dictionaryVOs));

        }
        return entryInfoEntities;
    }

    // public List<EntryInfoEntity> convertFromDictionaryVOToDICEntry(
    //     Collection<DictionaryVO> dictionaryVOs,
    //     String entrySource,
    //     String diFileName,
    //     TaskInfoEntity taskInfoEntity,
    //     String userName,
    //     List<TLanguage> languageList
    // ){
    //     List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
    //     String taskID = taskInfoEntity.getId();
    //     String productID = taskInfoEntity.getProductId();
    //     String versionID = taskInfoEntity.getVersionId();
    //     for(DictionaryVO dictionaryVO : dictionaryVOs){
    //         EntryInfoEntity entryInfoEntity = this.constructBasicAttributesForEntryInfoEntitiy(dictionaryVO, languageList);
    //         entryInfoEntity.setEntrySource(entrySource);
    //         entryInfoEntity.setDiFileName(diFileName);
    //         entryInfoEntity.setWriteType(ConstantInterface.DI);
    //         entryInfoEntity.setImportType(ConstantInterface.DI);
    //         entryInfoEntity.setEntryState(1);
    //         entryInfoEntity.setTaskId(taskID);
    //         entryInfoEntity.setProductID(productID);
    //         entryInfoEntity.setVersionID(versionID);
    //         entryInfoEntity.setUpdateTime(this.convertCurrentTime());
    //         entryInfoEntity.setUpdate(userName);
    //         entryInfoEntities.add(entryInfoEntity);
    //     }
    //     return entryInfoEntities;
    // }



    /**
     * 构建EntryInfoEntity，导入TS文件的词条(通用)
     * @param tsFileNamePrefixList
     * @param targetLanguages
     * @param i18nUrl
     * @param taskInfoEntity
     * @param userName
     * @return
     */
    @Transactional
    protected List<EntryInfoEntity> buildEntryFromTSFilesUsingI18nServer(
        List<String> tsFileNamePrefixList,
        List<TLanguage> targetLanguages,
        String i18nUrl,
        TaskInfoEntity taskInfoEntity,
        String userName
    ){

        // String taskID = taskInfoEntity.getId();
        // String versionID = taskInfoEntity.getVersionId();
        // String productId = taskInfoEntity.getProductId();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        Map<String,Set<DictionaryVO>> dictionaryVOMap = new HashMap<>();    // key: ts文件名 ,value : 词条集合
        for (String tsFileNamePrefix : tsFileNamePrefixList) {
            Set<DictionaryVO> dictionaryVOs = new HashSet<>();
            Map<DictionaryVO,Map<String,String>> entryTranslatesMap = new HashMap<>();  // key: 词条 value: {"en_US": "xxx","zh_CN": "xxx"}
            for(TLanguage targetLanguage : targetLanguages){
                String langCode = targetLanguage.getCode();
                String fullTSFileName = tsFileNamePrefix + "_" + langCode + ".ts";
                Map<String, String> headerParameters = new HashMap<>();
                headerParameters.put("fileName", fullTSFileName);
                JSONArray jsonArray = new JSONArray();
                String s = "";
                try {
                    s = httpUtils.get(i18nUrl + ConstantInterface.GET_WORDS, headerParameters);  // 如果对应语言的ts文件不存在，例如gui_i18n_tool_en_US.ts存在，但是gui_i18n_tool_es_ES.ts不存在，此时es_ES情况下返回空字符串
                } catch (Exception e) {
                    throw new RuntimeException(e);  // 网络异常
                }
                try {
                    jsonArray = JSONArray.parseArray(s);
                } catch (Exception e) {
                    throw new RuntimeException(e);  // 数据格式异常
                }
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONArray jsonArray1 = JSONArray.parseArray(jsonArray.get(i).toString());
                    String entry = jsonArray1.get(0).toString();
                    String translate = jsonArray1.getString(1);
                    String tag = jsonArray1.getString(2);
                    String comment = jsonArray1.getString(3);
                    DictionaryVO dictionaryVO = new DictionaryVO();
                    dictionaryVO.setSource(entry);
                    dictionaryVO.setTag(tag);
                    dictionaryVO.setComments(comment);
                    if(entryTranslatesMap.containsKey(dictionaryVO)){
                        entryTranslatesMap.get(dictionaryVO).put(langCode, translate);
                    }else{
                        HashMap<String,String> translates = new HashMap<>();
                        translates.put(langCode, translate);
                        entryTranslatesMap.put(dictionaryVO, translates);
                    }
                }

            }
        
            for(Map.Entry<DictionaryVO,Map<String,String>> entryTranslate : entryTranslatesMap.entrySet()){
                DictionaryVO dictionaryVO = entryTranslate.getKey();
                Map<String,String> translateMap = entryTranslate.getValue();
                dictionaryVO.setTranslation(translateMap);
                dictionaryVOs.add(dictionaryVO);
            }
            dictionaryVOMap.put(tsFileNamePrefix, dictionaryVOs);
        }


        for(Map.Entry<String,Set<DictionaryVO>> dictionaryVOEntry :dictionaryVOMap.entrySet()){
            Set<DictionaryVO> dictionaryVOs = dictionaryVOEntry.getValue();
            String tsFileNamePrefix = dictionaryVOEntry.getKey();
            // for(DictionaryVO dictionaryVO : dictionaryVOs){
  
            //     EntryInfoEntity entryInfoEntity = this.constructBasicAttributesForEntryInfoEntitiy(dictionaryVO, targetLanguages);

            //     entryInfoEntity.setEntrySource(tsFileNamePrefix);
            //     entryInfoEntity.setEntryState(1);
            //     entryInfoEntity.setTaskId(taskID);
            //     entryInfoEntity.setUpdate(userName);
            //     entryInfoEntity.setUpdateTime(this.convertCurrentTime());
            //     entryInfoEntity.setProductID(productId);
            //     entryInfoEntity.setVersionID(versionID);
            //     entryInfoEntity.setImportType(ConstantInterface.TS);
            //     entryInfoEntity.setWriteType(ConstantInterface.TS);
            //     entryInfoEntity.setIsDelete(0);
            //     entryInfoEntities.add(entryInfoEntity);
            // }
            // entryInfoEntities.addAll(this.convertFromDictionaryVOToTSEntry(dictionaryVOs, tsFileNamePrefix, targetLanguages, taskInfoEntity, userName));

            TSDictionaryVOConverter tsDictionaryVOConverter = new TSDictionaryVOConverter(tsFileNamePrefix, taskInfoEntity.getId(), taskInfoEntity.getProductId(), taskInfoEntity.getVersionId(), commonUtils, userName, targetLanguages);
            entryInfoEntities.addAll(entryImportProcessor.convertToEntryInfos(tsDictionaryVOConverter, dictionaryVOs));
        }

        return entryInfoEntities;
    }

    // public List<EntryInfoEntity> convertFromDictionaryVOToTSEntry(
    //     Set<DictionaryVO> dictionaryVOs,
    //     String entrySource,
    //     List<TLanguage> targetLanguages,
    //     TaskInfoEntity taskInfoEntity,
    //     String userName
    // ){
    //     List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
    //     String versionID = taskInfoEntity.getVersionId();
    //     String productId = taskInfoEntity.getProductId();

    //     for(DictionaryVO dictionaryVO : dictionaryVOs){

    //         EntryInfoEntity entryInfoEntity = this.constructBasicAttributesForEntryInfoEntitiy(dictionaryVO, targetLanguages);

    //         entryInfoEntity.setEntrySource(entrySource);
    //         entryInfoEntity.setEntryState(1);
    //         entryInfoEntity.setTaskId(taskInfoEntity.getId());
    //         entryInfoEntity.setUpdate(userName);
    //         entryInfoEntity.setUpdateTime(this.convertCurrentTime());
    //         entryInfoEntity.setProductID(productId);
    //         entryInfoEntity.setVersionID(versionID);
    //         entryInfoEntity.setImportType(ConstantInterface.TS);
    //         entryInfoEntity.setWriteType(ConstantInterface.TS);
    //         entryInfoEntity.setIsDelete(0);
    //         entryInfoEntities.add(entryInfoEntity);
    //     }
    //     return entryInfoEntities;
    // }


    /**
     * 根据给定的entryInfoEntities, 构建TranslateEntitiy对象(多种语言的)(通用的)
     * @param entryInfoEntities
     * @param targetLanguageTypes
     * @param department
     * @return 返回要插入的翻译, 如果没有，则为空列表
     */
    @Transactional
    public List<TranslateEntity> buildTranslateEntity(List<EntryInfoEntity> entryInfoEntities,List<String> targetLanguageTypes,String department){

        List<TranslateEntity> translateEntities = new ArrayList<>();
        for(String targetLanguageType : targetLanguageTypes){

            String getTranslateMethod = this.languageGetTranslateMethodMap.get(targetLanguageType);
            String setTranslateIDMethod = this.setTranslateIDMethodMap.get(targetLanguageType);
            /* 将所有词条按照翻译结果进行分类 */
            Map<TranslateEntitiyReplicateCheckVO,List<EntryInfoEntity>> entryInfoGroups = new HashMap<>(); 

            Set<TranslateEntitiyReplicateCheckVO> translateEntitiyReplicateCheckVOs = new HashSet<>();
            for(EntryInfoEntity t : entryInfoEntities){
                String translate = "";
                try {
                    Object translateObject = t.getClass().getMethod(getTranslateMethod).invoke(t);
                    if(translateObject != null){
                        translate = String.valueOf(translateObject);
                    }else{
                        translate = "";
                    }
                    
                } catch( Exception e){
                    throw new RuntimeException(e);  // 关键错误

                }
                if(StringUtils.isBlank(translate)){
                    /* 如果没有对应语种的翻译, 那么不创建translateEntitiy，也不挂载 */
                    if(translate == null){
                        try {
                            /* 设定词条的enTransID等 */
                            Method method = t.getClass().getMethod(setTranslateIDMethod,String.class);
                            method.invoke(t,"");    // 默认值是空字符串
                        } catch (Exception e){
                            throw new RuntimeException(e);  // 关键错误
                        }
                    }
                    continue;
                }

                // TODO Auto-generated method stub
                TranslateEntitiyReplicateCheckVO translateEntitiyReplicateCheckVO = new TranslateEntitiyReplicateCheckVO();
                translateEntitiyReplicateCheckVO.setEntry(t.getEntry());
                translateEntitiyReplicateCheckVO.setTranslate(translate);
                translateEntitiyReplicateCheckVO.setTranslateState(3);
                translateEntitiyReplicateCheckVO.setTranslateType(targetLanguageType);
                translateEntitiyReplicateCheckVO.setVisualRange(department);

                translateEntitiyReplicateCheckVOs.add(translateEntitiyReplicateCheckVO);
                List<EntryInfoEntity> entryInfos = entryInfoGroups.get(translateEntitiyReplicateCheckVO);
                if(entryInfos == null){
                    entryInfos = new ArrayList<>();
                    entryInfos.add(t);
                    entryInfoGroups.put(translateEntitiyReplicateCheckVO, entryInfos);
                }else{
                    entryInfos.add(t);
                }
            }
            if(translateEntitiyReplicateCheckVOs.isEmpty()){
                /* 该语种，所有词条都没有翻译, 不需要translateEntitiy，也不需要挂载,直接跳过 */
                continue;
            }
            /* 获取已经存在的翻译, 并将entryInfo对应的transID设定为已存在的翻译的ID */
            List<TranslateEntitiyReplicateCheckVO> replicatedTranVos = this.translateMapper.newGetRepTrans(translateEntitiyReplicateCheckVOs);  // equals相同的两个对象, transID和last_use_time不同

            Map<TranslateEntitiyReplicateCheckVO,List<TranslateEntitiyReplicateCheckVO>> transVOGroup = new HashMap<>();
            for(TranslateEntitiyReplicateCheckVO checkVO : replicatedTranVos){
                List<TranslateEntitiyReplicateCheckVO> checkVOs = transVOGroup.get(checkVO);
                if(checkVOs == null){
                    checkVOs = new ArrayList<>();
                    checkVOs.add(checkVO);
                    transVOGroup.put(checkVO, checkVOs);    // 每一个checkVO的transID和last_use_time会不同
                }else{
                    checkVOs.add(checkVO);
                }
            }

            for(Map.Entry<TranslateEntitiyReplicateCheckVO,List<EntryInfoEntity>> entity : entryInfoGroups.entrySet()){
                TranslateEntitiyReplicateCheckVO checkVO = entity.getKey();
                List<EntryInfoEntity> entryInfos = entity.getValue();
                String translate = checkVO.getTranslate();
                String entry = checkVO.getEntry();
                String transID = "";
                if(replicatedTranVos.contains(checkVO) && transVOGroup.containsKey(checkVO)){
                    List<TranslateEntitiyReplicateCheckVO> checkVOs = transVOGroup.get(checkVO);
                    
                    transID = checkVOs.stream().max(new Comparator<TranslateEntitiyReplicateCheckVO>() {

                        @Override
                        public int compare(TranslateEntitiyReplicateCheckVO o1, TranslateEntitiyReplicateCheckVO o2) {
                            // TODO Auto-generated method stub
                            return o1.getLatestUseTime().compareTo(o2.getLatestUseTime());
                        }
                    }).get().getId();
                    
                }else{
                    /* 没有重复的就新建 */
                    TranslateEntity translateEntity = new TranslateEntity();
                    translateEntity.setId(commonUtils.getUUID());
                    translateEntity.setEntry(entry);
                    translateEntity.setType(targetLanguageType);
                    translateEntity.setTranslate(translate);
                    translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                    translateEntity.setTranslateState("1");
                    translateEntity.setDeleteState(0);
                    translateEntity.setVisualRange(department);
                    translateEntity.setPublicState(0);
                    translateEntities.add(translateEntity);
                    transID = translateEntity.getId();  // transID挂载到新建的translate上
                }
                /* 挂载transID */
                for(EntryInfoEntity entryInfoEntity : entryInfos){
                    try {
                        /* 设定词条的enTransID等 */
                        Method method = entryInfoEntity.getClass().getMethod(setTranslateIDMethod,String.class);
                        method.invoke(entryInfoEntity, transID);

                    } catch (Exception e){
                        throw new RuntimeException(e);  // 关键错误
                    }
                }
                
            }

        }

        return translateEntities;
    }


    public List<TranslateEntity> buildTranslateEntity(
        List<EntryInfoEntity> entryInfoEntities,
        List<String> targetLanguageTypes,
        String department,
        ThreadPoolExecutor executor){
        if(executor == null){
            return buildTranslateEntity(entryInfoEntities, targetLanguageTypes, department);
        }
        int totalNum = entryInfoEntities.size();
        int taskNumber = (totalNum + batchSize - 1) / batchSize;
        log.info(String.format("任务分片共%s个", String.valueOf(taskNumber)));
        CountDownLatch countDownLatch = new CountDownLatch((totalNum + batchSize - 1) / batchSize); // 任务总数=ceil(totalNum/batchSize)
        Set<String> errorMessages = new HashSet<>();
        Queue<TranslateEntity> translateEntities = new LinkedBlockingQueue<>(); // 并发
        for(int idx = 0 ; idx < totalNum ; idx += batchSize){
            int currentIdx = idx;
            int endIdx = Math.min(currentIdx + batchSize, totalNum);
            List<EntryInfoEntity> batchList = entryInfoEntities.subList(currentIdx, endIdx);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        // 执行分片处理（传入提前截取的batchList，避免闭包引用问题）
                        List<TranslateEntity> _translateEntitiy = buildTranslateEntity(batchList, targetLanguageTypes, department);
                        translateEntities.addAll(_translateEntitiy);
                    } catch (Exception e) {
                        // 日志记录：建议用日志框架（SLF4J），这里简化打印
                        log.error("分片任务执行失败，当前分片索引：" + currentIdx + "-" + endIdx + "，异常信息：" + e.getMessage(),e);
                        errorMessages.add("分片任务执行失败，当前分片索引：" + currentIdx + "-" + endIdx + "，异常信息：" + e.getMessage());
                    } finally {
                        // 计数器减1（无论任务成功失败，都要计数）
                        countDownLatch.countDown();
                    }
                }
            });
            
        }
        try {
            // 4. 同步等待所有任务执行完成（根据业务需求选择：如果需要等待结果，就加这行；如果纯异步，可去掉）
            // 超时时间：避免无限等待（根据业务调整，比如1小时）
            boolean allDone = countDownLatch.await(3, TimeUnit.HOURS);
            if (!allDone) {

                throw new RuntimeException("部分分片任务执行超时，可能存在未完成的任务");
            } else {
                log.info(String.format("所有分片任务执行完成,成功的共%s,失败的共%s", String.valueOf(taskNumber - errorMessages.size()),String.valueOf(errorMessages.size())));
                if(!errorMessages.isEmpty()){
                    throw new RuntimeException("插入数据时出现异常, 异常信息为: " + errorMessages.toString());
                }
                return translateEntities.stream().collect(Collectors.toList());
            }
        } catch (InterruptedException e) {
            log.warn("等待任务执行时被中断：" + e.getMessage());
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException(e);
        }

    }


    /**
     * t_entry_info,t_translate,t_product_relation插入数据操作（通用的）
     * @param entryInfoEntities
     * @param taskInfoEntity
     * @param targetLanguageTypes
     * @param department
     * @param entryInfoEntitiesConsumer
     * @param translateEntitiesConsumer
     */
    @Transactional
    public void createEntryInfoEntities(
        List<EntryInfoEntity> entryInfoEntities,
        TaskInfoEntity taskInfoEntity,
        List<String> targetLanguageTypes,
        String department,
        Consumer<EntryInfoEntity> entryInfoEntitiesConsumer,
        Consumer<TranslateEntity> translateEntitiesConsumer ){
        /* 对生成的EntryInfoEntitiy进行再处理 */
        if(entryInfoEntitiesConsumer != null){
            entryInfoEntities.stream().forEach(entryInfoEntitiesConsumer);
        }
        

        List<ProductRelationEntity> productRelationEntities = new ArrayList<>();
        String taskID = taskInfoEntity.getId();

        List<TranslateEntity> translateEntities = this.buildTranslateEntity(entryInfoEntities, targetLanguageTypes, department,executor);
        /* 对生成的TranslateEntity进行再处理 */
        if(translateEntitiesConsumer != null){
            translateEntities.stream().forEach(translateEntitiesConsumer);
        }
        

        /* 将获取到的文件词条导入库中,翻译也导入库中 */
        for(EntryInfoEntity entryInfoEntity : entryInfoEntities){
         
            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setId(commonUtils.getUUID());
            productRelationEntity.setEntryId(entryInfoEntity.getId());
            productRelationEntity.setTaskId(taskID);
            productRelationEntity.setProductId(taskInfoEntity.getProductId());
            productRelationEntity.setVersionId(taskInfoEntity.getVersionId());
            productRelationEntities.add(productRelationEntity);
        
        }
        
        /* 插入t_entry_info表 */
        // entryInfoMapper.insertEntryList(entryInfoEntities);
        this.batchInserts(entryInfoEntities, entryInfoMapper, "insertEntryList",batchSize);
        
        /* 插入t_translate表 */
        for(TranslateEntity translateEntity : translateEntities){
            translateMapper.insertTranslate(translateEntity);
        }

        this.batchInserts(productRelationEntities, productRelationMapper, "insertList",batchSize);
        /* 插入t_product_relation表 */
        // productRelationMapper.insertList(productRelationEntities);            

    }

    @Transactional
    protected <T> boolean batchInserts(List<T> entities,Object mapper,String methodName,int batchSize){

        Method method;
        try {
            method = mapper.getClass().getMethod(methodName, List.class);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("未找到方法: " + methodName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("批量插入数据时获取方法时出现异常, 异常为: " + e.getMessage());
        }

        if(batchSize <= 0){
            try {
                method.invoke(mapper, entities);
            } catch(Exception e){
                throw new RuntimeException("批量插入数据时出现异常, 异常为: " + e.getMessage());
            }
        }else{
            /**
             * 不能用多线程, 因为每个线程采用的是不同的jdbc连接，所以不在一个事务中，所以整体的事务性无法保证
             * 如果采用分布式事务，分布式事务提交过程中也会有短暂的不一致的问题，如果需要保证最终一致性，需要另开一个专门进行分布式事务管理的服务，
             * 例如java程序异常终止，分布式事务仅提交部分，导致最终一致性无法保证
             */
            int totalNum = entities.size();
            int taskNumber = (totalNum + batchSize - 1) / batchSize;
            log.info(String.format("任务分片共%s个", String.valueOf(taskNumber)));
            Set<String> errorMessages = new HashSet<>();
            for(int idx = 0 ; idx < totalNum ; idx += batchSize){
                int currentIdx = idx;
                int endIdx = Math.min(currentIdx + batchSize, totalNum);
                List<T> batchList = entities.subList(currentIdx, endIdx);
                try {
                    // 执行分片处理（传入提前截取的batchList，避免闭包引用问题）
                    method.invoke(mapper, batchList);
                } catch (Exception e) {
                    // 日志记录：建议用日志框架（SLF4J），这里简化打印
                    log.error("分片任务执行失败，当前分片索引：" + currentIdx + "-" + endIdx + "，异常信息：" + e.getMessage(),e);
                    errorMessages.add("分片任务执行失败，当前分片索引：" + currentIdx + "-" + endIdx + "，异常信息：" + e.getMessage());
                } finally {

                }
            }
            log.info(String.format("所有分片任务执行完成,成功的共%s,失败的共%s", String.valueOf(taskNumber - errorMessages.size()),String.valueOf(errorMessages.size())));
            if(!errorMessages.isEmpty()){
                throw new RuntimeException("插入数据时出现异常, 异常信息为: " + errorMessages.toString());
            }
        }

        return true;
    }



    /**
     * 任务添加指定的多个文件的词条, 并将词条存库(不通用，仅针对直接导入lang文件夹的词条服务)
     * @param i18nUrl
     * @param fileNameList
     * @param versionID
     * @param taskInfoEntity
     * @param department
     * @param userName
     */
    @Transactional
    public BatchCreateEntryResult createEntryFromTRFiles(String i18nUrl,List<String> fileNameList,TaskInfoEntity taskInfoEntity,List<String> targetLanguageTypes,String token,String backendTaskID){
        BatchCreateEntryResult createEntryResult = new BatchCreateEntryResult();
        Set<String> fileNameForNoEntryInserted = createEntryResult.getFileNameForNoEntryInserted(); // 实际没有导入任何词条的文件名称

        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("准备获取的词条翻译语种包括: %s", targetLanguageTypes.toString()));
        List<TLanguage> targetTLanguages = new ArrayList<>();
        for(String targetLanguage : targetLanguageTypes){   // 英文,俄文
            TLanguage searchTemplate = new TLanguage();
            searchTemplate.setName(targetLanguage);
            List<TLanguage> languages = languageMapper.getLanguages(searchTemplate);
            if(languages.isEmpty()){
                log.warn("警告: 没有找到对应语言的信息: " + targetLanguage);
                continue;
            }
            targetTLanguages.add(languages.get(0));
        }
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "语种信息获取成功");
        // Map<String,List<EntryInfoEntity>> wholeEntryInfoEntitiesMap = new HashMap<>();
        Map<String,String> fileNameErrorMessages = new HashMap<>();
        for(String fileName : fileNameList){

                // TODO Auto-generated method stub
            try {
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("准备导入文件: %s 的词条", fileName));
                log.info(String.format("准备导入文件: %s 的词条", fileName));
                fileNameForNoEntryInserted.add(fileName);
                List<String> _fileNameList = new ArrayList<>();
                _fileNameList.add(fileName);    // 限流
                /* 请求i18n获取对应文件的词条 */
                List<EntryInfoEntity> _entryInfoEntities = buildEntryFromTRFilesUsingI18nServer(_fileNameList, i18nUrl, taskInfoEntity, userName, targetTLanguages);
                List<EntryInfoEntity> filteredEntryInfoEntities = _entryInfoEntities.stream().filter(new Predicate<EntryInfoEntity>() {

                    @Override
                    public boolean test(EntryInfoEntity t) {
                        // TODO Auto-generated method stub
                        if(t.getEntry().length() > 512){
                            log.info(String.format("词条长度超过限制, 词条为: %s", t.getEntry()));
                            backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("词条长度超过限制, 词条为: %s", t.getEntry()));
                            createEntryResult.getFailedEntryInfoEntities().add(t);
                            return false;
                        }else{
                            return true;
                        }
                    }
                    
                }).collect(Collectors.toList());
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("成功导入文件: %s 的词条到缓存, 总共获取到的词条共: %s个, 其中不符合存库要求的词条共: %s 个", fileName,_entryInfoEntities.size(),_entryInfoEntities.size() - filteredEntryInfoEntities.size()));
                    
                
                log.info(String.format("文件: %s的词条准备存库", fileName));
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("准备将dic词条存库, 共%s个", filteredEntryInfoEntities.size()));
                if(!filteredEntryInfoEntities.isEmpty()){
                    entryUtils.caseExistEntry(filteredEntryInfoEntities, taskInfoEntity,executor);
                    createEntryInfoEntities(
                        filteredEntryInfoEntities,
                        taskInfoEntity, 
                        targetLanguageTypes, 
                        department,
                        consumerForImportEntitiesFromLangDirDirectly,
                        consumerForImportTranslatesFromLangDirDirectly
                    );
                    fileNameForNoEntryInserted.remove(fileName);
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("文件: %s的词条存库成功", fileName));
                }else{
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("文件: %s的没有词条需要存库", fileName));
                }
                
            } catch (Exception e) {
                // TODO: handle exception
                log.error("导入文件: " + fileName + "的词条时出错, " + e.getMessage(),e);
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "导入文件: " + fileName + "的词条时出错, " + e.getMessage());
                fileNameErrorMessages.put(fileName, e.getMessage());
            } finally{

            }

        }
        if(!fileNameErrorMessages.isEmpty()){
            /* 存在导入失败的文件 */
            backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "存在导入失败的dic文件,相关信息为: " + fileNameErrorMessages.toString());
            throw new RuntimeException("警告,存在导入失败的ts文件,相关信息为: " + fileNameErrorMessages.toString());
        }

        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "导入dic词条成功");
        createEntryResult.setSuccess(true);
        if(!fileNameForNoEntryInserted.isEmpty()){
            createEntryResult.setMessage(String.format("没有导入任何词条的词条来源(dic类型)有: %s", fileNameForNoEntryInserted.toString()));
        }
        return createEntryResult;
    }
    /**
     * (不通用，仅针对直接导入lang文件夹的词条服务)
     * @param i18nUrl
     * @param fileNameList
     * @param taskInfoEntity
     * @param targetLanguageTypes
     * @param request
     */
    @Transactional
    public BatchCreateEntryResult createEntryFromTSFiles(String i18nUrl,List<String> fileNameList,TaskInfoEntity taskInfoEntity,List<String> targetLanguageTypes,String token,String backendTaskID){
        BatchCreateEntryResult createEntryResult = new BatchCreateEntryResult();
        Set<String> fileNameForNoEntryInserted = createEntryResult.getFileNameForNoEntryInserted(); // 实际没有导入任何词条的文件名称


        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("准备获取的词条翻译语种包括: %s", targetLanguageTypes.toString()));

        List<TLanguage> targetTLanguages = new ArrayList<>();
        for(String targetLanguage : targetLanguageTypes){   // 英文,俄文
            TLanguage searchTemplate = new TLanguage();
            searchTemplate.setName(targetLanguage);
            List<TLanguage> languages = languageMapper.getLanguages(searchTemplate);
            targetTLanguages.add(languages.get(0));
        }
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "语种信息获取成功");


        Map<String,String> fileNameErrorMessages = new HashMap<>();
        for(String fileName : fileNameList){
                    // TODO Auto-generated method stub
            try {
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("准备导入文件: %s 的词条", fileName));

                log.info(String.format("准备导入文件: %s 的词条", fileName));
                fileNameForNoEntryInserted.add(fileName);
                List<String> _fileNameList = new ArrayList<>();
                _fileNameList.add(fileName);    // 限流
                /* 请求i18n获取对应文件的词条 */
                List<EntryInfoEntity> _entryInfoEntities = buildEntryFromTSFilesUsingI18nServer(_fileNameList, targetTLanguages, i18nUrl, taskInfoEntity, userName);
                List<EntryInfoEntity> filteredEntryInfoEntities = _entryInfoEntities.stream().filter(new Predicate<EntryInfoEntity>() {

                    @Override
                    public boolean test(EntryInfoEntity t) {
                        // TODO Auto-generated method stub
                        if(t.getEntry().length() > 512){
                            log.info(String.format("词条长度超过限制, 词条为: %s", t.getEntry()));
                            backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("词条长度超过限制, 词条为: %s", t.getEntry()));
                            
                            createEntryResult.getFailedEntryInfoEntities().add(t);
                            return false;
                        }else{
                            return true;
                        }
                    }
                    
                }).collect(Collectors.toList());
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("成功导入文件: %s 的词条到缓存, 总共获取到的词条共: %s个, 其中不符合存库要求的词条共: %s 个", fileName,_entryInfoEntities.size(),_entryInfoEntities.size() - filteredEntryInfoEntities.size()));
                log.info("准备将词条存库");
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("准备将ts词条存库, 共%s个", filteredEntryInfoEntities.size()));
                if(!filteredEntryInfoEntities.isEmpty()){
                    
                    entryUtils.caseExistEntry(filteredEntryInfoEntities, taskInfoEntity, executor);
                    createEntryInfoEntities(
                        filteredEntryInfoEntities,
                        taskInfoEntity, 
                        targetLanguageTypes, 
                        department,
                        consumerForImportEntitiesFromLangDirDirectly,
                        consumerForImportTranslatesFromLangDirDirectly
                    );
                    fileNameForNoEntryInserted.remove(fileName);
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("文件: %s词条成功存库", fileName));
                }else{
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("文件: %s没有词条需要存库", fileName));
                }
                
                                
            } catch (Exception e) {
                // TODO: handle exception
                log.error("导入文件: " + fileName + "的词条时出错, " + e.getMessage(),e);
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "导入文件: " + fileName + "的词条时出错, " + e.getMessage());
                fileNameErrorMessages.put(fileName, e.getMessage());
            } finally{
                
            }

        }
        if(!fileNameErrorMessages.isEmpty()){
            /* 存在导入失败的文件 */
            backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "存在导入失败的ts文件,相关信息为: " + fileNameErrorMessages.toString());
            throw new RuntimeException("警告,存在导入失败的ts文件,相关信息为: " + fileNameErrorMessages.toString());
        }

        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "导入ts词条成功");

        createEntryResult.setSuccess(true);
        if(!fileNameForNoEntryInserted.isEmpty()){
            createEntryResult.setMessage(String.format("没有导入任何词条的词条来源(ts类型)有: %s", fileNameForNoEntryInserted.toString()));
        }

        return createEntryResult;
    }





    public static class DictionaryVO{

        protected String comments;
        //词条
        protected String source;
        protected String tag;
        // "en_US": "dofodifdoi1",
        protected Map<String, String> translation;

        public DictionaryVO(){

        }

        public DictionaryVO(DictionaryVo dictionaryVo){
            this.source=  dictionaryVo.getSource();
            this.tag = dictionaryVo.getTag();
            this.comments = dictionaryVo.getComments();
            this.translation = dictionaryVo.getTranslation();
        }


        @Override
        public boolean equals(Object o) {
            // 1. 自反性：对象和自身相等
            if (this == o) return true;
            // 2. 非空性 + 类型一致性：排除 null 和不同类型
            if (o == null || getClass() != o.getClass()) return false;
            // 3. 强制类型转换
            DictionaryVO that = (DictionaryVO) o;
            // 4. 核心属性比较（使用 Objects.equals 处理 null）
            return Objects.equals(comments, that.getComments())
                    && Objects.equals(source, that.getSource())
                    && Objects.equals(tag, that.getTag());
        }

        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return Objects.hash(comments, source, tag);
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public Map<String, String> getTranslation() {
            return translation;
        }

        public void setTranslation(Map<String, String> translation) {
            this.translation = translation;
        }

    }

    public static class BatchCreateEntryResult{

        boolean isSuccess = false;

        String message;
    
        Set<String> fileNameForNoEntryInserted = new HashSet<>();

        public Set<String> getFileNameForNoEntryInserted() {
            return fileNameForNoEntryInserted;
        }

        List<EntryInfoEntity> failedEntryInfoEntities = new ArrayList<>();

        public List<EntryInfoEntity> getFailedEntryInfoEntities() {
            return failedEntryInfoEntities;
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public void setSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }
    

}
