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

    String taskSubmission(List<String> taskIDs, int oldState, int nextState);
}
