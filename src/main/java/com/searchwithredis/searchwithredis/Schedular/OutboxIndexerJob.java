package com.searchwithredis.searchwithredis.Schedular;

import com.searchwithredis.searchwithredis.entity.OutboxEvent;
import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.entity.RedisProductEntity;
import com.searchwithredis.searchwithredis.repository.OutboxRepository;
import com.searchwithredis.searchwithredis.repository.ProductJpaRepository;
import com.searchwithredis.searchwithredis.repository.ProductRedisRepository;
import com.searchwithredis.searchwithredis.service.ProductCacheSerivice;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
@Component
public class OutboxIndexerJob {
    private final OutboxRepository outboxRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductRedisRepository productRedisRepository;
    private final ProductCacheSerivice productCacheSerivice;

    public OutboxIndexerJob(OutboxRepository outboxRepository,
                            ProductJpaRepository productJpaRepository,
                            ProductRedisRepository productRedisRepository,
                            ProductCacheSerivice productCacheSerivice) {
        this.outboxRepository = outboxRepository;
        this.productJpaRepository = productJpaRepository;
        this.productRedisRepository = productRedisRepository;
        this.productCacheSerivice = productCacheSerivice;
    }

    @Scheduled(fixedDelay = 1000) // every 1s
    @Transactional
    public void processOutbox() {
        /*The scheduler tracks “already processed” using processedAt
        * It only selects events where processedAt IS NULL
        * It marks processed only after success
        * Failed events remain unprocessed and retry
        * */

        List<OutboxEvent> events = outboxRepository.findTop500ByProcessedAtIsNullOrderByIdAsc();
        if (events.isEmpty()) return;

        for (OutboxEvent e : events) {
            try {
                if ("UPSERT".equals(e.getEventType())) {
                    Long id = Long.valueOf(e.getAggregateId());
                    Products p = productJpaRepository.findById(id).orElse(null);
                    if (p != null) {
                        RedisProductEntity redis = productCacheSerivice.mapToRedis(p);
                        productRedisRepository.save(redis); // UPSERT
                    }
                } else if ("DELETE".equals(e.getEventType())) {
                    productRedisRepository.deleteById(e.getAggregateId());
                }

                e.setProcessedAt(LocalDateTime.now());
                e.setLastError(null);

            } catch (Exception ex) {
                // leave processedAt null so it retries
                e.setAttempts(e.getAttempts() + 1);
                e.setLastError(ex.getMessage());
                // optional: if attempts > N, move to "dead" state
            }
        }
    }
}
