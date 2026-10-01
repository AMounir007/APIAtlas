# API Atlas – Release Notes

## v1.0.0 (2026-10-01)

**Discover. Map. Document.**

AI-powered API discovery and documentation platform for web and mobile applications, with no source code access required.

### Platform
- Spring Boot 3.3.4 backend on Java 21, with Spring Security and JWT authentication.
- PostgreSQL schema managed by Flyway. Entities and repositories cover endpoints, requests, responses, categories, tags, relationships, statistics, security findings and discovery sessions.
- Kafka messaging and Redis caching.
- React frontend with pages for Dashboard, Inventory, Explorer, Discovery, Traffic, Security, Service Map, Swagger Viewer and Exports.

### Discovery
- Web discovery with Playwright.
- Mobile discovery with Appium.
- Traffic capture through MITMProxy (`docker/mitm/atlas_addon.py`), ingested through `/api/traffic/ingest` and protected by the `X-Ingest-Token` header.
- Protocols: REST, GraphQL, SOAP, gRPC, WebSocket and SSE.
- Paths are normalised, for example `/api/v1/users/{id}`, so repeated hits map to one endpoint.

### Analysis and Security
- Detection of duplicate, deprecated, unused and third-party APIs, plus API versions.
- Authentication detection: JWT, OAuth2, Basic and anonymous.
- Security findings: token leakage, sensitive data exposure, weak security headers and missing authentication.
- Sensitive values are masked in stored samples and in exports.

### Exports
- Swagger/OpenAPI, Postman, Excel, CSV, JSON, HTML and PDF.
- Generated REST Assured/TestNG/Maven test project.
- Security report.

### DevOps and Docs
- Dockerfile, docker-compose (including Prometheus and Grafana), and Kubernetes manifests.
- GitHub Actions CI/CD pipeline.
- Documentation in `docs/`: architecture, API reference, configuration, database, installation, security, user guide and roadmap.

### Testing
- Unit tests for the analyzers and utilities.
- `IngestionFlowIT`, a Testcontainers integration test run by failsafe (`mvn verify`) that covers Flyway against the JPA entities, ingestion, masking, findings and exports.

### Changes in this release
- `IngestionFlowIT` hardening:
  - Null checks on the login, ingest and swagger responses.
  - The `get` helper asserts HTTP 200 and a non-null body.
  - The PDF export must start with `%PDF` and the Excel export with `PK`.

### Known follow-ups
- Isolate database state between integration tests.
- Split the large ingestion test into smaller ones.
- Parse security findings instead of matching raw JSON strings.
- Confirm ingestion is synchronous, or add Awaitility.
