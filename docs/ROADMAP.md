# API Atlas Roadmap

Status legend: **Implemented** = code and docs are in the repository. Nothing has been compiled or run in the authoring
environment yet (no terminal was available), so run `mvn verify` and `npm run build` before relying on it - see
"Verification status" below.

| Phase | Scope | Status |
|---|---|---|
| 1 | Foundation: Spring Boot, `AtlasProperties`, `application.yml`, architecture docs | Implemented |
| 2 | Domain entities, repositories, Flyway migration `V1__init_schema.sql` | Implemented |
| 3 | Discovery: Playwright crawler, Appium explorer, mitmproxy addon, Kafka ingestion, ingest API | Implemented |
| 4 | Catalog: path normalization, dedup, versioning, service/module mapping, search, service map | Implemented |
| 5 | AI analysis (LLM + heuristic fallback): descriptions, category, tags, test recommendations | Implemented |
| 6 | Exporters: OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF, REST Assured/TestNG project | Implemented |
| 7 | Security engine: missing auth, sensitive data, headers, token leakage, BOLA, transport; security report | Implemented |
| 8 | React + Material UI dashboard (inventory, explorer, discovery, service map, traffic, security, swagger, exports) | Implemented |
| 9 | Dockerfiles, docker-compose, Kubernetes manifests, GitHub Actions, Prometheus/Grafana | Implemented |

## Verification status
| Item | State |
|---|---|
| Static IDE inspection of backend sources | no compile errors reported |
| `mvn verify` (unit tests: `UtilityTest`, `SecurityAnalyzerTest`, `ApiAnalyzerTest`) | not run yet |
| Application start against PostgreSQL (Flyway + Hibernate `validate`) | not run yet |
| `npm install && npm run build` | not run yet |
| Docker image build, Kubernetes deployment | not run yet |
| Playwright / Appium discovery against a real target | not run yet |

## Next steps
- Integration tests with Testcontainers (`*IT.java`, picked up by failsafe)
- User management and roles (currently one admin from environment variables)
- Schema inference (JSON Schema in OpenAPI components), GraphQL/gRPC/SOAP-specific documentation
- Retention jobs for captured payloads
- Grafana dashboards, alerting rules
