package com.apiatlas.repository;

import com.apiatlas.model.ApiRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiRelationshipRepository extends JpaRepository<ApiRelationship, Long> {
    Optional<ApiRelationship> findBySourceEndpointIdAndTargetEndpointIdAndType(Long source, Long target, String type);
}
