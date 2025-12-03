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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
