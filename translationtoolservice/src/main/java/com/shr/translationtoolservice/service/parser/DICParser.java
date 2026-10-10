package com.shr.translationtoolservice.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.common.reflect.TypeToken;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.util.JSONUtils;

@Component
public class DICParser implements Parser{


    public Set<DictionaryVO> parse(InputStream inputStream, String encoding){
        
        List<DictionaryVO> dictionaryVOs = JSONUtils.parseToJson(inputStream, encoding, new TypeToken<List<DictionaryVO>>() {}.getType());
        return dictionaryVOs.stream().collect(Collectors.toSet());
    }
    
}
