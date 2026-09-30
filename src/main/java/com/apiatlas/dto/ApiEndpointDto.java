package com.apiatlas.dto;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.model.Enums.Protocol;

import java.time.Instant;

/** Catalog list view of an endpoint (without bulky samples). */
public record ApiEndpointDto(
        Long id, String name, String module, String service, String url, String method, String version,
        Protocol protocol, AuthType authType, EndpointStatus status, boolean thirdParty,
        String description, long hitCount, Instant firstSeen, Instant lastSeen) {

    public static ApiEndpointDto from(ApiEndpoint e) {
        return new ApiEndpointDto(e.getId(), e.getName(), e.getModule(), e.getService(), e.getUrl(), e.getMethod(),
                e.getVersion(), e.getProtocol(), e.getAuthType(), e.getStatus(), e.isThirdParty(),
                e.getDescription(), e.getHitCount(), e.getFirstSeen(), e.getLastSeen());
    }
}
