package com.shr.translationtoolservice.util;

/**
 * 上下文
 *
 * @date ：Created in 2023/8/8 9:13
 * @description：ApplicationContextUtil
 */

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext args) throws BeansException
    {
        com.shr.translationtoolservice.util.ApplicationContextUtil.applicationContext = args;
    }

    public static ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    /*
     *@Description: 根据bean的id获取对象
     *@Param: [id]
     *@return: T
     *@Author: wenfeng
     */
    public static <T> T getObject(String id)
    {
        Object obj = applicationContext.getBean(id);
        if (obj != null) {
            return (T) obj;
        }
        else {
            return null;
        }
    }

}
