package com.shr.translationtoolservice.service;


/**
 * 词条存储相关服务(与DAO层交互)
 */
public interface EntryStorageService {
    

    boolean updateEntryInfoTransRelation(String entryID,String translation,String transType);

}
