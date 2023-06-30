package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.EntryReqEntity;

import java.util.List;

/**
 *
 */
public interface EntryCommonEntityService extends IService<EntryCommonEntity> {

    List<EntryCommonEntity> searchEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize);
}
