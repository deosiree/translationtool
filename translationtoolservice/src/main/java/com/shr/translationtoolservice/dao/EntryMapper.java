package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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

    EntryEntity selectById(String id,String tableName);

    int insert(@Param("entryEntity") EntryEntity entryEntity);

    int auditByIds(@Param("idList") List<String> idList,@Param("state") String state);

    int auditById(String tableName,String id,@Param("state") int state);

    List<EntryEntity> selectByAbbr(@Param("entryEntity") EntryEntity entryEntity);

    int updateById(@Param("entryEntity") EntryEntity entryEntity);

    int deleteEntries(@Param("entryEntities") List<EntryEntity> entryEntities,@Param("tableName") String tableName);

    List<EntryEntity> selectByName(@Param("entryEntity") EntryEntity entryEntity);

    List<EntryEntity> selectRepeEntry(String repeatEntryId);


    List<EntryEntity> selectListByEntry(EntryEntity entryEntity, Integer limit, int offset,String entryState);

    List<EntryEntity> selectListByEntries(@Param("entryEntity") EntryEntity entryEntity,@Param("tableNames") List<String> tableNames, Integer limit, int offset,String entryState);

    List<EntryEntity>  selectListByEntriesTotal(@Param("entryEntity") EntryEntity entryEntity,@Param("tableNames") List<String> tableNames,String entryState);

    int selectListByEntryTotal(@Param("entryEntity") EntryEntity entryEntity,String entryState);
}




