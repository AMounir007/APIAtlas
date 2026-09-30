package com.apiatlas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "api_statistics")
@Getter
@Setter
@NoArgsConstructor
public class ApiStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long endpointId;

    private long callCount;
    private long errorCount;
    private double avgResponseMs;
    private long maxResponseMs;

    @Column(nullable = false)
    private Instant updatedAt;
}
