package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import lombok.Data;

import java.util.List;

/**
 * @ClassName TSWordVO
 * @USER: Cola
 * @Date 2023/12/15 0015 16:24
 **/
@Data
public class TSWordVO {
    private String fileName;
    private List<EntryInfoEntity> entryInfoEntities;
    private String translateType;
}
