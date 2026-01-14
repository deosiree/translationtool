package com.shr.translationtoolservice.service;

import java.util.Collection;
import java.util.Map;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.KeyValueArguments;

public interface TranslationStorageService {

    /**
     * 更新词条对应有语种的翻译，利用map中提供的值
     * @param entryID
     * @param translations  key: 语言类型, 例如: "英文,中文,....",value: 对应的翻译
     * @return  词条更新成功为{@code true}, 如果对应id的词条没找到返回{@code false}, 其他SQL的DML执行异常会抛出异常
     */
    boolean updateEntryInfoTranslation(String entryID,Map<String,String> translations,KeyValueArguments<String> kwargs);

    /**
     * 根据指定的语言类型，根据entryInfoEntities中对应语言的属性的值，更新库里对应词条的翻译
     * @param entryInfoEntities 词条对象, 只有entryID有用
     * @param transTypes
     * @param kwargs
     * @return  所有的词条的翻译都更新成功时返回{@code true}, 否则为{@code false}, 其他SQL的DML执行异常会抛出异常
     */
    boolean updateEntryInfoTranslations(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> transTypes,KeyValueArguments<String> kwargs);

    
}
