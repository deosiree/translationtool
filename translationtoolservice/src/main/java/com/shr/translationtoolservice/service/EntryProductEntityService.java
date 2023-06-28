package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryProductEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.EntryProjectEntity;
import com.shr.translationtoolservice.entity.EntryReqEntry;

import java.util.List;

/**
 *
 */
public interface EntryProductEntityService extends IService<EntryProductEntity> {

    List<EntryProductEntity> searchEntry(EntryReqEntry entryReqEntry, Integer pageIndex, Integer pageSize);
}
