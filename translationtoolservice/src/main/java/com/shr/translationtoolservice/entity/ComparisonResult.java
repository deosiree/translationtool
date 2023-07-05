package com.shr.translationtoolservice.entity;

import lombok.Data;

/**
 * @ClassName ComparisonResult
 * @Description 比较结果
 * @USER: Cola
 * @Date 2023/7/4 0004 16:51
 **/
@Data
public class ComparisonResult {
    private String key;
    private String label;
    private String previous;
    private String later;
    private String str;
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getLater() {
        return later;
    }

    public void setLater(String later) {
        this.later = later;
    }
}

