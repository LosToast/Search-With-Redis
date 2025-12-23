package com.searchwithredis.searchwithredis.controller;

import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.request.ProductRequest;

import com.searchwithredis.searchwithredis.service.ProductCacheSerivice;
import com.searchwithredis.searchwithredis.service.ProductWriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/v1/products")
public class ProductSearchController {
    private final ProductCacheSerivice service;
    private final ProductWriteService productWriteService;

    public ProductSearchController(ProductCacheSerivice service, ProductWriteService productWriteService) {
        this.service = service;
        this.productWriteService = productWriteService;
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

    @PostMapping("/new")
    @ResponseStatus(HttpStatus.CREATED)
    public Products create(@RequestBody ProductRequest request) {
        Products p = new Products();
        p.setName(request.getName());
        p.setCategory(request.getCategory());
        p.setDescription(request.getDescription());
        p.setPrice(request.getPrice());
        p.setCreatedAt(LocalDateTime.now());
        return productWriteService.create(p);
    }

    @PutMapping("/{id}")
    public Products update(@PathVariable Long id,
                                  @RequestBody ProductRequest request) {
        Products p = new Products();
        p.setName(request.getName());
        p.setCategory(request.getCategory());
        p.setDescription(request.getDescription());
        p.setPrice(request.getPrice());
        return productWriteService.update(id, p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productWriteService.delete(id);
    }
}
