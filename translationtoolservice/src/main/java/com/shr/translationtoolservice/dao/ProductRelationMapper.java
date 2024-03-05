package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ProductRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.ProductRelationEntity
 */
@Mapper
public interface ProductRelationMapper extends BaseMapper<ProductRelationEntity> {

    void deleteByEntryID(List<String> entryID);
}




