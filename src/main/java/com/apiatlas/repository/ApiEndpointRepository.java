package com.apiatlas.repository;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.Enums.EndpointStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long>, JpaSpecificationExecutor<ApiEndpoint> {
    Optional<ApiEndpoint> findByMethodAndServiceAndPath(String method, String service, String path);

    long countByStatus(EndpointStatus status);

    long countByThirdPartyTrue();

    List<ApiEndpoint> findByStatusAndLastSeenBefore(EndpointStatus status, Instant before);

    List<ApiEndpoint> findTop100ByDescriptionIsNull();

    @Query("select e.authType, count(e) from ApiEndpoint e group by e.authType")
    List<Object[]> countByAuthType();

    @Query("select e.protocol, count(e) from ApiEndpoint e group by e.protocol")
    List<Object[]> countByProtocol();

    @Query("select e.service, count(e) from ApiEndpoint e group by e.service order by count(e) desc")
    List<Object[]> countByService();
}
