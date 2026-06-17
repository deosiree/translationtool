package com.shr.translationtoolservice.service.exporter;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.util.JSONUtils;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class DICExporter implements JSONExporter {
    

    static protected Map<String,String> translateGetMethodMap = ConstantInterface.entryInfoEntityGetterTranslateMap();

    static protected Map<String,String> translateLangCodeMap = new HashMap<>();
    

    /*
        导出的词条要包含所有语言的翻译（默认），其他情况根据指定的语言类型决定
    */

    public int export(Collection<EntryInfoEntity> entryInfoEntities, OutputStream outputStream){


        List<DictionaryVO> dictionaryVOs = entryInfoEntities.stream().map((entry)->{
            DictionaryVO dictionaryVO = new DictionaryVO();
            dictionaryVO.setSource(entry.getEntry());
            dictionaryVO.setTag(entry.getTag());
            dictionaryVO.setComments(entry.getComment());

            for(Map.Entry<String,String> translateLangCode : translateLangCodeMap.entrySet()){
                
            }
            return dictionaryVO;
            
        }).collect(Collectors.toList());

        if(!JSONUtils.exportJson(dictionaryVOs, "UTF-8", outputStream)){
            log.error("");
            return -1;
        }
        return 0;
    }
}
