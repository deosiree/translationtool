package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryTempEntity;
import lombok.Data;

import java.util.List;

/**
 * @ClassName EntryTempCompareVO
 * @USER: Cola
 * @Date 2023/12/29 0029 11:35
 **/

@Data
public class EntryTempCompareVO {
    private List<EntryTempEntity> oldEntrytempList;
    private List<EntryTempEntity> newEntrytempList;
    private String tableName;
}
