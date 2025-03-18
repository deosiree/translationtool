package com.shr.translationtoolservice.util.processor.impl;

import org.springframework.stereotype.Component;

import com.shr.translationtoolservice.util.processor.TransProcessor;


@Component("DefaultTransProcessor")
public class DefaultTransProcessor implements TransProcessor {

    @Override
    public String correctPlaceHolder(String text) {
        // [switch ({:d})]{:s}->{:s}[{:s}]网络组 通过[{:s}]网({:s})建立连接
        // 将数据库中的存在占位符的东西进行修正
        return text.replace("{: d}", "{:d}").
            replace("{: s}", "{:s}");

    }
    
}
