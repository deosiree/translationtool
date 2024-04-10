package com.shr.translationtoolservice.config;

import com.shr.translationtoolservice.interceptor.JWTInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> urls = new ArrayList<>();
        urls.add("/swagger-resources/**");
        urls.add("/v3/**");
        urls.add("/swagger-ui/**");
        urls.add("/error");
        urls.add("/userLogin/login");
        urls.add("/public/**");
//        addInterceptor 就是加过滤器
        registry.addInterceptor(new JWTInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(urls);
    }
}
