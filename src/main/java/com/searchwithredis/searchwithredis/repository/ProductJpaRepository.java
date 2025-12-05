package com.searchwithredis.searchwithredis.repository;

import com.searchwithredis.searchwithredis.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM products
                WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))
                """,
            nativeQuery = true
    )
    Page<Products> searchByNameNative(@Param("name") String name, Pageable pageable);

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
