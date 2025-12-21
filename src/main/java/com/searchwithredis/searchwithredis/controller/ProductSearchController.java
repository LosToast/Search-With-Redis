package com.searchwithredis.searchwithredis.controller;

import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.service.ProductCacheSerivice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public Page<RedisProductEntity> search(@RequestParam String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page , size);
        return service.search(q,pageable);
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam String q,
                                     @RequestParam(defaultValue = "10") int limit,
                                     @RequestParam(defaultValue = "true") boolean fuzzy) {
        return service.autocompleteHybrid(q, limit , fuzzy);
    }
}
