package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
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



    List<String>  getVersionIDByTaskID(@Param("taskID")  String taskID);

    List<TaskInfoEntity> getTaskInfoByUserName(@Param("userName") String userName, @Param("offset") Integer offset, @Param("limit") Integer pageSize, @Param("taskInfoEntity") TaskInfoEntity taskInfoEntity);

    int getToDoTaskInfoTotal(@Param("userName") String userName,@Param("taskInfoEntity") TaskInfoEntity taskInfoEntity);

    List<TaskInfoEntity> getFinishTaskInfo(@Param("userName") String userName, @Param("offset") Integer offset, @Param("limit") Integer pageSize,@Param("taskInfoEntity") TaskInfoEntity taskInfoEntity);

    int getFinishTaskInfoTotal(@Param("userName") String userName,@Param("taskInfoEntity") TaskInfoEntity taskInfoEntity);

    TaskInfoEntity getTaskEntityByTaskID(String taskID);
}




