package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.EntryPublicEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.entity.vo.UpgradeVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
public interface EntryInfoService extends IService<EntryInfoEntity> {

    List<EntryVO> getEntryByVersion(EntryInfoEntity entryInfoEntity,Integer offset,Integer pageSize);

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
}
