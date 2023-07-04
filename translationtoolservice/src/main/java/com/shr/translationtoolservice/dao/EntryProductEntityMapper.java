package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shr.translationtoolservice.entity.EntryProductEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.EntryProjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com/shr/translationtoolservice.entity.EntryProductEntity
 */
@Mapper
public interface EntryProductEntityMapper extends BaseMapper<EntryProductEntity> {
    @Override
    List<EntryProductEntity> selectList(@Param("ew") Wrapper<EntryProductEntity> queryWrapper);

    int auditByIds(@Param("idList") List<String> idList,@Param("state") String state);

}




