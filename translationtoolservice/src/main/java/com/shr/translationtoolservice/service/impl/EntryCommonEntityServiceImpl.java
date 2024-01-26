package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryReqEntity;
import com.shr.translationtoolservice.service.EntryCommonEntityService;
import com.shr.translationtoolservice.dao.EntryCommonEntityMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 *
 */
@Service
@Slf4j
public class EntryCommonEntityServiceImpl extends ServiceImpl<EntryCommonEntityMapper, EntryCommonEntity>
    implements EntryCommonEntityService{

    @Autowired
    CommonUtils commonUtils;
    @Autowired
    EntryCommonEntityMapper entryCommonEntityMapper;
    @Override
    public List<EntryCommonEntity> searchEntry(EntryReqEntity reqEntry, Integer pageIndex, Integer pageSize) {
        QueryWrapper<EntryCommonEntity> queryWrapper = new QueryWrapper<EntryCommonEntity>();
        queryWrapper.eq(StringUtils.isNotBlank(reqEntry.getEntry()),"entry", reqEntry.getEntry());
        queryWrapper.eq(StringUtils.isNotBlank(reqEntry.getCreator()),"creator", reqEntry.getCreator());
        queryWrapper.ge(Objects.nonNull(reqEntry.getCreateStartDate()),"create_time", reqEntry.getCreateStartDate());
        queryWrapper.le(Objects.nonNull(reqEntry.getCreateEndDate()),"create_time", reqEntry.getCreateEndDate());
        queryWrapper.eq(StringUtils.isNotBlank(reqEntry.getUpdate()),"update", reqEntry.getUpdate());
        queryWrapper.ge(Objects.nonNull(reqEntry.getUpdateStartDate()),"update_time", reqEntry.getUpdateStartDate());
        queryWrapper.le(Objects.nonNull(reqEntry.getUpdateEndDate()),"update_time", reqEntry.getUpdateEndDate());
        queryWrapper.eq(StringUtils.isNotBlank(reqEntry.getAbbr()),"abbr", reqEntry.getAbbr());
        queryWrapper.eq(StringUtils.isNotBlank(reqEntry.getVersion()),"version", reqEntry.getVersion());
        queryWrapper.eq(StringUtils.isNotBlank(reqEntry.getEntryState()),"entry_state", reqEntry.getEntryState());
        //翻译内容模糊查询
        queryWrapper.like(StringUtils.isNotBlank(reqEntry.getTranslate()),"english", reqEntry.getTranslate());
        queryWrapper.like(StringUtils.isNotBlank(reqEntry.getTranslate()),"russian", reqEntry.getTranslate());
        queryWrapper.like(StringUtils.isNotBlank(reqEntry.getTranslate()),"spanish", reqEntry.getTranslate());
        queryWrapper.like(StringUtils.isNotBlank(reqEntry.getTranslate()),"french", reqEntry.getTranslate());

        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            queryWrapper.last(" LIMIT " + pageSize + " OFFSET " + offset);
        }
        // List entryProjectEntities1 = entryProjectEntityService.list(Wrappers.<EntryProjectEntity>lambdaQuery().eq(EntryProjectEntity::getAbbr,1));

        List<EntryCommonEntity> entryCommonEntities = entryCommonEntityMapper.selectList(queryWrapper);
        return entryCommonEntities;
    }
}




