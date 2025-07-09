package com.shr.translationtoolservice.util;

import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class EntryUtils {
    @Autowired
    private TaskInfoMapper taskInfoMapper;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private EntryProcessUtils entryProcessUtils;
    public void caseExistEntry(List<EntryInfoEntity> newEntry, String taskID) {


    TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

    //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
    String productTableName = "t_entry_info";

    for (EntryInfoEntity entryInfoEntity : newEntry) {
        if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())) {
            caseExistEntry(entryInfoEntity.getChildren(), taskID);
        }
        // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());


        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());



        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersion(1);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
        } else {
            entryInfoEntity.setIsExist(1);
            entryInfoEntity.setEntryVersion(entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion());
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
        }
    }

}

    public void caseNewEntry(List<EntryInfoEntity> exportEntry, String taskID,ArrayList<EntryInfoEntity> newEntry ) {


        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        String productTableName = "t_entry_info";

        for (EntryInfoEntity entryInfoEntity : exportEntry) {
            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())) {
                caseNewEntry(entryInfoEntity.getChildren(), taskID,newEntry);
            }
            // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());


            List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());



            if (CollectionUtils.isEmpty(entryEntities)) {
                //创建新翻译
                entryInfoEntity.setIsExist(0);
                entryInfoEntity.setEntryVersion(1);
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
                newEntry.add(entryInfoEntity);
            }
        }

    }

    public void createNewTrans(EntryInfoEntity entryInfoEntity, String translateType, String translate) {
        //写入翻译字段
        switch (translateType) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setChinese(translate);
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
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

    public List<EntryInfoEntity> buildRepeTempEntry(List<EntryInfoEntity> entryInfoEntities, String translateType) {
        return entryProcessUtils.buildRepeEntry(entryInfoEntities, translateType);
    }

}
