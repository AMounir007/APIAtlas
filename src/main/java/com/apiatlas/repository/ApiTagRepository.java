package com.apiatlas.repository;

import com.apiatlas.model.ApiTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiTagRepository extends JpaRepository<ApiTag, Long> {
    List<ApiTag> findByEndpointId(Long endpointId);

    boolean existsByEndpointIdAndTag(Long endpointId, String tag);
}
