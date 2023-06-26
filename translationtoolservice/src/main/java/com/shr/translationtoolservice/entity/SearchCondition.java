package com.shr.translationtoolservice.entity;

import com.sun.org.glassfish.gmbal.Description;
import lombok.Data;

/**
 * @ClassName SearchCondition
 * @USER: Cola
 * @Date 2023/6/19 0019 14:49
 **/
@Data
public class SearchCondition {
    @Description(value = "词条")
    private String entry;

    @Description(value = "Abbr")
    private String abbr;
    @Description(value = "中文释义")
    private String chinese_interpretation;
    @Description(value = "词条来源")
    private String entry_source;
    @Description(value = "词条状态")
    private String entry_state;
    @Description(value = "创建人")
    private String creator;
    @Description(value = "创建时间")
    private String create_time;
    @Description(value = "版本")
    private String version;
    @Description(value = "词条标签")
    private String entry_label;
    @Description(value = "词性备注")
    private String part_of_speech;

    @Description(value = "英文翻译")
    private String english;
    @Description(value = "英文翻译状态")
    private String english_translate_state;
    @Description(value = "英文禁用术语")
    private String english_disable;
    @Description(value = "俄文翻译")
    private String russian;
    @Description(value = "俄文翻译状态")
    private String russian_translate_state;
    @Description(value = "西班牙文翻译")
    private String spanish;
    @Description(value = "西班牙文翻译状态")
    private String spanish_translate_state;
    @Description(value = "法语翻译")
    private String french;
    @Description(value = "法语翻译状态")
    private String french_translate_state;

}
