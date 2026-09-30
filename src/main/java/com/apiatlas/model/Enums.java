package com.apiatlas.model;

/** Shared enumerations of the API Atlas domain model. */
public final class Enums {
    private Enums() {}

    public enum Protocol { REST, GRAPHQL, SOAP, GRPC, WEBSOCKET, SSE }

    public enum AuthType { NONE, BASIC, JWT, OAUTH2, API_KEY }

    public enum EndpointStatus { ACTIVE, DEPRECATED, UNUSED }

    public enum Severity { INFO, LOW, MEDIUM, HIGH, CRITICAL }

    public enum SessionType { WEB, MOBILE }

    public enum SessionStatus { RUNNING, COMPLETED, STOPPED, FAILED }
}
