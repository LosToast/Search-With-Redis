package com.searchwithredis.searchwithredis.entity;

import com.redis.om.spring.annotations.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document("products")
public class RedisProductEntity {
    @Id
    private String id; // usually same as Postgres id -> String.valueOf(id)

    @Searchable
    @AutoComplete
    private String name;

    @Indexed
    @AutoCompletePayload("name")
    private String category;

    @Indexed
    @AutoCompletePayload("name")
    private BigDecimal price;
    private String description;
    private LocalDateTime createdAt;
}
