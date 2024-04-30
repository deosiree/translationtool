package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.TaskInfoEntity;
import lombok.Data;

import java.util.List;

/**
 * @ClassName upgradeVO
 * @USER: Cola
 * @Date 2023/11/24 0024 11:40
 **/

@Data
public class UpgradeVO {

    private List<EntryVO> entryVOList;
    private  TaskInfoEntity taskInfoEntities;

}
