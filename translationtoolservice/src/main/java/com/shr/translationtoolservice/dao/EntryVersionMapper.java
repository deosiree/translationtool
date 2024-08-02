package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ConfigResUser;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryVersion;
import com.shr.translationtoolservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EntryVersionMapper {

    List<EntryVersion> queryVersionInfo(@Param("limit") int limit,
                                        @Param("offset") int offset);

    List<EntryVersion> queryVersionInfoByName(String versionName);

    int getVersionTotalNum(String versionName);

    Integer updateVersionInfo(@Param("entryVersion") EntryVersion entryVersion);

    Integer deleteVersionInfo(List<String> idList);

    Integer addVersionInfo(@Param("entryVersion") EntryVersion entryVersion);

    List<EntryVersion> getVersionByDefault(int isDeault);

    int updateDefault0();

    EntryVersion getNewVersion();

}