package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryPublicEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.TranslateEntity
 */
@Mapper
public interface TranslateMapper extends BaseMapper<TranslateEntity> {

    List<TranslateEntity> getTrans(@Param("translateEntity") TranslateEntity translateEntity,@Param("offset") int offset,@Param("limit")  Integer pageSize);


    int getPublicEntryTotal(@Param("translateEntity")TranslateEntity translateEntity);

    int deleteByIds(@Param("idList") List<String> idList);

    TranslateEntity selectPublicByEntry(TranslateEntity translateEntity);

    List<TranslateEntity> getPublicEntry(@Param("translateEntity") TranslateEntity translateEntity,
                                         @Param("offset") int offset,@Param("limit")  Integer pageSize);
}




