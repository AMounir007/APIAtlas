package com.apiatlas.model;

import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.model.Enums.Protocol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A unique API operation (method + service + normalized path) in the catalog. */
@Entity
@Table(name = "api_endpoint")
@Getter
@Setter
@NoArgsConstructor
public class ApiEndpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(length = 120)
    private String module;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 1000)
    private String path;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(length = 20)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Protocol protocol = Protocol.REST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthType authType = AuthType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EndpointStatus status = EndpointStatus.ACTIVE;

    private boolean thirdParty;

    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String businessPurpose;
    @Column(columnDefinition = "text")
    private String requestDescription;
    @Column(columnDefinition = "text")
    private String responseDescription;
    @Column(columnDefinition = "text")
    private String testRecommendations;
    @Column(columnDefinition = "text")
    private String sampleRequest;
    @Column(columnDefinition = "text")
    private String sampleResponse;

    private long hitCount;

    @Column(nullable = false)
    private Instant firstSeen;

    @Column(nullable = false)
    private Instant lastSeen;
}
