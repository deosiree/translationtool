package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryClassify;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryClassify
 */
@Mapper
public interface EntryClassifyMapper extends BaseMapper<EntryClassify> {

    List<EntryClassify>  getEntryClassfyIds();

    List<EntryClassify>  getEntryClassfyByIds( @Param("classfies") List<String> classfies);

    List<EntryClassify>  getEntryClassfyByNames( @Param("classfies") List<String> classfies);

    List<EntryClassify>  getEntryClassfyByParentId(@Param("ids") List<String> ids);

    EntryClassify selectClassfyById(String id);

    int insertClassfy(@Param("entryClassify") EntryClassify entryClassify);

    int deleteByIds(List<String> entryClassifies);

    int updateClassfyById(@Param("entryClassify") EntryClassify entryClassify);

    List<EntryClassify> getEntryClassfyIdsByDepartment(String department);

    EntryClassify getEntryClassfyById(String classifyId);

    List<EntryClassify> getParentClassify(@Param("department") String department, @Param("className") String className);

    List<EntryClassify> getChildClassify(@Param("department") String department, @Param("className") String className);

    List<EntryClassify> getClassfy(@Param("entryClassify") EntryClassify entryClassify);

    int deleteList(List<String> idList);
}




