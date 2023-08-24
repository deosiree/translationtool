package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryLabel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryLabel
 */
@Mapper
public interface EntryLabelMapper extends BaseMapper<EntryLabel> {

    int deleteLabel(List<String> idList);
}




