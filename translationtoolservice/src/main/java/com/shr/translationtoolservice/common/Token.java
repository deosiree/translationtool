package com.shr.translationtoolservice.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author
 * @date 2023/6/6 14:53
 * @description: 自定义跳过token验证的注解，如果不加该注解直接拦截
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Token {
    boolean required() default true;
}
