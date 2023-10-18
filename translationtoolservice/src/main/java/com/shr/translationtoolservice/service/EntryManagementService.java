package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public interface EntryManagementService {
    //查询词条信息
    ResponseListModel searchEntry(@RequestBody EntryCommonEntity entryEntity,
                                  String entryState,
                                  @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                  @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
    //批量审核
    String bathAudit(List<EntryGroupEntity> entryGroupEntities,int state, HttpServletRequest request,String note);


    HttpResponse<EntryCommonEntity> insertEntry(EntryCommonEntity entryEntity, HttpServletRequest request,String insertType);

    ResultObject updateEntry(EntryCommonEntity entryEntity,HttpServletRequest request,String notes);

    String deleteEntry(List<String> idList );

    TranslateEntities translate(String name,String type);

    List<EntryOperate> queryOperate(EntryOperate entryId);

    List<EntryCommonEntity> selectNoMergeEntry(String entry);

    List<EntryCommonEntity> selectMergeEntry(String entry);

    String entryMerge(List<EntryEntity> entryEntity);

    List<EntryClassify> getEntryClassfy(String department);

    List<Thesaurus> getThesaurus();

    String addEntryClassfy(EntryClassify entryClassify,HttpServletRequest request);

    String updateEntryClassfy(EntryClassify entryClassify);

    String deleteEntryClassfy(List<String> idList);

    ResponseListModel<EntryLabel>  queryLabel(EntryLabel entryLabel,int pageIndex,int pageSize);

    String deleteLabel(List<String> idList);

    String addLabel(EntryLabel entryLabel);

    String updateLabel(EntryLabel entryLabel);

    List<EntryProperty> queryEntryProperty(EntryProperty entryProperty);

    String mergerSplit(List<String> idList);

    List<EntryCommonEntity>  importExcle(MultipartFile multipartFile);

    String createVersionTable(List<EntryCommonEntity> entryEntities, String version,String remark, String department,HttpServletRequest request);


    List<VersionTable>  getVersionTable(String tableName ,String version, Integer pageIndex, Integer pageSize,String department);

    String bachAddEntry(List<EntryCommonEntity> entryEntities);

    EntryResponse   getEntryToVersion(String version, List<String> classfy, String tag, String creator);

    List<EntryCommonEntity> getTranslatedEntry( Integer pageIndex, Integer pageSize);

    HttpResponse<EntryCommonEntity>  upgradeEntry(EntryCommonEntity entryEntity,HttpServletRequest request,String insertType);

    List<String> getKindEntryVersion(String typeID);
}
