package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import lombok.Data;

import java.util.List;

/**
 * @ClassName ImportResultEntryVO
 * @Description  导入词条结果
 * @USER: Cola
 * @Date 2024/2/19 0019 16:13
 **/
@Data
public class ImportResultEntryVO {
    private List<EntryInfoEntity> existEntryList;
    private List<EntryInfoEntity> importEntryList;
}
