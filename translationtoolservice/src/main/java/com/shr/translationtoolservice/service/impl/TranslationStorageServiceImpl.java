package com.shr.translationtoolservice.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
import com.shr.translationtoolservice.service.EntryStorageService;
import com.shr.translationtoolservice.service.TranslationStorageService;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler;

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

    protected static final Map<String,String> TRANSLATE_GETTER_METHOD_MAP = ConstantInterface.entryInfoEntityGetterTranslateMap();
    
    protected static final Map<String,String> TRANSLATE_SETTER_METHOD_MAP = ConstantInterface.entryInfoEntitySetterTranslateMap();

    protected static final Map<String,String> SET_TRANS_ID_METHOD_MAP = ConstantInterface.entryInfoEntitySetTranslateIDMethodMap();

    protected static final Map<String,String> GET_TRANS_ID_METHOD_MAP = ConstantInterface.entryInfoEntityGetTranslateIDMethodMap();

    
    
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateEntryInfoTranslation(String entryID, Map<String, String> translations,KeyValueArguments<String> kwargs) {
        if(entryID == null || entryID.trim().isEmpty()){
            throw new RuntimeException(String.format("没有提供更新的词条ID, 无法进行更新"));
        }
        if(translations == null || translations.isEmpty()){
            throw new RuntimeException(String.format("没有提供更新的翻译, 无法进行更新"));
        }
        /**
         *  更新翻译，创建翻译的功能应当保证同步，多线程并发会有问题
         */
        EntryInfoEntity selectTemplate = new EntryInfoEntity();
        selectTemplate.setId(entryID);
        EntryInfoEntity originalEntryInfo = entryInfoMapper.selectEntryById(selectTemplate);
        if(originalEntryInfo == null){
            log.debug(String.format("没有找到对应的词条, 词条的查询条件为: %s", selectTemplate.toString()));
            return false;
        }
        List<EntryInfoEntity> entryInfos = new ArrayList<>();
        entryInfos.add(originalEntryInfo);

        Map<Method, Object> setMethodValueMap = new HashMap<>();    // 设定entryInfoEntitiy的翻译的属性
        /* 更新t_entry_info的trans_id */
        Class<? extends EntryInfoEntity> clazz = originalEntryInfo.getClass();
        // {'英文':'xxxtransa'}
        translations.forEach((transType,translation)->{
            // 英文,"xxx_test "
            String methodName = TRANSLATE_SETTER_METHOD_MAP.get(transType);
            if(methodName == null || methodName.trim().isEmpty()){
                throw new RuntimeException(String.format("没有找到翻译类型: %s对应的设定翻译的方法, 请检查翻译类型", transType));
            }
            try {
                Method setMethod = clazz.getMethod(methodName, String.class);
                setMethodValueMap.put(setMethod, translation);
                setMethod.invoke(originalEntryInfo, translation);    
            } catch(Exception e){
                throw new RuntimeException(e);
            }

        });
        // 插入t_entry_info和t_translate要保证事务原子性, 否则会出现数据不一致的情况
        /* 向t_translate表创建新的记录 */
        List<TranslateEntity> translateEntities = batchInsertEntryHandler.buildTranslateEntity(entryInfos,new ArrayList<>(translations.keySet()),kwargs.get("department", String.class));
        Consumer<TranslateEntity> translateEntiityProcessor = kwargs != null ? kwargs.get("translateProcessor", new TypeToken<Consumer<TranslateEntity>>(){}) : null;
        if(translateEntiityProcessor != null){
            translateEntities.stream().forEach(translateEntiityProcessor);
        }
        entryInfos.stream().forEach((entry)->{
            Map<Method,Object> setTransIDMethodValueMap = new HashMap<>();
            for(Map.Entry<String,String> translationPair : translations.entrySet()){
                String transType = translationPair.getKey();
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
            EntryInfoEntity updateEntryInfoTemplate = entryStorageService.getUpdateEntryInfoTemplate(entryID,setTransIDMethodValueMap);
            boolean updateResult = entryStorageService.updateEntryInfo(updateEntryInfoTemplate);
            if(!updateResult){
                throw new RuntimeException(String.format("更新词条的相关信息失败, 请稍后重试, 更新的词条的查询条件为: %s", updateEntryInfoTemplate.toString()));    // 可能的原因，更新过程中词条被删除掉,
            }
        });

        translateEntities.forEach((translateEntity)->{
        
            int insertTranslate = this.translateMapper.insertTranslate(translateEntity);
            if(insertTranslate != 1){
                throw new RuntimeException("插入t_translate数据失败");
            }
        });

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateEntryInfoTranslations(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> transTypes,KeyValueArguments<String> kwargs) {
        if(entryInfoEntities == null || transTypes == null || transTypes.isEmpty()){
            throw new RuntimeException("没有同时提供更新的词条信息和要更新的语种信息");
        }
        Class<EntryInfoEntity> clazz = EntryInfoEntity.class;
        Map<String,Method> transTypeGetterMethodMap = new HashMap<>();
        transTypes.stream().forEach((transType)->{
            String getMethodName = TRANSLATE_GETTER_METHOD_MAP.get(transType);
            if(getMethodName == null || getMethodName.trim().isEmpty()){
                throw new RuntimeException(String.format("没有找到翻译类型对应的getTranslate方法: %s", transType));
            }
            try {
                Method method = clazz.getMethod(getMethodName);
                transTypeGetterMethodMap.put(transType, method);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("没有找到对应的方法, 方法名为: %s", getMethodName));
            } catch (SecurityException e) {
                throw new RuntimeException(String.format("类与方法不匹配, 类名为 :%s, 方法名为: %s", clazz.getName(),getMethodName));
            } catch (Exception e){
                throw new RuntimeException(String.format("出现其他异常, 异常信息为: %s", e.getMessage()));
            }
            
        });
        boolean updateAllSuccessfully = true;
        for(EntryInfoEntity entryInfoEntity : entryInfoEntities){
            String entryID = entryInfoEntity.getId();
            if(entryID == null || entryID.trim().isEmpty()){
                throw new RuntimeException("更新词条的词条ID为null, 检查传参");
            }
            Map<String,String> translations = new HashMap<>();
            transTypeGetterMethodMap.forEach((transType,method)->{
                try {
                    String translation = String.valueOf(method.invoke(entryInfoEntity));

                    if(translation == null || translation.trim().isEmpty()){
                        return; // 不会将词条的翻译更新为null或""
                    }
                    translations.put(transType, translation);
                } catch(Exception e){
                    throw new RuntimeException(String.format("更新词条翻译获取新的翻译时出现异常, 异常信息为: %s", e.getMessage()));
                }

            });
            boolean updateEntryInfoTranslation = this.updateEntryInfoTranslation(entryID, translations, kwargs);
            if(!updateEntryInfoTranslation){
                updateAllSuccessfully = updateEntryInfoTranslation;
                // throw new RuntimeException(String.format("更新失败, 更新的词条信息为: %s", entryInfoEntity.toString()));
            }
        }
        return updateAllSuccessfully;
        
    }

}
