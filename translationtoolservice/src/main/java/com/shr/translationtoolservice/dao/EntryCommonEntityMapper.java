package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com/shr/translationtoolservice.entity.EntryCommonEntity
 */
@Mapper
public interface EntryCommonEntityMapper extends BaseMapper<EntryCommonEntity> {
    @Override
    List<EntryCommonEntity> selectList(@Param("ew") Wrapper<EntryCommonEntity> queryWrapper);

    int auditByIds(@Param("idList") List<String> idList,@Param("state") String state);

}




