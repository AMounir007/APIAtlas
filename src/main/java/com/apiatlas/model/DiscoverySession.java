package com.apiatlas.model;

import com.apiatlas.model.Enums.SessionStatus;
import com.apiatlas.model.Enums.SessionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "discovery_session")
@Getter
@Setter
@NoArgsConstructor
public class DiscoverySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2048)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(length = 30)
    private String browser;

    @Column(length = 30)
    private String platform;

    private int pagesVisited;
    private int endpointsFound;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;
}
