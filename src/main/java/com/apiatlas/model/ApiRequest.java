package com.apiatlas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "api_request")
@Getter
@Setter
@NoArgsConstructor
public class ApiRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long endpointId;

    /** JSON object of (masked) request headers. */
    @Column(columnDefinition = "text")
    private String headers;
    /** JSON object of query parameters (sensitive values masked). */
    @Column(columnDefinition = "text")
    private String queryParams;
    /** JSON array of cookie names only; values are never stored. */
    @Column(columnDefinition = "text")
    private String cookies;
    @Column(columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private Instant capturedAt;
}
