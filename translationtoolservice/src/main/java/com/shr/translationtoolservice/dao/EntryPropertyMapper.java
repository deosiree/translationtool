package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryProperty;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryProperty
 */
@Mapper
public interface EntryPropertyMapper extends BaseMapper<EntryProperty> {

    List<EntryProperty> getPropertyByName(@Param("propertyName") String propertyName,@Param("limit")  Integer pageSize,@Param("offset")  int offset);

    int getPropertyByNameTotal(@Param("propertyName")  String propertyName);

    int deleteProperty(@Param("ids") List<String> id);

    int updateProperty(@Param("entryProperty")  EntryProperty entryProperty);

    int insertProperty(@Param("entryProperty")  EntryProperty entryProperty);

    List<EntryProperty> selectProperty(@Param("propertyName") String propertyName);
}




