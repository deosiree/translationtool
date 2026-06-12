package com.shr.translationtoolservice.service.processor.converter;


import org.springframework.stereotype.Component;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.service.parser.AbbrAppXMLParser.AbbrAppInfo;

@Component
public class AbbrAppInfoConverter implements EntryInfoEntityConverter<AbbrAppInfo>{

    @Override
    public EntryInfoEntity apply(AbbrAppInfo abbrAppInfo) {
        
        EntryInfoEntity entity = new EntryInfoEntity();
        entity.setEntry(abbrAppInfo.getAbbr());
        entity.setEntrySource(abbrAppInfo.getSourceTypes());
        entity.setChineseInterpretation(abbrAppInfo.getComments());

        return entity;

    }
    
}
