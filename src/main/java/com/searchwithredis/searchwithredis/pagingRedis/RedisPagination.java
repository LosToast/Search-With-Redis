/*
package com.searchwithredis.searchwithredis.pagingRedis;

import com.google.gson.Gson;
import com.redis.om.spring.ops.RedisModulesOperations;
import com.redis.om.spring.ops.search.SearchOperations;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RedisPagination {
    private final RedisModulesOperations<String> modulesOps;
    private final Gson gson;

    @Autowired
    public RedisPagination(RedisModulesOperations<String> modulesOps, Gson gson) {
        this.modulesOps = modulesOps;
        this.gson = gson;
    }

    public Page<RedisProductEntity> searchPaged(String rsQuery, Pageable pageable) {
        int offset = (int) pageable.getOffset();  // page * size
        int size = pageable.getPageSize();

        // Perform search with pagination
        SearchOperations<String> searchOps = modulesOps.opsForSearch("com.searchwithredis.searchwithredis.entity.RedisProductEntityIdx");

        Query query = new Query(rsQuery)
                .limit(offset, size);

        // Execute search
        SearchResult rawResult = searchOps.search(query);

        long totalResults = rawResult.getTotalResults();

        // Return paginated result as a Page
        List<RedisProductEntity> content = rawResult.getDocuments()
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, totalResults);
    }
    private RedisProductEntity toEntity(Document doc) {
        // For RediSearch JSON indexes, Redis OM stores the full JSON under "$"
        Object json = doc.get("$");
        if (json == null) {
            // fallback: doc.toString() if needed
            return gson.fromJson(doc.toString(), RedisProductEntity.class);
        }
        return gson.fromJson(json.toString(), RedisProductEntity.class);
    }
}
*/
