package com.searchwithredis.searchwithredis.repository;

import com.searchwithredis.searchwithredis.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop500ByProcessedAtIsNullOrderByIdAsc();
}
