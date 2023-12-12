package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.TaskInfoEntity
 */
@Mapper
public interface TaskInfoMapper extends BaseMapper<TaskInfoEntity> {

    List<TaskInfoEntity> getTaskInfo(@Param("taskInfoEntity") TaskInfoEntity taskInfoEntity, @Param("offset") Integer offset, @Param("limit") Integer pageSize);


    int insertList(List<TaskInfoEntity> taskInfoEntities);

    int taskSubmission(@Param("taskIDs") List<String> taskIDs,@Param("nextState") int nextState,@Param("date")  Date date);

    int deleteByIds(@Param("idList")  List<String> taskIds);

    int getTaskInfoTotal(@Param("taskInfoEntity") TaskInfoEntity taskInfoEntity);

    List<TaskInfoEntity> getTaskInfoByVersion(TaskInfoEntity taskInfoEntity, int offset, Integer pageSize);

    List<TaskInfoEntity> getTaskInfoByUserName(@Param("userName") String userName, @Param("offset") Integer offset, @Param("limit") Integer pageSize);

    int getToDoTaskInfoTotal(String userName);

    List<TaskInfoEntity> getFinishTaskInfo(String userName);

    int getFinishTaskInfoTotal(String userName);
}




