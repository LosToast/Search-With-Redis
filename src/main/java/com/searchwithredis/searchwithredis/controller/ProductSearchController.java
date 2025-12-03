package com.searchwithredis.searchwithredis.controller;

import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.service.ProductCacheSerivice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/products")
public class ProductSearchController {
    private final ProductCacheSerivice service;

    public ProductSearchController(ProductCacheSerivice service) {
        this.service = service;
    }
    @GetMapping("/search")
    public List<RedisProductEntity> search(@RequestParam String q,
                                           @RequestParam(defaultValue = "5") int limit) {
        return service.search(q, limit);
    }
}
