package com.redis.socialmediatracker;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class SocialmediatrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialmediatrackerApplication.class, args);
    }

}
