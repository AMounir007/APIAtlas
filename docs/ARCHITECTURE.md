# API Atlas — System Architecture

> **Discover. Map. Document.**
> API Atlas is an AI-powered enterprise platform that automatically discovers, maps, documents, analyzes, and exports APIs from web and mobile applications without requiring source code access.

## 1. High-Level Architecture

```mermaid
flowchart LR
    subgraph Clients
        UI[React + MUI Dashboard]
        CLI[CI / REST Clients]
    end

    subgraph Discovery["Discovery Layer"]
        PW[Playwright Web Crawler<br/>Chromium / Edge / Firefox / WebKit]
        AP[Appium Mobile Explorer<br/>Android / iOS]
        MITM[MITMProxy + atlas_addon.py]
    end

    subgraph Core["API Atlas Backend (Spring Boot 3, Java 21)"]
        GW[REST Controllers + JWT Security]
        DS[Discovery Service]
        ING[Traffic Ingestion]
        NORM[Endpoint Normalizer<br/>path templating, protocol detection]
        AN[Analyzers<br/>auth, duplicates, versions, security]
        AI[AI Enrichment Service]
        EXP[Exporters<br/>OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF, TestNG]
    end

    subgraph Infra
        K[(Kafka)]
        PG[(PostgreSQL)]
        R[(Redis)]
        LLM[[LLM Provider]]
        PROM[Prometheus] --> GRAF[Grafana]
    end

    UI --> GW
    CLI --> GW
    GW --> DS
    DS --> PW
    DS --> AP
    PW -- proxied traffic --> MITM
    AP -- device proxy --> MITM
    PW -- in-process capture --> ING
    MITM -- HTTP ingest --> ING
    ING --> K
    K --> NORM --> PG
    NORM --> K
    K --> AN --> PG
    K --> AI --> LLM
    AI --> PG
    GW --> EXP --> PG
    GW --> R
    Core --> PROM
```

## 2. Component Responsibilities

| Package | Responsibility |
|---|---|
| `config` | Spring configuration: Kafka topics, Redis cache, OpenAPI, async executors, typed properties |
| `controller` | REST API (`/api/apis`, `/api/statistics`, `/api/security`, `/api/export/*`, `/api/discovery/*`) |
| `service` | Business logic: catalog, statistics, discovery orchestration, ingestion |
| `repository` | Spring Data JPA repositories |
| `model` | JPA entities |
| `dto` | API contracts and Kafka event payloads |
| `crawler` | Playwright web crawler and Appium mobile explorer |
| `interceptor` | Traffic capture: Playwright listeners, MITMProxy ingestion, protocol detection |
| `analyzer` | Auth detection, duplicate/version/deprecation detection, security (OWASP API Top 10) |
| `exporter` | OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF, REST Assured/TestNG generator |
| `security` | JWT authentication, filter chain, user management |
| `ai` | LLM client, prompt templates, endpoint enrichment |
| `utility` | Path templating, JSON schema inference, hashing, masking |

## 3. Discovery Sequence

```mermaid
sequenceDiagram
    actor U as QA Engineer
    participant UI as Dashboard
    participant API as DiscoveryController
    participant DS as DiscoveryService
    participant C as Crawler (Playwright/Appium)
    participant M as MITMProxy
    participant K as Kafka
    participant N as Normalizer
    participant A as Analyzers + AI
    participant DB as PostgreSQL

    U->>UI: Start discovery (target, browser)
    UI->>API: POST /api/discovery/start
    API->>DS: start(request)
    DS->>DB: create discovery_session (RUNNING)
    DS-->>C: async crawl
    loop each page / screen
        C->>C: navigate, click, submit forms
        C->>M: traffic (mobile) / listeners (web)
        M->>API: POST /api/ingest/traffic
        C->>K: captured-traffic event
    end
    K->>N: consume
    N->>DB: upsert api_endpoint, save api_request/api_response
    N->>K: endpoint-discovered
    K->>A: consume
    A->>DB: security_finding, auth type, category, AI description
    U->>API: POST /api/discovery/stop
    DS->>DB: session COMPLETED + statistics
```

## 4. ER Diagram

```mermaid
erDiagram
    DISCOVERY_SESSION ||--o{ API_REQUEST : captures
    API_ENDPOINT ||--o{ API_REQUEST : has
    API_REQUEST ||--|| API_RESPONSE : produces
    API_CATEGORY ||--o{ API_ENDPOINT : groups
    API_ENDPOINT }o--o{ API_TAG : tagged
    API_ENDPOINT ||--o{ API_RELATIONSHIP : source
    API_ENDPOINT ||--o{ API_RELATIONSHIP : target
    API_ENDPOINT ||--o{ SECURITY_FINDING : has
    API_ENDPOINT ||--|| API_STATISTICS : aggregates

    API_ENDPOINT {
        uuid id PK
        string name
        string module
        string service
        string host
        string path_template
        string method
        string protocol
        string version
        string auth_type
        string status
        text description
        jsonb ai_documentation
        jsonb request_schema
        jsonb response_schema
        string signature_hash UK
        timestamp first_seen
        timestamp last_seen
    }
    DISCOVERY_SESSION {
        uuid id PK
        string name
        string source_type
        string target
        string status
        timestamp started_at
        timestamp ended_at
    }
    API_REQUEST {
        uuid id PK
        uuid endpoint_id FK
        uuid session_id FK
        string url
        jsonb headers
        jsonb query_params
        jsonb path_params
        text body
        timestamp captured_at
    }
    API_RESPONSE {
        uuid id PK
        uuid request_id FK
        int status_code
        jsonb headers
        text body
        string content_type
        bigint duration_ms
    }
    SECURITY_FINDING {
        uuid id PK
        uuid endpoint_id FK
        string type
        string severity
        string owasp_ref
        text description
        text recommendation
    }
```

## 5. Delivery Phases

| Phase | Scope |
|---|---|
| **1** | Architecture, project structure, `pom.xml`, `application.yml`, application bootstrap |
| 2 | Database schema (Flyway), entities, repositories |
| 3 | Security (JWT), DTOs, catalog services and REST controllers |
| 4 | Traffic interception (Playwright listeners, MITMProxy addon + ingestion, Kafka pipeline) |
| 5 | Web crawler (Playwright) and mobile explorer (Appium) |
| 6 | Analyzers (auth, duplicates, versions, deprecation, security/OWASP) + AI enrichment |
| 7 | Exporters (OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF) + REST Assured/TestNG generator |
| 8 | React + Material UI dashboard |
| 9 | Docker, docker-compose, Kubernetes, GitHub Actions, Prometheus/Grafana |
| 10 | Installation Guide and User Guide |

## 6. Project Structure

```
APIAtlas/
├── pom.xml
├── docs/                         # architecture, installation, user guide
├── mitmproxy/atlas_addon.py      # traffic forwarder (phase 4)
├── frontend/                     # React + MUI (phase 8)
├── deploy/{docker,k8s,monitoring}
├── .github/workflows/ci.yml
└── src/main/java/com/apiatlas/
    ├── ApiAtlasApplication.java
    ├── config/  controller/  service/  repository/  model/  dto/
    ├── crawler/  analyzer/  interceptor/  exporter/
    ├── security/  ai/  utility/
    └── src/main/resources/{application.yml, db/migration}
```
