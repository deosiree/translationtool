package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.Index;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Entity com/shr/translationtoolservice.entity.Index
 */
@Mapper
public interface IndexMapper extends BaseMapper<Index> {
    Index getIndexByEntry(@Param("entry") String entry);


    int deleteByRepeatId(@Param("repeatEntryId") String repeatEntryId);
}




