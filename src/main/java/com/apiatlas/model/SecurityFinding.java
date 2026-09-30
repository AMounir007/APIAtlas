package com.apiatlas.model;

import com.apiatlas.model.Enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "security_finding")
@Getter
@Setter
@NoArgsConstructor
public class SecurityFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long endpointId;

    @Column(nullable = false, length = 1100)
    private String endpointLabel;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(length = 60)
    private String owasp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant detectedAt;

    private boolean resolved;
}
