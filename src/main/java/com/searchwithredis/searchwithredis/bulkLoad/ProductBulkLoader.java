package com.searchwithredis.searchwithredis.bulkLoad;

import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.repository.ProductJpaRepository;
import com.searchwithredis.searchwithredis.repository.ProductRedisRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductBulkLoader {
    private static final String PROGRESS_KEY = "bulkload:lastId";
    private ProductJpaRepository dbRepo;
    private ProductRedisRepository redisRepo;
    private final StringRedisTemplate stringRedisTemplate;

    public ProductBulkLoader(ProductJpaRepository dbRepo,
                             ProductRedisRepository redisRepo,
                             StringRedisTemplate stringRedisTemplate) {
        this.dbRepo = dbRepo;
        this.redisRepo = redisRepo;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional()
    public void loadAllToRedis(int batchSize) {

        long lastId = getLastIdFromRedis();  // ✅ resume point
        long totalLoaded = 0;

        while (true) {
            List<Products> batch = dbRepo.fetchNextBatch(lastId, batchSize);
            if (batch.isEmpty()) break;

            List<RedisProductEntity> docs = batch.stream()
                    .map(this::mapToRedis)
                    .toList();

            redisRepo.saveAll(docs);

            lastId = batch.get(batch.size() - 1).getId();
            totalLoaded += batch.size();

            saveLastIdToRedis(lastId);  // ✅ checkpoint every batch

            System.out.println("Loaded so far. lastId=" + lastId +
                    " totalLoaded=" + totalLoaded);
        }

        System.out.println("✅ Bulk load complete.");
    }

    private long getLastIdFromRedis() {
        String v = stringRedisTemplate.opsForValue().get(PROGRESS_KEY);
        return (v == null) ? 0L : Long.parseLong(v);
    }

    private void saveLastIdToRedis(long lastId) {
        stringRedisTemplate.opsForValue().set(PROGRESS_KEY, String.valueOf(lastId));
    }


    private RedisProductEntity mapToRedis(Products p) {
        RedisProductEntity r = new RedisProductEntity();
        r.setId(String.valueOf(p.getId()));
        r.setName(p.getName());
        r.setCategory(p.getCategory());
        r.setPrice(p.getPrice());
        r.setDescription(p.getDescription());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
