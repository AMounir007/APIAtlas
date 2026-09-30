package com.apiatlas.dto;

import java.util.List;

/** Service-level dependency graph. Edge weight is the number of observed "called after" transitions. */
public record DependencyGraphDto(List<Node> nodes, List<Edge> edges) {
    public record Node(String id, long endpoints, boolean thirdParty) {}

    public record Edge(String source, String target, long weight) {}
}
