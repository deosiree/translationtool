package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.UserPartiality;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
*@title UserPartialityMapper
*@create 2024/4/11 9:27
*@description <TODO description class purpose>
**/
@Mapper
public interface UserPartialityMapper  extends BaseMapper<UserPartiality> {
    /**
     * delete by primary key
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(String id);

    /**
     * insert record to table
     * @param record the record
     * @return insert count
     */
    int insert(@Param("record") UserPartiality record);

    /**
     * insert record to table selective
     * @param record the record
     * @return insert count
     */
    int insertSelective(UserPartiality record);

    /**
     * select by primary key
     * @param id primary key
     * @return object by primary key
     */
    UserPartiality selectByPrimaryKey(String id);

    /**
     * update record selective
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(@Param("record") UserPartiality record);

    /**
     * update record
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKey(UserPartiality record);

    List<UserPartiality> select(@Param("userPartiality") UserPartiality userPartiality);
}