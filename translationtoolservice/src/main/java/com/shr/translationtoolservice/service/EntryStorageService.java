package com.shr.translationtoolservice.service;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.service.impl.StorageService;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;


/**
 * 词条存储相关服务(与DAO层交互)
 */
public interface EntryStorageService extends StorageService {

    /**
     * 设定指定要更新的属性, 对库中该属性的值进行更新， Method对应的是EntryInfoEntitiy的方法
     * @param entryID
     * @param setMethodValues
     * @return  如果setMethodValueMap为null或空, 则返回null,
     */
    EntryInfoEntity buildUpdateEntryInfoTemplate(String entryID, Set<MethodEntity> setMethodValues);

    /**
     * 设定指定要更新的属性, 对库中该属性的值进行更新， Method对应的是EntryInfoEntitiy的方法
     * @param entryID
     * @param setMethodValueMap    EntryInfoEntity对应的属性名, 如果为null或者map为空，返回false
     * @return  如果setMethodValueMap为null或空, 则返回null,
     */
    EntryInfoEntity buildUpdateEntryInfoTemplate(String entryID,Map<Method,Object> setMethodValueMap);

    /**
     * 利用提供的词条entity, 更新指定属性的值(不包含翻译信息)
     * @param entryInfoEntity
     * @return 成功更新，返回{@code true}, 否则为{@code false}
     */
    boolean updateEntryInfo(EntryInfoEntity entryInfoEntity);

    /**
     * 调用{@link #updateEntryInfo(EntryInfoEntity)} 更新，更新指定属性的值(不包含翻译信息)
     * @param entryInfoEntities
     * @return 全部更新成功, 返回{@code true}, 否则为{@code false}
     */
    boolean updateEntryInfos(Collection<EntryInfoEntity> entryInfoEntities);


}
