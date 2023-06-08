package com.shr.translationtoolservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class CommonUtils {
    /**
     * 获取uuid
     * @return
     */
    public String getUUID(){
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }
}
