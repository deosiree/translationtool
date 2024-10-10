package com.shr.translationtoolservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

@SpringBootApplication
@Slf4j
public class TranslationtoolserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TranslationtoolserviceApplication.class, args);
    }

}
