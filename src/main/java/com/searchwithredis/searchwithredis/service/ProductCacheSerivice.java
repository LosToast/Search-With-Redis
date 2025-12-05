package com.searchwithredis.searchwithredis.service;

import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.repository.ProductJpaRepository;
import com.searchwithredis.searchwithredis.repository.ProductRedisRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductCacheSerivice {
    private ProductJpaRepository dbRepo;
    private ProductRedisRepository redisRepo;

    public ProductCacheSerivice(ProductJpaRepository productJpaRepository,
                                ProductRedisRepository productRedisRepository) {
        this.dbRepo = productJpaRepository;
        this.redisRepo = productRedisRepository;
    }

    // this is a Cache-Aside Pattern
    public Page<RedisProductEntity> search(String q , Pageable pageable){
        String rsQuery = buildNameContainsQuery(q);
        //List<RedisProductEntity> cached = redisRepo.search(rsQuery);
        Page<RedisProductEntity> redisPage = redisRepo.search(rsQuery, pageable);
        if(!redisPage.isEmpty())
            return redisPage;

        //if cache miss
        Page<Products> products = dbRepo.searchByNameNative(q , pageable);
        if(products.isEmpty())
            return new PageImpl<>(List.of(), pageable, 0);

        //mapping the products from db to redisProductEntity
        List<RedisProductEntity> docs = products.stream()
                .map(this::mapToRedis)
                .toList();

        redisRepo.saveAll(docs);
        return new PageImpl<>(docs, pageable, products.getTotalElements());
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
