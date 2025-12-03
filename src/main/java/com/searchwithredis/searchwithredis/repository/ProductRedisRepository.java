package com.searchwithredis.searchwithredis.repository;

import com.redis.om.spring.annotations.Query;
import com.redis.om.spring.autocomplete.Suggestion;
import com.redis.om.spring.repository.RedisDocumentRepository;
import com.redis.om.spring.repository.query.autocomplete.AutoCompleteOptions;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRedisRepository extends RedisDocumentRepository<RedisProductEntity , String> {
    // Full-text search on name (because name is @Searchable)

    @Query("$q")
    List<RedisProductEntity> search(@Param("q") String q);

    // Autocomplete on name (because name is @AutoComplete)
    List<Suggestion> autoCompleteName(String query);

    // Autocomplete on name with options (payload, fuzzy, limit, etc.)
    List<Suggestion> autoCompleteName(String query, AutoCompleteOptions options);
}
