package com.shr.translationtoolservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    

    /**
     * TranslateMapper使用用来进行增删改查操作
     * @return
     */
    @Bean(name = "TranslateMapperExecutor")
    ThreadPoolTaskExecutor translateMapperExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(5);
        threadPoolTaskExecutor.setMaxPoolSize(10);
        threadPoolTaskExecutor.setQueueCapacity(15);
        return threadPoolTaskExecutor;
    }

}
