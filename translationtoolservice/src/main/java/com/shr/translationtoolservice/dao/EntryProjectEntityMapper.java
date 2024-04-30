package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shr.translationtoolservice.entity.EntryProjectEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryProjectEntity
 */
@Mapper
public interface EntryProjectEntityMapper extends BaseMapper<EntryProjectEntity> {
    @Override
    List<EntryProjectEntity> selectList(@Param("ew") Wrapper<EntryProjectEntity> queryWrapper);


    int auditByIds(@Param("idList") List<String> idList,@Param("state") String state);

    int auditById(String id,@Param("state") String state);
}




