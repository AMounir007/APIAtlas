package com.apiatlas.repository;

import com.apiatlas.model.ApiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiResponseRepository extends JpaRepository<ApiResponse, Long> {
    List<ApiResponse> findTop50ByEndpointIdOrderByCapturedAtDesc(Long endpointId);

    Optional<ApiResponse> findFirstByEndpointIdOrderByCapturedAtDesc(Long endpointId);
}
