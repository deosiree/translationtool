package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.TranslateEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 *
 */
public interface TranslateService extends IService<TranslateEntity> {

    String updateTranslation(List<TranslateEntity> translateEntityList);

    String addTranslate(TranslateEntity translateEntity);
}
