package com.redis.socialmediatracker;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRedisDocumentRepositories
@EnableAsync
public class SocialmediatrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialmediatrackerApplication.class, args);
    }

}
