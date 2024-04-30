package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.ProductTableEntity
 */
@Mapper
public interface ProductTableMapper extends BaseMapper<ProductTableEntity> {

    int addProductTable(@Param("productTable") ProductTableEntity productTableEntity);

    int createProductInfoTable(String tableName);

    int createProductRelationTable(String tableName);
    ProductTableEntity getTableInfoByProductId(String productID);

    List<EntryInfoEntity> getEntryInfoByAbbr( @Param("productTableName") String productTableName,@Param("entryTempEntity") EntryTempEntity entryTempEntity);

    ProductTableEntity getProductByTaskID(String taskID);
}




