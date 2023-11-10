package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.TLanguage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 *
 */
public interface TLanguageService extends IService<TLanguage> {

    List<TLanguage> getLanguages(TLanguage language);
}
