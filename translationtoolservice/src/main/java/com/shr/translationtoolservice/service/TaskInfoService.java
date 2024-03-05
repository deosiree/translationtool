package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface TaskInfoService extends IService<TaskInfoEntity> {

    List<TaskInfoEntity> getTaskInfo(TaskInfoEntity taskInfoEntity, Integer offset, Integer pageSize, HttpServletRequest request);

    int getTotalNum(TaskInfoEntity taskInfoEntity);

    String addTaskInfoList(List<TaskInfoVo> taskInfoVoList, HttpServletRequest request);

    String updateTaskInfo(TaskInfoVo taskInfoVo);

    String deleteTaskInfo(List<String> taskIds);

    String taskSubmission(List<String> taskIDs, String oldState, String nextState);

    List<TaskInfoEntity> getTaskInfoByVersion(TaskInfoEntity taskInfoEntity, int offset, Integer pageSize);

    List<TaskInfoEntity> getToDoTaskInfo(int offset, Integer pageSize, HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    int getToDoTaskInfoTotal(HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    List<TaskInfoEntity> getFinishTaskInfo(int offset, Integer pageSize, HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    int getFinishTaskInfoTotal(HttpServletRequest request,TaskInfoEntity taskInfoEntity);

    void taskEntryExport( String taskID,HttpServletResponse response,String importType);

    String taskCreateNewLanguageTask(TaskInfoEntity taskInfoEntity,String taskID);

    Map<String, String> getImportType(String taskID);

    void exportEntryByTaskId(String taskId, HttpServletResponse response);

    String putTempToProductTable(List<EntryTempEntity> entryTempEntities);
}
