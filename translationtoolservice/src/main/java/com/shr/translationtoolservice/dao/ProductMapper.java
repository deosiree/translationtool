package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ProductEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Entity com.shr.translationtoolservice.entity.ProductEntity
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {

    int deleteList(@Param("idList") List<String> idList);

    List<ProductEntity> selectByName(@Param("name") String name);

    List<ProductEntity> getProductList(@Param("productEntity") ProductEntity productEntity);

    int getProductListTotal(@Param("productEntity") ProductEntity productEntity);

    int deleteByIds(@Param("idList") List<String> idList);

    @MapKey("id")
    Map<String,ProductEntity> getProductByIDs(@Param("ids") Set<String> ids);
}




