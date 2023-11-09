package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
public interface TaskInfoService extends IService<TaskInfoEntity> {

    List<TaskInfoEntity> getTaskInfo(TaskInfoEntity taskInfoEntity, Integer offset, Integer pageSize,HttpServletRequest request);

    int getTotalNum();

    String addTaskInfoList(List<TaskInfoEntity> taskInfoEntities, HttpServletRequest request);

    String updateTaskInfo(TaskInfoEntity taskInfoEntity);

    String deleteTaskInfo(List<String> taskIds);

    String taskSubmission(List<String> taskIDs, int oldState, int nextState);
}
