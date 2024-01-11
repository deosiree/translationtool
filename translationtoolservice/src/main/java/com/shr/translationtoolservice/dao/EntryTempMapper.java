package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryTempEntity
 */
@Mapper
public interface EntryTempMapper extends BaseMapper<EntryTempEntity> {

    List<EntryTempEntity> getEntryTempByTaskID(@Param("taskID") String taskID);

    int getEntryTempByTaskIDTotal(@Param("taskID") String taskID);


    int deleteByTaskID(String taskID);
}




