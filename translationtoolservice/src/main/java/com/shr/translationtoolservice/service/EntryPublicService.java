package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryPublicEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 *
 */
public interface EntryPublicService extends IService<EntryPublicEntity> {

    List<EntryPublicEntity> getPublicEntryByDepartment(EntryPublicEntity entryPublicEntity, int offset, Integer pageSize);
}
