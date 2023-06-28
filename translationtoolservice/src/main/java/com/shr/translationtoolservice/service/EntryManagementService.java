package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryProjectEntity;
import com.shr.translationtoolservice.entity.SearchCondition;
import com.shr.translationtoolservice.entity.EntryReqEntry;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
public interface EntryManagementService {
    //查询词条信息
    List searchEntry(@RequestBody EntryReqEntry entryReqEntry,
                                         @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                         @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
    //批量审核
    String bathAudit(List<String> id);

    List getAllEntry(EntryReqEntry entryReqEntry, Integer pageIndex, Integer pageSize);
}
