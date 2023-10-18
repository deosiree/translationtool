package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.entity.VersionTable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.VersionTable
 */
@Mapper
public interface VersionTableMapper extends BaseMapper<VersionTable> {

    List<VersionTable> getByTableName(String tableName);

    int addVersionTable(@Param("versionTable") VersionTable versionTable);

    int createVersionTable(String tableName);

    int insertVersionTable(@Param("tableName") String tableName, @Param("versionEntity") EntryCommonEntity entryEntity,
                           @Param("versionTable") String version);

    int existTable(@Param("tableName") String tableName);

    List<VersionTable> getVersionTable(@Param("tableName") String tableName,@Param("version") String version,
                                       @Param("limit") Integer pageSize,@Param("offset") int offset,@Param("department")String department);

    int getVersionTableTotal(@Param("tableName") String tableName,@Param("version") String version);

    List<VersionTable> getVersionInfoByVersion(String version);

    List<VersionEntity> getAllVersionTable(@Param("tableName")  String versionTableName, @Param("version") String version);

    //通过版本和名字查询版本库重复的词条
    List<VersionEntity> getReVersionTableByName(@Param("entryEntities")  List<EntryCommonEntity> entryEntities, @Param("tableName") String versionTableName,@Param("version")  String version);

    List<VersionTable> getVersionTableByCondition(@Param("version") String version, @Param("offset") Integer pageIndex, @Param("limit") Integer pageSize);

    Integer getTotalByCondition(@Param("version") String version);

    List<VersionTable> getVersionTableByIds(@Param("ids") List<String> ids);

    int deleteEntryByVersion(@Param("tableName") String tableName,@Param("version") String version);
}





