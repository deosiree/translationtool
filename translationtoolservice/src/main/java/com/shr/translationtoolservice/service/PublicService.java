package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.PublicEntryEntity;
import com.shr.translationtoolservice.entity.TranslateEntities;

import java.util.List;

/**
 * @title PublicService
 * @create 2024/4/8 14:45
 * @description <TODO description class purpose>
 **/
public interface PublicService {
    List<PublicEntryEntity> queryTranslate(EntryInfoEntity entity, String targetLang);

    List<PublicEntryEntity> realTimeTranslate(List<String> entityList, String targetLang);
}
