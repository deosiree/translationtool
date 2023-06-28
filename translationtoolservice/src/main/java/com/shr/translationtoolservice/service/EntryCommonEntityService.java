package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.EntryProjectEntity;
import com.shr.translationtoolservice.entity.EntryReqEntry;

import java.util.List;

/**
 *
 */
public interface EntryCommonEntityService extends IService<EntryCommonEntity> {

    List<EntryCommonEntity> searchEntry(EntryReqEntry entryReqEntry, Integer pageIndex, Integer pageSize);
}
