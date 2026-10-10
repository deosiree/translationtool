package com.shr.translationtoolservice.service.processor.converter;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TSDictionaryVOConverter extends DictionaryVOConverter {

    protected String entrySource;

    protected String taskID;

    protected String productID;

    protected String versionID;

    protected String userName;


    public TSDictionaryVOConverter(
        String entrySource, 
        String taskID, 
        String productID, 
        String versionID,
        CommonUtils commonUtils, 
        String userName, 
        Set<TLanguage> targetLanguages) {
            
        super(targetLanguages, commonUtils);
        this.userName = userName;
        this.entrySource = entrySource;
        this.taskID = taskID;
        this.productID = productID;
        this.versionID = versionID;
    }


    @Override
    public EntryInfoEntity apply(DictionaryVO dictionaryVO) {
        EntryInfoEntity entryInfoEntity = super.apply(dictionaryVO);

        entryInfoEntity.setEntrySource(entrySource);
        entryInfoEntity.setEntryState(1);
        entryInfoEntity.setTaskId(taskID);
        entryInfoEntity.setUpdate(userName);
        entryInfoEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        entryInfoEntity.setProductID(productID);
        entryInfoEntity.setVersionID(versionID);
        entryInfoEntity.setImportType(ConstantInterface.TS);
        entryInfoEntity.setWriteType(ConstantInterface.TS);
        entryInfoEntity.setIsDelete(0);
        return entryInfoEntity;
    }

}
