package com.searchwithredis.searchwithredis.repository;

import com.redis.om.spring.repository.RedisDocumentRepository;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;

public interface ProductRedisRepository extends RedisDocumentRepository<RedisProductEntity , String> {

}
