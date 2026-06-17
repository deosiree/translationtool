package com.shr.translationtoolservice.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.ResponseEntity;

import io.lettuce.core.dynamic.annotation.Param;

@Mapper
public interface ResponseMapper extends BaseMapper<ResponseEntity>{
    

    List<ResponseEntity> getResponseByTaskId(@Param("responseEntity") ResponseEntity responseEntity);

    int insertBatchResponse(@Param("responseEntityList") List<ResponseEntity> responseEntityList);

    int insertResponse(@Param("responseEntity") ResponseEntity responseEntity);

    int updateResponse(@Param("responseEntity") ResponseEntity responseEntity);

}
