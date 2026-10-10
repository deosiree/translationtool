package com.shr.translationtoolservice.service.processor.converter;

import java.util.Date;
import java.util.Set;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DICDictionaryVOConverter extends DictionaryVOConverter{

    protected String entrySource;

    protected String taskID;

    protected String productID;

    protected String versionID;

    protected String userName;

    protected String diFileName;

    public DICDictionaryVOConverter(
        String entrySource, 
        String taskID, 
        String productID, 
        String versionID,
        CommonUtils commonUtils, 
        Set<TLanguage> tLanguages,
        String userName, 
        String diFileName) {

        super(tLanguages, commonUtils);
        
        this.entrySource = entrySource;
        this.taskID = taskID;
        this.productID = productID;
        this.versionID = versionID;
        this.userName = userName;
        this.diFileName = diFileName;
    }

    @Override
    public EntryInfoEntity apply(DictionaryVO dictionaryVO) {
        EntryInfoEntity entryInfoEntity = super.apply(dictionaryVO);
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
