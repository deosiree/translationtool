package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryClassify;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryClassify
 */
@Mapper
public interface EntryClassifyMapper extends BaseMapper<EntryClassify> {

    List<EntryClassify>  getEntryClassfyByIds();

    EntryClassify selectById(String parentId);

    int insert(@Param("entryClassify") EntryClassify entryClassify);

    int deleteByIds(List<String> entryClassifies);

    int updateById(@Param("entryClassify") EntryClassify entryClassify);
}




