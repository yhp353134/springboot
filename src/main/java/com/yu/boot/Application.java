package com.yu.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * 启动类
 * */
@SpringBootApplication
@EnableAutoConfiguration
public class Application extends SpringBootServletInitializer {

    public SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    /***
     * 启动方法
     * */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
