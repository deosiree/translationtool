package com.shr.translationtoolservice.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.service.EntryStorageService;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;
import com.shr.translationtoolservice.util.LocalTimeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EntryStorageServiceImpl implements EntryStorageService {

    @Autowired
    private LocalTimeUtils localTimeUtils;

    @Autowired
    protected EntryInfoMapper entryInfoMapper;

    @Autowired
    protected TranslateMapper translateMapper;

    @Autowired
    protected BatchInsertEntryHandler batchInsertEntryHandler;

    protected static final Map<String,String> TRANSLATE_GETTER_METHOD_MAP = ConstantInterface.entryInfoEntityGetterTranslateMap();
    
    protected static final Map<String,String> TRANSLATE_SETTER_METHOD_MAP = ConstantInterface.entryInfoEntitySetterTranslateMap();

    protected final boolean processUpdateResult(int updateRows,int expectUpdateRows){
        return updateRows == expectUpdateRows;
    }

    @Override
    public EntryInfoEntity buildUpdateEntryInfoTemplate(String entryID, Set<MethodEntity> setMethodValues) {
        if(setMethodValues == null || setMethodValues.isEmpty()){
            return null;
        }
        EntryInfoEntity updateEntryInfoTemplate = new EntryInfoEntity();
        updateEntryInfoTemplate.setId(entryID);
        updateEntryInfoTemplate.setUpdateTime(localTimeUtils.getBeijingTime()); 
        setMethodValues.forEach((methodEntity)->{methodEntity.invoke(updateEntryInfoTemplate);});
        return updateEntryInfoTemplate;
    }

    @Override
    public EntryInfoEntity buildUpdateEntryInfoTemplate(String entryID, Map<Method, Object> setMethodValueMap) {
        if(setMethodValueMap == null || setMethodValueMap.isEmpty()){
            return null;
        }
        EntryInfoEntity updateEntryInfoTemplate = new EntryInfoEntity();
        updateEntryInfoTemplate.setId(entryID);
        updateEntryInfoTemplate.setUpdateTime(localTimeUtils.getBeijingTime());

        setMethodValueMap.forEach((method,value)->{
            try {
                method.invoke(updateEntryInfoTemplate, value);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(String.format("该对象无法调用方法: %s", method.getName()));
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(String.format("方法: %s对应的参数不符合要求, 请检查", method.getName()));
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
        });
        return updateEntryInfoTemplate;
    }

    @Transactional
    @Override
    public boolean updateEntryInfo(EntryInfoEntity entryInfoEntity) {
        int updateResult = entryInfoMapper.updateEntryInfo(entryInfoEntity);
        return this.processUpdateResult(updateResult, 1);
    }

    @Transactional
    @Override
    public boolean updateEntryInfos(Collection<EntryInfoEntity> entryInfoEntities) {
        int updateCount = this.entryInfoMapper.batchUpdateEntryInfo(entryInfoEntities);
        return true;

    }

    
    
}
