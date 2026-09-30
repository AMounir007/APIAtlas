package com.apiatlas.repository;

import com.apiatlas.model.ApiStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiStatisticsRepository extends JpaRepository<ApiStatistics, Long> {
    Optional<ApiStatistics> findByEndpointId(Long endpointId);

    List<ApiStatistics> findTop10ByOrderByCallCountDesc();

    List<ApiStatistics> findTop10ByOrderByAvgResponseMsDesc();

    List<ApiStatistics> findTop10ByOrderByErrorCountDesc();
}
