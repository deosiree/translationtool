package com.shr.translationtoolservice.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shr.translationtoolservice.entity.EntryEntity;
import com.shr.translationtoolservice.entity.EntryOperate;
import com.shr.translationtoolservice.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EntryOperateMapper extends BaseMapper<EntryOperate> {

    EntryOperate selectByEntryId(String entryId);
}