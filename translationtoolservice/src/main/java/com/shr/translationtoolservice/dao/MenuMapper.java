package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {
    int deleteByPrimaryKey(String id);

    int insert(Menu record);

    int insertSelective(Menu record);

    Menu selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Menu record);

    int updateByPrimaryKey(Menu record);

    List<Menu> selectByRoleId(@Param("roleIds") List<String> roleId);

    List<Menu> selectAllInfo();

    int selectMenyTotal();
}