package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.TaskImportEntryEntity.TaskImportEntryVO;
import com.shr.translationtoolservice.entity.TaskInfoEntity.TaskInfoEntityVO;
import com.shr.translationtoolservice.entity.TaskStateEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface TaskInfoService extends IService<TaskInfoEntity> {

    List<TaskInfoEntity> getTaskInfo(TaskInfoEntity taskInfoEntity, Integer offset, Integer pageSize, HttpServletRequest request);

    int getTotalNum(TaskInfoEntity taskInfoEntity);

    String addTaskInfoList(List<TaskInfoVo> taskInfoVoList, HttpServletRequest request);

    List<TaskInfoEntity> addTaskInfoListAndSubmit(List<TaskInfoVo> taskInfoVoList, String token);

    String updateTaskInfo(TaskInfoVo taskInfoVo);

    String deleteTaskInfo(List<String> taskIds);

    String taskSubmission(List<String> taskIDs, String oldState, String nextState);

    List<TaskInfoEntity> getTaskInfoByVersion(TaskInfoEntity taskInfoEntity, int offset, Integer pageSize);

    List<TaskInfoEntityVO> getToDoTaskInfo(int offset, Integer pageSize, HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    int getToDoTaskInfoTotal(HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    List<TaskInfoEntity> getFinishTaskInfo(int offset, Integer pageSize, HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    int getFinishTaskInfoTotal(HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    void taskEntryExport( String taskID,HttpServletResponse response,String importType);

    String taskCreateNewLanguageTask(TaskInfoEntity taskInfoEntity,String taskID);

    Map<String, String> getImportType(String taskID);

    void exportEntryByTaskId(String taskId, HttpServletResponse response);

    String putTempToProductTable(List<EntryTempEntity> entryTempEntities);

    String createTaskAndCreateEntryByLangDir(
        String i18nAddress,
        List<TaskInfoVo> taskInfoVos,
        Map<String,String> taskDirMap,
        String token,
        List<String> targetLanguageTypes,
        String backendTaskID,
        Map<String,Object> otherArgs
    );

    String createEntryByLangDirForTaskInfos(
        String i18nAddress,
        List<TaskInfoEntity> taskInfoEntities,
        Map<String,String> taskDirMap,
        String token,
        List<String> targetLanguageTypes,
        String backendTaskID,
        Map<String,Object> otherArgs
    );


    /**
     * 
     * @param i18nAddress
     * @param taskInfoFileMaps  key: 任务名 ,value : ["tr/i18n_test","tr/gui_i18n_tool"]
     * @param token
     * @param targetLanguageTypes
     * @param backendTaskID
     * @return
     */
    String createEntryByLangDirForTaskInfos(
        String i18nAddress,
        Collection<TaskImportEntryVO>  taskImportEntryVOs,
        String token,
        List<String> targetLanguageTypes,
        String backendTaskID
    );
    /**
     * 获取各任务处于不同阶段的词条个数
     * @param taskIDs
     * @return
     */
    List<TaskStateEntity> countEntryTranslateStateForTasks(Set<String> taskIDs);

}
