package com.searchwithredis.searchwithredis.service;

import com.searchwithredis.searchwithredis.entity.OutboxEvent;
import com.searchwithredis.searchwithredis.entity.Products;
import com.searchwithredis.searchwithredis.repository.OutboxRepository;
import com.searchwithredis.searchwithredis.repository.ProductJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProductWriteService {
    private final ProductJpaRepository productJpaRepository;
    private final OutboxRepository outboxRepository;

    public ProductWriteService(ProductJpaRepository productJpaRepository, OutboxRepository outboxRepository) {
        this.productJpaRepository = productJpaRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public Products create(Products p) {
        Products saved = productJpaRepository.save(p);
        writeOutbox("UPSERT", saved.getId().toString());
        return saved;
    }

    @Transactional
    public Products update(Long id, Products patch) {
        Products existing = productJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        existing.setName(patch.getName());
        existing.setCategory(patch.getCategory());
        existing.setPrice(patch.getPrice());
        existing.setDescription(patch.getDescription());

        Products saved = productJpaRepository.save(existing);
        writeOutbox("UPSERT", saved.getId().toString());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        productJpaRepository.deleteById(id);
        writeOutbox("DELETE", id.toString());
    }

    private void writeOutbox(String eventType, String productId) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("PRODUCT");
        e.setAggregateId(productId);
        e.setEventType(eventType);
        outboxRepository.save(e);
    }
}

