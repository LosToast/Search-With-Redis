# Project Documentation: Redis-based Search & Autocomplete System

## 1. Project Overview

This project is a **production-style implementation of a search and autocomplete system** built on top of **Redis / Redis Stack** using **Redis OM**. The goal is to simulate how large-scale systems (≈ **1 million product records**) implement **low-latency, high-throughput search and autocomplete** while keeping data consistent with a primary relational database.

The project demonstrates:
- How Redis can be used as a **search engine**, not just a cache
- How to design **event-driven synchronization** between a relational database and Redis
- How to support **real-time autocomplete & search** at scale
- How to structure code so it remains understandable even after long gaps in development

If you return to this project after months, this document should help you quickly understand **why it exists, what problem it solves, how it works, and what technologies are involved**.

---

## 2. Problem Statement

Traditional database-based search (e.g., SQL `LIKE '%term%'`) does not scale well for:
- Autocomplete suggestions
- Full-text or prefix search
- Low-latency responses under heavy read load

In real-world systems:
- Reads are far more frequent than writes
- Search must respond in **milliseconds**
- Systems often contain **hundreds of thousands to millions of records**

This project solves these problems by:
1. Storing **source-of-truth data** in a relational database
2. Indexing searchable fields in **Redis Stack**
3. Querying Redis directly for search/autocomplete
4. Keeping Redis in sync using the **Outbox Pattern**

---

## 3. High-Level Architecture

### Components

```
Client
  │
  ▼
Spring Boot REST API (Controller)
  │
  ├── READ path (Search/Autocomplete)
  │     └── ProductCacheSerivice → Redis Stack (RediSearch + Autocomplete)
  │
  ├── WRITE path (Create/Update/Delete)
  │     └── ProductWriteService → Relational DB (JPA) + Outbox table
  │
  └── Background sync
        └── OutboxIndexerJob → reads Outbox → updates Redis index
```

### Key Principles
- **Database is the source of truth**
- **Redis is a read-optimized projection** for search/autocomplete
- **Eventual consistency** between DB and Redis (via Outbox)
- **Reads are isolated from writes** so read traffic doesn’t overload the DB

---

## 4. Core Goal of the Project

The main goals are:

1. Implement **fast autocomplete and search** using Redis
2. Handle **large datasets (~1M records)** efficiently
3. Avoid tight coupling between database writes and Redis indexing
4. Demonstrate **real-world patterns** used in production systems
5. Keep the system **observable, debuggable, and maintainable**

This is **not just a demo**—it is designed to resemble how real backend systems are built.

---

## 5. Technology Stack

### Backend
- **Java**
- **Spring Boot**
- **Spring Data JPA**

### Data Storage
- **Relational Database** (PostgreSQL / MySQL)
  - Persistent storage
  - ACID guarantees

- **Redis Stack**
  - RedisJSON
  - RediSearch
  - Indexed search & autocomplete

### Redis Integration
- **Redis OM**
  - Object mapping
  - Index management
  - Search query abstraction

### Architectural Patterns
- **Outbox Pattern** (DB transaction + async indexing)
- **Eventual Consistency** (Redis catches up to DB)
- **CQRS-inspired separation** (explained below)

> Note: This project does **not** implement “full CQRS” (separate command/query models, buses, handlers, etc.). Instead, it uses a **CQRS-style separation of responsibilities**: write operations go through the DB/outbox, while search queries are served from Redis.

---

## 6. Domain Model

### Product (Relational DB)

- Stored in the relational database
- Represents the authoritative version of product data

Key responsibilities:
- Persistence
- Transactions
- Business validation

---

### RedisProductEntity (Redis)

- Stored as JSON documents in Redis
- Indexed for fast search and autocomplete

Used for:
- Prefix search
- Text search
- High-performance reads

Redis is **not** the source of truth—it is a **search-optimized projection**.

---

## 7. Write Flow (Data Creation / Update)

### Step-by-Step

1. Client sends a request to create/update a product
2. `ProductWriteService`
   - Saves product to the relational database
   - Creates an **OutboxEvent** in the same transaction
3. Transaction commits
4. No Redis interaction happens here

### Why?
- Prevents partial failures
- Guarantees DB + Outbox consistency
- Keeps write path simple and safe

---

## 8. Outbox Pattern Explained

### What is the Outbox?

The Outbox is a database table that stores **events describing what changed**.

Example events:
- PRODUCT_CREATED
- PRODUCT_UPDATED

Each event contains:
- Entity ID
- Event type
- Payload (or reference to entity)
- Processing status

---

### Why Use Outbox?

- Avoids distributed transactions
- Prevents DB–Redis inconsistencies
- Allows retries and failure recovery
- Scales independently

This is a **production-grade pattern** widely used in microservices.

