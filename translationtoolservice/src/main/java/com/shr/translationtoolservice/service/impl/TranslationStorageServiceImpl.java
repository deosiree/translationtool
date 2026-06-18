package com.shr.translationtoolservice.service.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.reflect.TypeToken;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.KeyValueArguments;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.DO.EntryInfoEntityDO;
import com.shr.translationtoolservice.service.EntryStorageService;
import com.shr.translationtoolservice.service.TranslationStorageService;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler;
import com.shr.translationtoolservice.util.EntryUtils;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TranslationStorageServiceImpl implements TranslationStorageService{

    @Autowired
    protected EntryInfoMapper entryInfoMapper;

    @Autowired
    protected TranslateMapper translateMapper;

    @Autowired
    protected EntryStorageService entryStorageService;

    @Autowired
    protected BatchInsertEntryHandler batchInsertEntryHandler;

    @Autowired
    protected EntryUtils entryUtils;

    protected ThreadPoolExecutor executor = new ThreadPoolExecutor(80, 120, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());


    protected static final Map<String,String> TRANSLATE_GETTER_METHOD_MAP = ConstantInterface.entryInfoEntityGetterTranslateMap();
    
    protected static final Map<String,String> TRANSLATE_SETTER_METHOD_MAP = ConstantInterface.entryInfoEntitySetterTranslateMap();

    protected static final Map<String,String> SET_TRANS_ID_METHOD_MAP = ConstantInterface.entryInfoEntitySetTranslateIDMethodMap();

    protected static final Map<String,String> GET_TRANS_ID_METHOD_MAP = ConstantInterface.entryInfoEntityGetTranslateIDMethodMap();


    @Transactional(rollbackFor = Exception.class)
    protected boolean updatetTranslationsInternal(Collection<EntryInfoEntity> updateEntryInfoTemplates,Collection<TranslateEntity> translateEntities) {
        // 使用常量定义批次大小
        final int BATCH_SIZE = 500;
        
        try {
            // 处理词条信息更新
            if (updateEntryInfoTemplates != null && !updateEntryInfoTemplates.isEmpty()) {
                List<EntryInfoEntity> updateEntryInfoTemplatesList = new ArrayList<>(updateEntryInfoTemplates);
                int totalSize = updateEntryInfoTemplatesList.size();
                // 添加日志，便于监控
                log.debug("开始批量更新词条数据，总条数：{}，批次大小：{}", totalSize, BATCH_SIZE);
                
                for (int i = 0; i < totalSize; i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, totalSize);
                    List<EntryInfoEntity> batch = updateEntryInfoTemplatesList.subList(i, end);
                    
                    try {
                        boolean updateEntryInfosSuccessfully = entryStorageService.updateEntryInfos(batch);  // 更新transID, 只要正常返回就是true
                        if(!updateEntryInfosSuccessfully){
                            throw new RuntimeException(String.format("更新词条信息失败"));
                        }
                        log.debug("第 {} 批数据更新成功，条数：{}", i / BATCH_SIZE, batch.size());
                    } catch (Exception e) {
                        log.error("更新词条信息失败，批次：{}，起始位置：{}", i / BATCH_SIZE, i, e);
                        throw new RuntimeException("批量更新词条信息失败", e);
                    }
                }
                
                log.info("词条信息更新完成，总条数：{}", totalSize);
            }
            // 批量插入翻译数据
            if (translateEntities != null && !translateEntities.isEmpty()) {
                List<TranslateEntity> entityList = new ArrayList<>(translateEntities);
                int totalSize = entityList.size();
                
                // 添加日志，便于监控
                log.debug("开始批量插入翻译数据，总条数：{}，批次大小：{}", totalSize, BATCH_SIZE);
                
                for (int i = 0; i < totalSize; i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, totalSize);
                    List<TranslateEntity> batch = entityList.subList(i, end);
                    
                    try {
                        this.translateMapper.batchInsertTranslate(batch);
                        log.debug("第 {} 批数据插入成功，条数：{}", i / BATCH_SIZE, batch.size());
                    } catch (Exception e) {
                        log.error("批量插入失败，批次：{}，起始位置：{}", i / BATCH_SIZE, i, e);
                        throw new RuntimeException("批量插入翻译数据失败", e);
                    }
                }
                
                log.info("翻译数据批量插入完成，总条数：{}", totalSize);
            }
            return true;
            
        } catch (Exception e) {
            log.error("updatetTranslationsInternal 执行失败", e);
            throw e;  // 保持原异常抛出逻辑
        }
    }

    

    private Map<Method,Method> buildTransTypeMethodRelationMap(Class<?> clazz,Collection<String> transTypes){
        
        Map<String, String> translateFieldMap = ConstantInterface.translateFieldMap();
        Map<Method,Method> translateMethodMap = new HashMap<>();    // {"getEnglish": "setEnglish"}
        transTypes.stream().forEach((transType)->{
            String fieldName = translateFieldMap.get(transType);
            Method getMethod = MethodUtils.acquireMethod(clazz, fieldName, MethodUtils.DEFAULT_GET_METHOD_NAME_GENERATOR);
            Method setMethod = MethodUtils.acquireMethod(clazz, fieldName, MethodUtils.DEFAULT_SET_METHOD_NAME_GENERATOR,String.class);
            translateMethodMap.put(getMethod, setMethod);
        });
        return translateMethodMap;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateEntryInfoTranslations(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> transTypes,KeyValueArguments<String> kwargs) {
        if(entryInfoEntities == null || transTypes == null || transTypes.isEmpty()){
            throw new RuntimeException("没有同时提供更新的词条信息和要更新的语种信息");
        }
        Class<EntryInfoEntity> clazz = EntryInfoEntity.class;
        Map<Method, Method> transTypeMethodRelationMap = this.buildTransTypeMethodRelationMap(clazz, transTypes);
        
        List<EntryInfoEntity> entryInfosDB = entryInfoMapper.selectEntryInfosByIDs(entryInfoEntities.stream().map(EntryInfoEntity::getId).collect(Collectors.toList()));
        Function<EntryInfoEntity,String> func = (entry)->{return entry.getId();};
        /* 更新翻译用的词条的模板, 还需要添加翻译信息 */
        Map<String,List<EntryInfoEntity>> entryInfosDBMap = entryInfosDB.stream().collect(Collectors.groupingBy(func));
        List<EntryInfoEntity> buildTransEntryInfoTemplates = new ArrayList<>();
        for(EntryInfoEntity entryInfoEntity : entryInfoEntities){
            String entryID = entryInfoEntity.getId();
            if(entryID == null || entryID.trim().isEmpty()){
                throw new RuntimeException("更新词条的词条ID为null, 检查传参");
            }
            Set<MethodEntity> setMethodValues = new HashSet<>();
            /* 文件中该词条每种语言的翻译结果 */
            transTypeMethodRelationMap.forEach((getterMethod,setterMethod)->{
                try {
                    String translation = getterMethod.invoke(entryInfoEntity) != null ? String.valueOf(getterMethod.invoke(entryInfoEntity)) : null;

                    if(translation == null || translation.trim().isEmpty()){
                        return; // 不会将词条的翻译更新为null或""
                    }
                    MethodEntity setTransMethodEntity = new MethodEntity(setterMethod, translation);
                    setMethodValues.add(setTransMethodEntity);
                } catch(Exception e){
                    throw new RuntimeException(String.format("更新词条翻译获取新的翻译时出现异常, 异常信息为: %s", e.getMessage()));
                }
            });
            if(!entryInfosDBMap.containsKey(entryID)){
                /* 词条不存在  */
                log.info(String.format("更新词条翻译, id为: %s的词条不存在", entryID));
                continue;
            }
            EntryInfoEntity buildTransEntryInfoTemplate = entryInfosDBMap.get(entryID).get(0);
            if(buildTransEntryInfoTemplate.getIsDelete() == 1){
                /* 词条被删掉了, 不存在  */
                log.info(String.format("更新词条翻译, id为: %s的词条已删除, 无法更新翻译", entryID));
                continue;
            }
            setMethodValues.stream().forEach((setMethodValue)->{setMethodValue.invoke(buildTransEntryInfoTemplate);});
            buildTransEntryInfoTemplates.add(buildTransEntryInfoTemplate);  // buildTranslateEntity 需要利用完整的词条信息
        }
        List<TranslateEntity> translateEntities = this.batchInsertEntryHandler.buildTranslateEntity(buildTransEntryInfoTemplates, new ArrayList<>(transTypes), kwargs.get("department", String.class),this.executor);
        Consumer<TranslateEntity> translateEntiityProcessor = kwargs != null ? kwargs.get("translateProcessor", new TypeToken<Consumer<TranslateEntity>>(){}) : null;
        if(translateEntiityProcessor != null){
            translateEntities.stream().forEach(translateEntiityProcessor);
        }

        List<EntryInfoEntity> updateEntryInfoTemplates = buildTransEntryInfoTemplates.stream().map((entry)->{
            String entryID = entry.getId();
            Map<Method,Object> setTransIDMethodValueMap = new HashMap<>();
            for(String transType : transTypes){
                // String transType = translationPair.getKey();
                String getTransIDMethodName = GET_TRANS_ID_METHOD_MAP.get(transType);
                String setTransIDMethodName = SET_TRANS_ID_METHOD_MAP.get(transType);
                try {
                    Method getTransIDMethod = clazz.getMethod(getTransIDMethodName);
                    String transID = String.valueOf(getTransIDMethod.invoke(entry));
                    Method setTransIDMethod = clazz.getMethod(setTransIDMethodName, String.class);

                    setTransIDMethodValueMap.put(setTransIDMethod,transID);
                    
                } catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
            EntryInfoEntity updateEntryInfoTemplate = entryStorageService.buildUpdateEntryInfoTemplate(entryID,setTransIDMethodValueMap);
            return updateEntryInfoTemplate;
        }).collect(Collectors.toList());

        return this.updatetTranslationsInternal(updateEntryInfoTemplates, translateEntities);
    }
}
