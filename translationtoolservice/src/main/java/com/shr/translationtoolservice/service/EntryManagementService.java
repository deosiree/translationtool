package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public interface EntryManagementService {
    //查询词条信息
    ResponseListModel searchEntry(@RequestBody EntryReqEntity entryReqEntity,
                                  @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                  @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
    //批量审核
    String bathAudit(List<EntryGroupEntity> entryGroupEntities,int state, HttpServletRequest request);

    List getAllEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize);

    String insertEntry(EntryEntity entryEntity,HttpServletRequest request);

    String updateEntry(EntryEntity entryEntity,HttpServletRequest request);

    String deleteEntry(EntryGroupEntity entryGroupEntity);
}
