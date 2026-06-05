package com.elderlycare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.elderlycare.mapper")
public class CommunityElderlyCareApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityElderlyCareApplication.class, args);
    }
}