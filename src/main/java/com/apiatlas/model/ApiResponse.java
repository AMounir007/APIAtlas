package com.apiatlas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "api_response")
@Getter
@Setter
@NoArgsConstructor
public class ApiResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long endpointId;

    private Long requestId;

    @Column(nullable = false)
    private int statusCode;

    @Column(columnDefinition = "text")
    private String headers;
    @Column(columnDefinition = "text")
    private String body;

    private String contentType;

    @Column(nullable = false)
    private long responseTimeMs;

    @Column(nullable = false)
    private Instant capturedAt;
}
