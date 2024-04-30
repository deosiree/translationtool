package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.ImportResultEntryVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 *
 */
public interface EntryTempService extends IService<EntryTempEntity> {

    String insertEntry(List<EntryInfoEntity> entryInfoEntities);

    String updateEntryTemp(List<EntryTempEntity> tempEntities);

    List<EntryTempEntity> getEntryTempByTaskID(String taskID);

    String deleteEntryInfoByID(List<String> entryID);

    int getEntryTempByTaskIDTotal(String taskID);

    List<EntryInfoEntity> preTranslate( List<EntryInfoEntity> entryInfoEntities,String taskID,String priority);

    void getTemplateFile(HttpServletResponse response,String fileType       );

    List<EntryInfoEntity> getEntryInfoList(String taskID,String entryState, List<String> transStates,String entry);

    String updateEntryList(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request);

    //  ImportResultEntryVO checkExistEntry(List<EntryTempEntity> entryTempEntities);
}
