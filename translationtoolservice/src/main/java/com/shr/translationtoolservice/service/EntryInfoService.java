package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.EntryTempCompareVO;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.entity.vo.UpgradeVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 *
 */
public interface EntryInfoService extends IService<EntryInfoEntity> {

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

    List<EntryTempEntity>  importExcle(MultipartFile multipartFile,String taskID);

    String addEntryByTemp(List<EntryTempEntity> entryTempEntities, HttpServletRequest request, String tableName);

    TranslateEntities translate(String name, String type,String visualRange);

    String updateEntryTemp(List<EntryTempEntity> entryTempEntities, HttpServletRequest request);

    void addTransID(TranslateEntity translateEntities, EntryInfoEntity entryInfoEntity);

    void versionExport(String versionID, HttpServletResponse response,String translateType);
}
