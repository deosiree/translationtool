package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName EntryParentEntity
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/29 0029 16:03
 **/

@Data
public class EntryParentEntity {
    EntryProjectEntity entryProjectEntity;

    EntryProductEntity entryProductEntity;

    EntryCommonEntity entryCommonEntity;

}
