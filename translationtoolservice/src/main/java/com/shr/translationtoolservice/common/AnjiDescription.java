package com.shr.translationtoolservice.common;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AnjiDescription {

    /**
     * 加上注解默认对比
     * @return
     */
    String value();
}