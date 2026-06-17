package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface EntryInfoService extends IService<EntryInfoEntity>,EntryFileImportService,EntryFileExportService,EntryFileService {

    List<EntryInfoEntity> getEntryByVersion(EntryInfoEntity entryInfoEntity,Integer offset,Integer pageSize);

    int getEntryByVersionTotal(EntryInfoEntity entryInfoEntity);

    HttpResponse<String> addEntryByVersion(List<EntryVO> entryVOS, HttpServletRequest request);

    String addEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request,String tableName);

    String updateEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request, String notes);

    String deleteEntryInfo(List<String> idList,String tableName);

    List<TranslateEntity> getPublicEntry(TranslateEntity translateEntity, int offset, Integer pageSize);

    int getPublicEntryTotal(TranslateEntity translateEntity);

    String updatePublicEntry(TranslateEntity translateEntity);

    String addPublicEntry(List<TranslateEntity> translateEntity);

    String deletePublicEntry(List<String> idlist);

    String upgrade(UpgradeVO upgradeVO, HttpServletRequest request);

    String updateEntryInfoList(List<EntryInfoEntity> entryInfoEntities, HttpServletRequest request, String notes);


    boolean checkBeforeAddSingleEntry(EntryInfoEntity entryInfoEntity,String department);

    EntryInfoEntity addSingleEntry(EntryInfoEntity entryInfoEntity, HttpServletRequest request);

    TranslateEntities translate(String name, String type,String visualRange);

    String updateEntryTemp(List<EntryTempEntity> entryTempEntities, HttpServletRequest request);

    void addTransID(TranslateEntity translateEntities, EntryInfoEntity entryInfoEntity);

    void versionExport(String versionID, HttpServletResponse response,String translateType);


    List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities,String taskID,HttpServletRequest httpServletRequest);

    List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities,String taskID,String userName,String department);


    String addEntryAudit(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request);

    String createVersionByEntry(List<EntryInfoEntity> entryInfoEntities, String productID, String common, String versionName, HttpServletRequest request);


    List<EntryClassify> getClassfy(String parentId, String type);

    String addProductRelation(List<EntryInfoEntity>  relationEntity,HttpServletRequest request) throws Exception;


    List<EntryInfoEntity>  filterSourceLanguage(List<EntryInfoEntity> entryInfoEntities, String languageType);



    String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType, String writeType, boolean tag, boolean comment, String fileName,String i18nUrl);


    List<EntryInfoEntity> capitalizeWords(List<EntryInfoEntity> entryInfoEntities, String changeType,String translateType);

    List<EntryInfoEntity> replaceWords(List<EntryInfoEntity> entryInfoEntities, String sourceStr, String replaceStr,String translateType);

    // int getEntryByClassfTotal(EntryInfoEntity entryInfoEntity, String classfyID,String translateType);

    List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID, Map<String,List<String>> soucreMap)
     throws Exception;

    List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,List<EntryInfoEntity> entities,Map<String,List<String>> sourceMap);


    List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID,  Map<String,List<String>> soucreMap,Map<String,String> condition) throws Exception;


    List<SourceEntryVO> getEntrysourceListByClassfy( String classfyID,String i18nUrl,String token,List<String> exceptionMessage);

    void doWeight();

    List<String> updateEntryByEntrySource(List<SourceEntryVO> sourceEntryVOS,HttpServletRequest request);

    EntryInfoVO getEntryByClassfy(EntryInfoEntityQO entryInfoEntityTemplate,Set<String> clearMatchSet,String classfyID,String startTime,String endTime,Integer pageIndex,Integer pageSize);
    
    EntryInfoVO getEntryByClassfyOnPage(String classfyID, Integer pageIndex, Integer pageSize);

    List<EntryInfoEntity> getEntryInfoByTsVo(List<TsVo> tsVoList);
    /* 禁用词条 */
    boolean forrbiddenEntry(List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request);

    Set<String> getEntrySourcesByClassify(String classifyID,String writeType);

    Set<String> getWriteFileNamesByClassify(String classifyID,String writeType);


    EntryInfoGroupVO makeGroupForEntryInfos(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> replicatedTargetAttributes);

    void getLog(ByteArrayOutputStream outputStream, String logPath);
}
