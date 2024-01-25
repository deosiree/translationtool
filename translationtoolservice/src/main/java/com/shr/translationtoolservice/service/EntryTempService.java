package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 *
 */
public interface EntryTempService extends IService<EntryTempEntity> {

    String insertEntry(List<EntryTempEntity> tempEntities);

    String updateEntryTemp(List<EntryTempEntity> tempEntities);

    List<EntryTempEntity> getEntryTempByTaskID(String taskID);

    String deleteEntryTempByID(List<String> entryID);

    int getEntryTempByTaskIDTotal(String taskID);

    List<EntryTempEntity> preTranslate(String taskID);

    void getTemplateFile(HttpServletResponse response);
}
