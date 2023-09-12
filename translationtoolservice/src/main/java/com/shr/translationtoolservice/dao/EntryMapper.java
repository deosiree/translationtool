package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.EntryClassify;
import com.shr.translationtoolservice.entity.EntryEntity;
import com.shr.translationtoolservice.entity.EntryProjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryEntity
 */
@Mapper
public interface EntryMapper extends BaseMapper<EntryEntity> {
    @Override
    List<EntryEntity> selectList(@Param("ew") Wrapper<EntryEntity> queryWrapper);

    EntryEntity selectEntryById(String id);

    int insertEntry(@Param("entryEntity") EntryEntity entryEntity);

    int auditByIds(@Param("idList") List<String> idList,@Param("state") String state);

    int auditById(@Param("tableName") String tableName,@Param("id") String id,@Param("state") int state);

    List<EntryEntity> selectByAbbr(@Param("abbr") String abbr,@Param("version") String version);

    int updateEntryById(@Param("entryEntity") EntryEntity entryEntity);

    int deleteEntries(@Param("idList") List<String> idList);

    List<EntryEntity> selectByName(@Param("entryEntity") EntryEntity entryEntity);

    List<EntryEntity> selectNoMerge(String chinese);

    List<EntryEntity> selectMerge(String chinese);

    List<EntryEntity> selectListByEntry(@Param("entryEntity")  EntryEntity entryEntity,@Param("limit") Integer limit, @Param("offset") int offset, @Param("entryState")String entryState);

    List<EntryEntity> selectListByEntries(@Param("entryEntity") EntryEntity entryEntity,
                                          @Param("classifyIds") List<EntryClassify> classfyList,  @Param("limit") Integer limit,  @Param("offset") int offset,@Param("entryState")  String entryState);

    int selectListByEntriesTotal(@Param("entryEntity") EntryEntity entryEntity,@Param("entryState")  String entryState,@Param("classifyIds") List<EntryClassify> classfyList);


    int mergerSplit(List<String> idList);
}




