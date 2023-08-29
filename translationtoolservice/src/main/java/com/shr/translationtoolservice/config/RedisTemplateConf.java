package com.shr.translationtoolservice.config;

/**
 * @date ：Created in 2021/4/8 9:15
 * @description：RedisTemplateConf
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;
import java.time.Duration;

@Configuration
@Slf4j
public class RedisTemplateConf {
    @Value("${spring.redis.database}")
    int database;
    @Value("${spring.redis.host}")
    String host;
    @Value("${spring.redis.port}")
    int port;
    @Value("${spring.redis.password}")
    String password;
    @Value("${spring.redis.timeout}")
    int timeout;

    @Bean("redisTemplate")
    public RedisTemplate<String, Serializable> redisTemplate(LettuceConnectionFactory myLettuceConnectionFactory)
    {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder
                builder = LettucePoolingClientConfiguration.builder().
                commandTimeout(Duration.ofMillis(timeout));

        LettuceClientConfiguration lettuceClientConfiguration = builder.build();

        RedisConfiguration configuration = new RedisStandaloneConfiguration(host, port);
        ((RedisStandaloneConfiguration) configuration).setDatabase(database);
        ((RedisStandaloneConfiguration) configuration).setPassword(password);

        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
//        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);

        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);



        myLettuceConnectionFactory = new LettuceConnectionFactory(configuration, lettuceClientConfiguration);
        myLettuceConnectionFactory.afterPropertiesSet();

        template.setConnectionFactory(myLettuceConnectionFactory);
        return template;
    }

}
