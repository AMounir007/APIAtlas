package com.apiatlas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_tag")
@Getter
@Setter
@NoArgsConstructor
public class ApiTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long endpointId;

    @Column(nullable = false, length = 80)
    private String tag;
}
