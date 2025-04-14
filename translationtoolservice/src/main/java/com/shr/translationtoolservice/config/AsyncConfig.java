package com.shr.translationtoolservice.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
public class AsyncConfig {
    

    /**
     * TranslateMapper使用用来进行增删改查操作
     * @return
     */
    @Bean(name = "TranslateMapperExecutor")
    ThreadPoolExecutor translateMapperExecutor(){
        LinkedBlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor threadPoolTaskExecutor = new ThreadPoolExecutor(30,60,120,TimeUnit.SECONDS,linkedBlockingQueue);
        return threadPoolTaskExecutor;
    }

}
