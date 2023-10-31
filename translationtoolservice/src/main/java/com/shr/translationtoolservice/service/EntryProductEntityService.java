package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryProductEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.EntryReqEntity;

import java.util.List;

/**
 *
 */
public interface EntryProductEntityService extends IService<EntryProductEntity> {

    List<EntryProductEntity> searchEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize);
}
