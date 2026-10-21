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

    String deleteEntryInfoByIDs(List<String> entryIDs, String productID,String versionID, String taskID);

    int getEntryTempByTaskIDTotal(String taskID);

    List<EntryInfoEntity> preTranslateForTask(HttpServletRequest request, List<EntryInfoEntity> entryInfoEntities,String taskID,String priority);

    List<EntryInfoEntity> preTranslateByType(HttpServletRequest request, List<EntryInfoEntity> entryInfoEntities,String translateType,String priority);

    void getTemplateFile(HttpServletResponse response,String fileType ,String translateType);

    List<EntryInfoEntity> getEntryInfoList(String taskID,String entryState, List<String> transStates,String entry);

    /**
     * 更新对应任务的词条信息,返回更新失败的词条信息
     * @param entryInfoEntities
     * @param taskID
     * @param request
     * @return 更新失败的词条信息
     */
    List<EntryInfoEntity> updateEntryList(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request);

    String deleteEntryInfoByTaskID(List<String> entryID, String taskID);

    //  ImportResultEntryVO checkExistEntry(List<EntryTempEntity> entryTempEntities);
}
