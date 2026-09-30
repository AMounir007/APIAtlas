package com.apiatlas.repository;

import com.apiatlas.model.DiscoverySession;
import com.apiatlas.model.Enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DiscoverySessionRepository extends JpaRepository<DiscoverySession, Long> {
    long countByStatus(SessionStatus status);

    List<DiscoverySession> findAllByOrderByStartedAtDesc();

    @Modifying
    @Transactional
    @Query("update DiscoverySession s set s.endpointsFound = s.endpointsFound + 1 where s.id = :id")
    void incrementEndpointsFound(@Param("id") Long id);
}
