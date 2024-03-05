package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @ClassName DictionaryVo
 * @Description
 * @USER: Cola
 * @Date 2023/12/15 0015 15:33
 **/
@Data
public class DictionaryVo {
    private String comments;
    private String source;
    private String tag;
    // "en_US": "dofodifdoi1",
    private List<Map<String, String>> translation;
}