---

## 9. Background Indexing Job

### OutboxIndexerJob

Responsibilities:
- Poll unprocessed Outbox events
- Load product data from DB
- Convert to RedisProductEntity
- Save/update Redis index
- Mark Outbox event as processed

This job:
- Runs asynchronously
- Can be scaled horizontally
- Can be retried safely

---

## 10. Read Flow (Search & Autocomplete)

### Step-by-Step

1. Client calls search/autocomplete endpoint
2. `ProductSearchController`
3. Delegates to `ProductCacheSerivice`
4. Query is executed **directly on Redis** (RediSearch / Redis OM)
5. Results returned quickly

### Cache-aside behavior in this project
`ProductCacheSerivice.search(q, pageable)` first tries Redis.
- If Redis returns results → return immediately.
- If Redis is empty (cache miss) → query DB, map results to `RedisProductEntity`, save to Redis, then return.

### Important
- The read path is optimized for speed and high concurrency.
- The DB is used as a fallback (and as the source of truth).

---

## 11. Redis Search & Autocomplete Strategy

### Indexing
- Products are stored in Redis as JSON documents (`RedisProductEntity`).
- Fields like `name` are indexed for:
  - **prefix search** (autocomplete)
  - **contains / token search** (search)

### Autocomplete approaches implemented
In `ProductCacheSerivice` you have two autocomplete options:

1) **True autocomplete** using Redis OM’s `autoCompleteName(...)`
- Uses `AutoCompleteOptions` (limit, fuzzy)
- Returns `Suggestion` objects which are mapped to strings

2) **Hybrid autocomplete** (`autocompleteHybrid`)
- First tries true autocomplete suggestions
- Then falls back to a search query and extracts product names
- Merges results with de-duplication (LinkedHashSet)

### RediSearch query building
`buildContainsQueryOnName(q)` builds a RediSearch query with:
- tokenization
- escaping special characters
- prefix-only matching using `*`

---

## 12. Sequence Diagram (Text-based)

### A) Write path (Create/Update/Delete) + async indexing (Outbox)

```
Client
  |
  |  POST/PUT/DELETE /products
  v
ProductSearchController
  |
  |  calls
  v
ProductWriteService
  |
  |  (Tx) save Product via ProductJpaRepository
  |  (Tx) insert OutboxEvent via OutboxRepository
  v
Relational DB
  |
  |  commit
  v
(Product write returns success)

--- async ---

Scheduler
  |
  |  @Scheduled tick
  v
OutboxIndexerJob
  |
  |  find unprocessed events
  v
OutboxRepository
  |
  |  for each event:
  |    - if DELETE: remove from Redis
  |    - else: load product from DB
  v
ProductJpaRepository
  |
  |  map to RedisProductEntity
  v
ProductRedisRepository
  |
  |  mark event processed (processedAt)
  v
Relational DB (Outbox row updated)
```

### B) Read path (Search / Autocomplete)

```
Client
  |
  |  GET /search?q=...
  v
ProductSearchController
  |
  |  calls
  v
ProductCacheSerivice
  |
  |  search Redis first
  v
Redis (RediSearch index)
  |
  |  [cache hit] results
  v
Client

  OR

  |  [cache miss]
  v
Relational DB (searchByNameNative)
  |
  |  map & write-through into Redis
  v
Redis
  |
  v
Client
```

---



## 12. Scaling Considerations (1M Records)

This design supports:
- Millions of indexed documents
- Thousands of concurrent reads
- Minimal database load

Optimizations include:
- Read-heavy traffic routed to Redis
- Batched outbox processing
- Horizontal scaling of indexer jobs

---

## 13. Failure Scenarios & Recovery

### Redis Down
- Writes still succeed (DB + Outbox)
- Indexing resumes when Redis is back

### Indexer Failure
- Outbox events remain unprocessed
- Safe to retry

### Partial Failures
- No data loss
- Eventual consistency guaranteed

---

## 14. Why This Architecture Matters

This project demonstrates:
- How **real companies** build search systems
- How to safely integrate Redis
- How to think beyond "Redis as cache"
- How to design for scale from day one

---

## 15. Summary

**Purpose**:
Build a realistic, scalable search & autocomplete system using Redis

**What it does**:
- Writes data safely to DB
- Indexes data asynchronously
- Serves ultra-fast search queries

**How it achieves this**:
- Redis Stack + Redis OM
- Outbox Pattern
- Clean separation of read/write paths

This documentation is intended to be your **mental reset button** when you return to this project in the future.

---

## 16. Future Enhancements (Optional)

- Fuzzy search
- Ranking & scoring
- Redis Streams instead of polling
- Metrics & observability
- Load testing with 1M+ records

---

End of documentation.

