package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryPublicEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.shr.translationtoolservice.entity.Translate;
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

    int updateTranslation(@Param("translateEntityList") List<TranslateEntity> translateEntityList);

    List<TranslateEntity> selectRepByEntryTemp(@Param("entryTempEntity") EntryTempEntity entryTempEntity);

    List<TranslateEntity> getSuggestTrans(@Param("name") String name,@Param("translateType")  String type,@Param("visualRange")  String visualRange);

    List<TranslateEntity> getVersionSuggestTrans(@Param("entry") String entry,@Param("translateType") String translateType);
}




