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

    List<EntryInfoEntity> getEntryByVersion(@Param("versionID") String versionID,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    int getEntryByVersionTotal(String versionID);

    List<EntryInfoEntity> getEntryByAbbr(@Param("abbr") String abbr,@Param("versionID") String versionID,@Param("tableName")   String tableName);

    int insertEntry(@Param("entryInfoEntity")  EntryInfoEntity entryInfoEntity,@Param("tableName")   String tableName);
}




