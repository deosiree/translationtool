package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.EntryPublicEntity;
import com.shr.translationtoolservice.service.EntryPublicService;
import com.shr.translationtoolservice.dao.EntryPublicMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
public class EntryPublicServiceImpl extends ServiceImpl<EntryPublicMapper, EntryPublicEntity>
    implements EntryPublicService{

    @Override
    public List<EntryPublicEntity> getPublicEntryByDepartment(EntryPublicEntity entryPublicEntity, int offset, Integer pageSize) {


        return null;
    }




}




