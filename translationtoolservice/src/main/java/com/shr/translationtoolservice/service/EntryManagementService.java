package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryParentEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.EntryReqEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

@Service
public interface EntryManagementService {
    //查询词条信息
    ResponseListModel searchEntry(@RequestBody EntryReqEntity entryReqEntity,
                                  @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                  @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
    //批量审核
    String bathAudit(List<String> id,String type);

    List getAllEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize);

    String insertEntry(EntryParentEntity entryReqEntry, String type);

    String updateEntry(EntryParentEntity entryParentEntity, String type);

    String deleteEntry(EntryParentEntity entryParentEntity, String type);
}
