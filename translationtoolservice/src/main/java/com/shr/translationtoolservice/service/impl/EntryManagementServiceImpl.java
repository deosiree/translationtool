package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.dao.EntryProjectEntityMapper;
import com.shr.translationtoolservice.entity.EntryProjectEntity;
import com.shr.translationtoolservice.entity.EntryReqEntry;
import com.shr.translationtoolservice.service.EntryCommonEntityService;
import com.shr.translationtoolservice.service.EntryProductEntityService;
import com.shr.translationtoolservice.service.EntryProjectEntityService;
import com.shr.translationtoolservice.service.EntryManagementService;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName TermManagementServiceImpl
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/19 0019 16:58
 **/
@Service
@Slf4j
public class EntryManagementServiceImpl implements EntryManagementService {
    @Autowired
    EntryProjectEntityMapper entryProjectEntityMapper;
    @Autowired
    EntryProductEntityService entryProductEntityService;
    @Autowired
    EntryCommonEntityService entryCommonEntityService;

    @Autowired
    EntryProjectEntityService entryProjectEntityService;

    @Override
    public List searchEntry(EntryReqEntry entryReqEntry, Integer pageIndex, Integer pageSize) {
 /*       //如果没有库则全量查
        if (StringUtils.isBlank(entryReqEntry.getLexicon())) {
            return getAllEntry(entryReqEntry, pageIndex, pageSize);
            //产品表
        } else if ("project".equals(entryReqEntry.getLexicon())) {
            return entryProjectEntityService.searchEntry(entryReqEntry, pageIndex, pageSize);
            //工程表
        } else if ("product".equals(entryReqEntry.getLexicon())) {
            return entryProductEntityService.searchEntry(entryReqEntry, pageIndex, pageSize);
            //公共表
        } else if ("common".equals(entryReqEntry.getLexicon())) {
            return entryCommonEntityService.searchEntry(entryReqEntry, pageIndex, pageSize);
        }
*/
        return null;
    }

    //先查project表，不够再查 product ，最后再查comm
    @Override
    public List getAllEntry(EntryReqEntry entryReqEntry, Integer pageIndex, Integer pageSize) {
        List entry = new ArrayList();

        List projectEntities = entryProjectEntityService.searchEntry(entryReqEntry, pageIndex, pageSize);
        if (CollectionUtils.isEmpty(projectEntities)) {
            return entry;
        }
        entry.addAll(projectEntities);


        //先查project 查出页码和参数页码相同 即返回
        if (checkPage(projectEntities, pageIndex, pageSize)) {
            return entry;
        }

        //第一页没满的时候

        //剩余页码

        int pageLastIndex = pageIndex - projectEntities.size() / pageSize;


        List productEntities = entryProductEntityService.searchEntry(entryReqEntry, pageLastIndex, pageSize);
        if (CollectionUtils.isEmpty(productEntities)) {
            return entry;
        }
        entry.addAll(productEntities);

        if (checkPage(productEntities, pageIndex, pageSize)) {
            return entry;
        }


        //剩余页码
        int pageLastIndex1 = pageLastIndex -  productEntities.size() / pageSize;
        List commonEntities = entryCommonEntityService.searchEntry(entryReqEntry, pageLastIndex1, pageSize);
        if (CollectionUtils.isEmpty(commonEntities)) {
            return entry;
        }
        entry.addAll(commonEntities);
        return entry;
    }

    //
    private boolean checkPage(List list, Integer pageIndex, Integer pageSize) {
        if (list.size() < pageSize) {
            return false;
        }
        int pageNowIndex = getPageIndex(list.size(), pageSize);
        //先查project 查出页码和参数页码相同 即返回
        if (pageNowIndex >= pageIndex) {
            return true;
        }
        return false;
    }

    private int getPageIndex(int size, Integer pageSize) {
        return size / pageSize + size % pageSize == 0 ? 0 : 1;
    }


    @Override
    //TODO
    public String bathAudit(List<String> id) {

        return null;
    }
}
