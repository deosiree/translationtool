package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface I18nService {

    String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType,boolean tag,boolean comment,String i18nUrl);

    List<DictionaryVo> getDictory(String entry, String tag, String common,String fileName,String i18nUrl);
}
