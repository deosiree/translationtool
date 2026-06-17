package com.shr.translationtoolservice.service.processor.converter;

import org.springframework.stereotype.Component;

import com.alibaba.druid.util.StringUtils;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.service.parser.AppInfoDictXMLParser.AppDictInfo;

@Component
public class AppDictInfoConverter implements EntryInfoEntityConverter<AppDictInfo>{

    @Override
    public EntryInfoEntity apply(AppDictInfo appDictInfo) {
        EntryInfoEntity entity = new EntryInfoEntity();
        String chinese = appDictInfo.getCnDesc();
        String english = appDictInfo.getEnDesc();
        String spanish = appDictInfo.getEsDesc();
        String russian = appDictInfo.getRuDesc();

        entity.setEntry(appDictInfo.getAbbr());

        entity.setChinese(chinese);
        entity.setZhCharLength(chinese.length());
        entity.setChineseTranslateState(StringUtils.isEmpty(chinese) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

        /* 写英文翻译 */
        entity.setEnglish(english);
        entity.setEnCharLength(english.length());
        entity.setEnglishTranslateState(StringUtils.isEmpty(english) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

        /* 写西班牙语 */
        entity.setSpanish(spanish);
        entity.setSpaCharLength(spanish.length());
        entity.setSpanishTranslateState(StringUtils.isEmpty(spanish) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

        /* 写俄文翻译 */
        entity.setRussian(russian);
        entity.setRuCharLength(russian.length());
        entity.setRussianTranslateState(StringUtils.isEmpty(russian) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

        /* 俄文暂不支持 */
        entity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);

        return entity;
    }
    
}
