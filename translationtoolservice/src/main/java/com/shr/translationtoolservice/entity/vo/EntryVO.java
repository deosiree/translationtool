package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName EntryVO
 * @USER: Cola
 * @Date 2023/11/21 0021 18:52
 * @Describe 词条前端交互类
 **/
@Data
public class EntryVO {
    private EntryInfoEntity entryInfoEntity;
    private List<TranslateEntity> translateEntity;
    private String TableName;
}
