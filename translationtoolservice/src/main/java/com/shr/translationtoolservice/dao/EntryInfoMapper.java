package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryInfoEntity
 */
@Mapper
public interface EntryInfoMapper extends BaseMapper<EntryInfoEntity> {

    List<EntryInfoEntity> getEntryByVersion(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    int getEntryByVersionTotal(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    List<EntryInfoEntity> getEntryByAbbr(@Param("abbr") String abbr, @Param("versionID") String versionID, @Param("tableName") String tableName);

    int insertEntry(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity, @Param("tableName") String tableName);

    int updateEntryInfo(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    EntryInfoEntity selectEntryById(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    int deleteByIdList(@Param("idList") List<String> idList, @Param("tableName") String tableName);

    List<EntryInfoEntity> getEntryByTaskID( @Param("id")String id,@Param("tableName") String tableName);

    List<EntryInfoEntity> getEnrtyByTaskID(String taskID);

    List<EntryInfoEntity> getEntryByVersionID(@Param("tableName") String tableName,@Param("versionID") String versionID);
}




