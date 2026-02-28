package com.shr.translationtoolservice.service;

import java.util.Collection;
import java.util.Map;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.KeyValueArguments;

public interface TranslationStorageService {


    /**
     * 根据指定的语言类型，根据entryInfoEntities中对应语言的属性的值，更新库里对应词条的翻译
     * @param entryInfoEntities 词条对象, 只有entryID有用
     * @param transTypes    英文,中文, 俄文
     * @param kwargs
     * @return  所有的词条的翻译都更新成功时返回{@code true}, 否则为{@code false}, 其他SQL的DML执行异常会抛出异常
     */
    boolean updateEntryInfoTranslations(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> transTypes,KeyValueArguments<String> kwargs);

    
}
