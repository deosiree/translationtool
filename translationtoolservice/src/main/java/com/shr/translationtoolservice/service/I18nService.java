package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public interface I18nService {

    String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType,boolean tag,boolean comment,String i18nUrl);

    List<DictionaryVo> getDictory(String entry, String tag, String common,String fileName,String i18nUrl);

    List<EntryInfoEntity> getDBEntryBySource(String i18nUrl, Set<String> dbfileSet, String taskID, String userName, String productId, String versionID, String translateType);

    List<EntryInfoEntity> getENUMEntryBySource(String i18nUrl, Set<String> enumfileSet, String taskID, String userName, String productId, String versionID, String translateType) throws Exception;

    List<EntryInfoEntity> getCONFIGEntryBySource(String i18nUrl,Set<String> configfileSet, String taskID, String userName, String productId, String versionID, String translateType) throws Exception;
}
