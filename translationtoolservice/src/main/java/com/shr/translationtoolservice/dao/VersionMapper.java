package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.VersionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Entity com.shr.translationtoolservice.entity.VersionEntity
 */
@Mapper
public interface VersionMapper extends BaseMapper<VersionEntity> {

    List<VersionEntity> selectByName(@Param("version") String version);
    List<VersionEntity> selectByNameAndProductId(@Param("version") String version,@Param("productId") String productId);

    List<VersionEntity> getVersion(@Param("versionEntity") VersionEntity versionEntity);

    int getVersionTotal(@Param("versionEntity") VersionEntity versionEntity);

    int deleteByIds(@Param("idList") List<String> idList);

    List<VersionEntity> getVersionByProductName(@Param("productName") String productName, @Param("department") String department);


    VersionEntity getVersionByID(String versionID);


    @MapKey("id")
    Map<String,VersionEntity> getVersionByIDs(@Param("ids") Set<String> ids);
}




