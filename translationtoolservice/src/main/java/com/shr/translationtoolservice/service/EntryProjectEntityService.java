package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryProjectEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.EntryReqEntity;

import java.util.List;

/**
 *
 */
public interface EntryProjectEntityService extends IService<EntryProjectEntity> {
    List<EntryProjectEntity> searchEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize);
}
