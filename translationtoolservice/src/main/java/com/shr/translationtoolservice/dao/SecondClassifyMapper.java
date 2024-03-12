package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.SecondClassify;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
*@title SecondClassifyMapper
*@create 2024/3/8 14:08
*@description <TODO description class purpose>
**/
@Mapper
public interface SecondClassifyMapper {
    /**
     * delete by primary key
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(@Param("ids") List<String> ids);

    /**
     * insert record to table
     * @param secondClassify the record
     * @return insert count
     */
    int insert(@Param("secondClassify") SecondClassify secondClassify);

    /**
     * insert record to table selective
     * @param record the record
     * @return insert count
     */
    int insertSelective(SecondClassify record);

    /**
     * select by primary key
     * @param id primary key
     * @return object by primary key
     */
    SecondClassify selectByPrimaryKey(String id);

    /**
     * update record selective
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(@Param("secondClassify") SecondClassify record);

    /**
     * update record
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKey(SecondClassify record);

    List<SecondClassify> selectSecondClassify(@Param("secondClassify") SecondClassify secondClassify);
}