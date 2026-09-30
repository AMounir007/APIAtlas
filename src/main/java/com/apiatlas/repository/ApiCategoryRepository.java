package com.apiatlas.repository;

import com.apiatlas.model.ApiCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiCategoryRepository extends JpaRepository<ApiCategory, Long> {
    Optional<ApiCategory> findByName(String name);
}
