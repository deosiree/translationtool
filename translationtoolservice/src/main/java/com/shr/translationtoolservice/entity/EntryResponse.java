package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;

/**
 * @ClassName 模糊查询词条返回实体
 * @USER: Cola
 * @Date 2023/9/13 0013 17:19
 **/
@Data
public class EntryResponse {
    //版本库词条
    List<VersionEntity> VersionEntries;
    //模糊查询词条
    List<EntryCommonEntity> fuzzyEntries;
}
