package com.searchwithredis.searchwithredis;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.searchwithredis.searchwithredis.bulkLoad.ProductBulkLoader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class SearchWithRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchWithRedisApplication.class, args);
    }

  /*  @Bean
    CommandLineRunner loadToRedis(ProductBulkLoader loader){
        return args -> {
          loader.loadAllToRedis(10_000);
        };
    }*/
}
