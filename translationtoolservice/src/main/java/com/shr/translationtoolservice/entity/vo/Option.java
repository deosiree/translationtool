package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName Options
 * @Description TODO
 * @USER: Cola
 * @Date 2024/9/4 0004 18:50
 **/
@Data
public class Option {
    private String label;
    private List<String> options;
    // 构造函数
    public Option() {}

    public Option(String label, List<String> options) {
        this.label = label;
        this.options = options;
    }
}
