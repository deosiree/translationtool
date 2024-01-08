package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    String taskEntryExport( String taskID);
}
