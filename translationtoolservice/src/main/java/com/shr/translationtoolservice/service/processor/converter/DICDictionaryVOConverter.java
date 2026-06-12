package com.shr.translationtoolservice.service.processor.converter;

import java.util.Date;
import java.util.List;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DICDictionaryVOConverter extends DictionaryVOConverter{


    protected List<TLanguage> languageList;

    protected String userName;

    protected String diFileName;

    public DICDictionaryVOConverter(
        String entrySource, 
        String taskID, 
        String productID, 
        String versionID,
        CommonUtils commonUtils, 
        List<TLanguage> languageList, 
        String userName, 
        String diFileName) {

        super(entrySource, taskID, productID, versionID, commonUtils);
        
        this.languageList = languageList;
        this.userName = userName;
        this.diFileName = diFileName;
    }

    @Override
    public EntryInfoEntity apply(DictionaryVO dictionaryVO) {
        EntryInfoEntity entryInfoEntity = this.constructBasicAttributesForEntryInfoEntitiy(dictionaryVO, languageList);
        entryInfoEntity.setEntrySource(entrySource);
        entryInfoEntity.setDiFileName(diFileName);
        entryInfoEntity.setWriteType(ConstantInterface.DI);
        entryInfoEntity.setImportType(ConstantInterface.DI);
        entryInfoEntity.setEntryState(1);
        entryInfoEntity.setTaskId(taskID);
        entryInfoEntity.setProductID(productID);
        entryInfoEntity.setVersionID(versionID);
        entryInfoEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        entryInfoEntity.setUpdate(userName);
        return entryInfoEntity;
    }

    
}
