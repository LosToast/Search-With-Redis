package com.searchwithredis.searchwithredis.service;

import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.repository.ProductJpaRepository;
import com.searchwithredis.searchwithredis.repository.ProductRedisRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductCacheSerivice {
    private ProductJpaRepository dbRepo;
    private ProductRedisRepository redisRepo;

    public ProductCacheSerivice(ProductJpaRepository productJpaRepository, ProductRedisRepository productRedisRepository) {
        this.dbRepo = productJpaRepository;
        this.redisRepo = productRedisRepository;
    }

    // this is a Cache-Aside Pattern
    public List<RedisProductEntity> search(String q , int limit){
        String rsQuery = buildNameContainsQuery(q);
        List<RedisProductEntity> cached = redisRepo.search(rsQuery);
        if(!cached.isEmpty())
            return cached.stream().limit(limit).toList();

        //if cache miss
        List<Products> products = dbRepo.searchByNameNative(q , limit);
        if(products.isEmpty())
            return List.of();

        //mapping the products from db to redisProductEntity
        List<RedisProductEntity> docs = products.stream()
                .map(this::mapToRedis)
                .toList();

        redisRepo.saveAll(docs);
        return docs;
    }

    private RedisProductEntity mapToRedis(Products products) {
        RedisProductEntity redisProduct = new RedisProductEntity();
        redisProduct.setId(String.valueOf(products.getId()));
        redisProduct.setName(products.getName());
        redisProduct.setCategory(products.getName());
        redisProduct.setPrice(products.getPrice());
        redisProduct.setDescription(products.getDescription());
        redisProduct.setCreatedAt(products.getCreatedAt());
        return redisProduct;
    }

    /*
    * RediSearch treats characters like -[]{}()|=>@~*:"\ as operators.
    * User might type them normally, so escape them.
    * */
    private String escapeRediSearchToken(String t) {
        return t.replaceAll("([\\\\\\-\\[\\]\\{\\}\\(\\)\\|\\=\\>\\<\\~\\*\\:\\\"\\@])", "\\\\$1");
    }

    private String buildNameContainsQuery(String q) {
        String normalized = q == null ? "" : q.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return "*"; // match all (or return empty)

        String[] tokens = normalized.split(" ");

        String tokenPart = java.util.Arrays.stream(tokens)
                .filter(t -> !t.isBlank())
                .map(this::escapeRediSearchToken)
                .map(t -> "*" + t + "*")          // wildcard each token
                .reduce((a, b) -> a + " " + b)    // AND by default inside ()
                .orElse("*");

        return "@name:(" + tokenPart + ")";
    }


}
