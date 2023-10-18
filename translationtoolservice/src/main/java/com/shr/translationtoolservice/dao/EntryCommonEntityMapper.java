package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shr.translationtoolservice.entity.EntryClassify;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.EntryEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryCommonEntity
 */
@Mapper
public interface EntryCommonEntityMapper extends BaseMapper<EntryCommonEntity> {


    @Override
    List<EntryCommonEntity> selectList(@Param("ew") Wrapper<EntryCommonEntity> queryWrapper);

    int auditByIds(@Param("idList") List<String> idList,@Param("state") String state);

    int auditById(String id,@Param("state") int state);

    List<EntryCommonEntity> getRepAbbrAndVersionEntry(@Param("entryEntities") List<EntryCommonEntity> entryEntities);







    EntryCommonEntity selectEntryById(String id);

    int insertEntry(@Param("entryEntity") EntryCommonEntity entryEntity);



    List<EntryCommonEntity> selectByAbbr(@Param("abbr") String abbr,@Param("version") String version);

    int updateEntryById(@Param("entryEntity") EntryCommonEntity entryEntity);

    int deleteEntries(@Param("idList") List<String> idList);

    List<EntryCommonEntity> selectByName(@Param("entryEntity") EntryCommonEntity entryEntity);

    List<EntryCommonEntity> selectNoMerge(String chinese);

    List<EntryCommonEntity> selectMerge(String chinese);

    List<EntryCommonEntity> selectListByEntry(@Param("entryEntity")  EntryCommonEntity entryEntity,@Param("limit") Integer limit, @Param("offset") int offset, @Param("entryState")String entryState);

    List<EntryCommonEntity> selectListByEntries(@Param("entryEntity") EntryCommonEntity entryEntity,
                                          @Param("classifyIds") List<EntryClassify> classfyList, @Param("limit") Integer limit, @Param("offset") int offset, @Param("entryState")  String entryState);

    int selectListByEntriesTotal(@Param("entryEntity") EntryCommonEntity entryEntity,@Param("entryState")  String entryState,@Param("classifyIds") List<EntryClassify> classfyList);


    int mergerSplit(List<String> idList);

    List<EntryCommonEntity> getEntryToVersion(@Param("classfies") List<String> classfies,@Param("tag") String tag,
                                        @Param("creator")String creator,@Param("versionEntities")   List<VersionEntity> versionEntities);

    List<EntryCommonEntity> getTranslatedEntry(@Param("limit") Integer limit, @Param("offset") int offset);

    List<String> getKindEntryVersion(@Param("typeID") String typeID);
}




