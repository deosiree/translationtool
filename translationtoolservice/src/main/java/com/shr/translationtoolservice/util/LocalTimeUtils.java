package com.shr.translationtoolservice.util;

import org.springframework.stereotype.Component;

import com.shr.translationtoolservice.entity.EntryInfoEntity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Component
public class LocalTimeUtils {

    public static DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    /* 导出的excel、csv对应时间单元格的内容格式 */
    public static DateFormat formatForFile = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static DateFormat formatForExportFile = new SimpleDateFormat("yyyyMMddhhmmss");
    public static DateFormat format2ForFile = new SimpleDateFormat("yyyy/MM/dd HH:mm:sss");
    
    public static DateFormat format3ForFile = new SimpleDateFormat("yyyy/MM/dd");

    static{

        LocalTimeUtils.formatForFile.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }


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
