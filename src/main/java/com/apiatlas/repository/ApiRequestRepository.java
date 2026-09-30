package com.apiatlas.repository;

import com.apiatlas.model.ApiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {
    long countByEndpointId(Long endpointId);

    Optional<ApiRequest> findFirstByEndpointIdOrderByCapturedAtDesc(Long endpointId);
}
