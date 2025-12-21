package com.searchwithredis.searchwithredis.service;

import com.redis.om.spring.autocomplete.Suggestion;
import com.redis.om.spring.repository.query.autocomplete.AutoCompleteOptions;
import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.repository.ProductJpaRepository;
import com.searchwithredis.searchwithredis.repository.ProductRedisRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        String rsQuery = buildContainsQueryOnName(q);
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
        redisProduct.setCategory(products.getCategory());
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

   /* public List<String> autocomplete(String q, int limit, boolean fuzzy) {
        String query = (q == null) ? "" : q.trim();
        if (query.length() < 2) return List.of(); // avoid spam + better UX

        AutoCompleteOptions options = AutoCompleteOptions.get()
                .limit(Math.min(Math.max(limit, 1), 20))  // 1..20
                .fuzzy();

        List<Suggestion> suggestions = redisRepo.autoCompleteName(query, options);

        // Convert Suggestion -> plain strings for the UI
        return suggestions.stream()
                .map(Suggestion::getValue) // Redis OM Suggestion has getString()
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> autocompleteContains(String q, int limit) {
        if (q == null) return List.of();
        String query = q.trim();
        if (query.length() < 2) return List.of();

        int safeLimit = Math.min(Math.max(limit, 1), 20);

        // Build a RediSearch query that matches tokens anywhere in name:
        // Example: "Watch" -> @name:(*watch*)
        String redisQuery = buildContainsQueryOnName(query);

        return redisRepo.search(redisQuery, PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(RedisProductEntity::getName)
                .distinct()
                .toList();
    }*/

    private String buildContainsQueryOnName(String input) {
        // Normalize spacing; RediSearch is typically case-insensitive for TEXT,
        // but keeping normalization helps consistency.
        String normalized = input.trim().replaceAll("\\s+", " ");

        String[] tokens = normalized.split(" ");

        String tokenPart = java.util.Arrays.stream(tokens)
                .filter(t -> !t.isBlank())
                .map(this::escapeRediSearchToken)
                .map(t -> t + "*")           // ✅ prefix-only
                .reduce((a, b) -> a + " " + b)     // AND across tokens
                .orElse("*");

        return "@name:(" + tokenPart + ")";
    }

    public List<String> autocompleteHybrid(String q, int limit, boolean fuzzyRequested) {
        if (q == null) return List.of();

        String query = q.trim();
        if (query.length() < 2) return List.of();

        int safeLimit = Math.min(Math.max(limit, 1), 20);

        // Use LinkedHashSet to keep order + avoid duplicates
        Set<String> out = new LinkedHashSet<>();

        // ---------- 1) TRUE AUTOCOMPLETE (prefix-based) ----------
        AutoCompleteOptions options = AutoCompleteOptions.get()
                .limit(Math.min(Math.max(limit, 1), 20))  // 1..20
                .fuzzy();


        List<Suggestion> ac = redisRepo.autoCompleteName(query, options);
        if (ac != null) {
            for (Suggestion s : ac) {
                if (s != null && s.getValue() != null && !s.getValue().isBlank()) {
                    out.add(s.getValue());
                    if (out.size() >= safeLimit) return out.stream().toList();
                }
            }
        }

        // ---------- 2) FALLBACK: "contains" suggestions using RediSearch ----------
        // Only do this if autocomplete didn't fill enough items
        String redisQuery = buildContainsQueryOnName(query); // helper below

        var page = redisRepo.search(redisQuery, PageRequest.of(0, safeLimit));
        if (page != null && page.getContent() != null) {
            for (RedisProductEntity p : page.getContent()) {
                if (p != null && p.getName() != null && !p.getName().isBlank()) {
                    out.add(p.getName());
                    if (out.size() >= safeLimit) break;
                }
            }
        }

        return out.stream().toList();
    }


}
