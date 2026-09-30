package com.apiatlas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_relationship")
@Getter
@Setter
@NoArgsConstructor
public class ApiRelationship {
    public static final String CALLED_AFTER = "CALLED_AFTER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceEndpointId;

    @Column(nullable = false)
    private Long targetEndpointId;

    @Column(nullable = false, length = 40)
    private String type = CALLED_AFTER;

    @Column(nullable = false)
    private long weight = 1;
}
