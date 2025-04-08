package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ProductRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.ProductRelationEntity
 */
@Mapper
public interface ProductRelationMapper extends BaseMapper<ProductRelationEntity> {

    void deleteByEntryID(List<String> entryID);

    int deleteByProductIdList(@Param("idList") List<String>  idList);

    int insertList(@Param("productRelationEntities") List<ProductRelationEntity> productRelationEntities);

    int checkExist(@Param("productRelationEntity") ProductRelationEntity productRelationEntity);

    /**
     * @param productRelationEntity 该对象中必须保证有一个属性不是null或''
     * @return
     */
    List<ProductRelationEntity> getProductionRelation(@Param("productRelationEntity") ProductRelationEntity productRelationEntity);

    void deleteByVersionID(List<String> idList);
}




