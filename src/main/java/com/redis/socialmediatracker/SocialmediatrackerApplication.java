package com.redis.socialmediatracker;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRedisDocumentRepositories
@EnableAsync
@EnableScheduling
public class SocialmediatrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialmediatrackerApplication.class, args);
    }

}
