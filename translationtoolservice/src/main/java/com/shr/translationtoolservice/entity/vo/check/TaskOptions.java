package com.shr.translationtoolservice.entity.vo.check;

public class TaskOptions {

    // 是否将空字符串视为有效值
    private boolean emptyStringAsValue;
    // 是否快速失败（遇到第一个错误就停止）
    private boolean failFast;
    public boolean isEmptyStringAsValue() {
        return emptyStringAsValue;
    }
    public boolean isFailFast() {
        return failFast;
    }

    
}
