package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;



/**
 * @ClassName SourceEntryVO
 * @Description 不同来源中 所属任务的新增词条类
 * @USER: Cola
 * @Date 2025/2/28 210093 18:50
 **/
@Data
public class SourceEntryVO {
    private String type;

    private List<SourceFileAndEntryVO> SourceFileAndEntryVO;
}
