package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.ProductEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
}




