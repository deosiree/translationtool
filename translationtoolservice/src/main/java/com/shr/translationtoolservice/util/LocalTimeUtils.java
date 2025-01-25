package com.shr.translationtoolservice.util;

import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Component
public class LocalTimeUtils {

    public Date getBeijingTime() {
        // 获取当前时间
        Date date = new Date(System.currentTimeMillis());

        // 创建一个 Calendar 实例并设置为当前时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // 设置时区为北京时间
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        calendar.setTimeZone(timeZone);

        // 获取北京时间
        Date beijingTime = calendar.getTime();

        // 格式化输出
        return  beijingTime;
    }
}
