package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 *
 */
public interface EntryTempService extends IService<EntryTempEntity> {

    String insertEntry(List<EntryTempEntity> tempEntities);

    String updateEntryTemp(List<EntryTempEntity> tempEntities);

    List<EntryTempEntity> getEntryTempByTaskID(String taskID, int offset, Integer pageSize);

    String deleteEntryTempByID(List<String> entryID);

    int getEntryTempByTaskIDTotal(String taskID);

}
