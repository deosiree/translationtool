package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryLabel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryLabel
 */
@Mapper
public interface EntryLabelMapper extends BaseMapper<EntryLabel> {

    int deleteLabel(@Param("idList") List<String> idList);

    List<EntryLabel> getLabels(@Param("entryLabel") EntryLabel entryLabel,@Param("limit") int limit,@Param("offset") int offset);

    int getLabelsTotal(@Param("entryLabel") EntryLabel entryLabel,@Param("limit")  int limit,@Param("offset") int offset);

}




