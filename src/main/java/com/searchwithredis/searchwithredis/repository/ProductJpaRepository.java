package com.searchwithredis.searchwithredis.repository;

import com.searchwithredis.searchwithredis.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Products ,Long > {
}
