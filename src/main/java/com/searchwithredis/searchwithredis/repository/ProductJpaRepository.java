package com.searchwithredis.searchwithredis.repository;

import com.searchwithredis.searchwithredis.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductJpaRepository extends JpaRepository<Products ,Long > {
    @Query(
            value = """
              SELECT *
              FROM products
              WHERE name ILIKE CONCAT('%', :q, '%')
              ORDER BY id
              LIMIT :limit
              """,
            nativeQuery = true
    )
    List<Products> searchByNameNative(@Param("q") String q,
                                      @Param("limit") int limit);

    @Query(value = """
        SELECT *
        FROM products
        WHERE id > :lastId
        ORDER BY id
        LIMIT :limit
        """, nativeQuery = true)
    List<Products> fetchNextBatch(@Param("lastId") long lastId,
                                  @Param("limit") int limit);
}
