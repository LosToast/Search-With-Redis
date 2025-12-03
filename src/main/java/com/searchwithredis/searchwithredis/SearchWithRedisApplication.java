package com.searchwithredis.searchwithredis;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class SearchWithRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchWithRedisApplication.class, args);
    }

}
