package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public interface EntryManagementService {
    //查询词条信息
    ResponseListModel searchEntry(@RequestBody EntryEntity entryEntity,
                                  String entryState,
                                  @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                  @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
    //批量审核
    String bathAudit(List<EntryGroupEntity> entryGroupEntities,int state, HttpServletRequest request,String note);


    HttpResponse<EntryEntity> insertEntry(EntryEntity entryEntity, HttpServletRequest request);

    String updateEntry(EntryEntity entryEntity,HttpServletRequest request);

    String deleteEntry(List<String> idList,String tableName);

    TranslateEntity translate(EntryEntity entryEntity);

    List<EntryOperate> queryOperate(EntryOperate entryId);

    List<EntryEntity> selectRepeEntry(String mergeState);

    String entryMerge(List<EntryEntity> entryEntity);

    List<EntryClassify> getEntryClassfy();

    List<Thesaurus> getThesaurus();

    String addEntryClassfy(EntryClassify entryClassify);

    String updateEntryClassfy(EntryClassify entryClassify);

    String deleteEntryClassfy(List<String> idList);

    List<EntryLabel> queryLabel();
}
