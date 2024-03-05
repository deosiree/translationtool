package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.LanguageEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.TLanguage
 */
@Mapper
public interface TLanguageMapper extends BaseMapper<TLanguage> {

    List<TLanguage> getLanguages(@Param("language") TLanguage language);

    List<TLanguage>  selectLaguageByName(@Param("language")String language);

    TLanguage getLanguageByTask(String taskID);
}




