package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.UserProductEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.UserProductEntity
 */
@Mapper
public interface UserProductMapper extends BaseMapper<UserProductEntity> {

    UserProductEntity getPermissionByNameAndDepartment(@Param("name") String name,@Param("department") String department,@Param("productId")String productId);

    List<String> getUserReadProductId(@Param("userName") String userName, @Param("department") String department);
}




