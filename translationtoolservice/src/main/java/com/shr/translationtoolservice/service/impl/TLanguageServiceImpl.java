package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.TLanguageService;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
@Slf4j
public class TLanguageServiceImpl extends ServiceImpl<TLanguageMapper, TLanguage>
    implements TLanguageService{

    @Autowired
    private TLanguageMapper tLanguageMapper;

    @Override
    public List<TLanguage> getLanguages(TLanguage language) {
        List<TLanguage> languageList = tLanguageMapper.getLanguages(language);

        return languageList;
    }

    @Override
    public String addLanguage(TLanguage language) {
        int insert = tLanguageMapper.insert(language);

        return ConstantInterface.OK_STR;
    }
}




