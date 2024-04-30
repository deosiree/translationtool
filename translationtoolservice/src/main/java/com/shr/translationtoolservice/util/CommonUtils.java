package com.shr.translationtoolservice.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
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
    public boolean checkPage(int pageIndex, int pageSize)
    {
        return StringUtils.isNotBlank(String.valueOf(pageIndex)) && StringUtils.isNotBlank(String.valueOf(pageSize));
    }

}
