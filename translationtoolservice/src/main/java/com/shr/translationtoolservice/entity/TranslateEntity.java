package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;

/**
 * @ClassName TranslateEntity
 * @USER: Cola
 * @Date 2023/7/6 0006 10:06
 **/
@Data
public class TranslateEntity {
   private List<LanguageEntity> languageEntities;
    private String entry;
    private String source;
}
