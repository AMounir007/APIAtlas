package com.apiatlas.repository;

import com.apiatlas.model.SecurityFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SecurityFindingRepository extends JpaRepository<SecurityFinding, Long> {
    Optional<SecurityFinding> findByEndpointIdAndCategory(Long endpointId, String category);

    List<SecurityFinding> findByResolvedFalseOrderByDetectedAtDesc();

    List<SecurityFinding> findByEndpointId(Long endpointId);

    long countByResolvedFalse();

    @Query("select f.severity, count(f) from SecurityFinding f where f.resolved = false group by f.severity")
    List<Object[]> countBySeverity();

    @Query("select f.category, count(f) from SecurityFinding f where f.resolved = false group by f.category")
    List<Object[]> countByCategory();
}
