# API Atlas
**Discover. Map. Document.**

API Atlas is an AI-powered enterprise platform that automatically discovers, maps, documents, analyzes, and exports APIs from web and mobile applications without requiring source code access.

## Capabilities
- **Discovery**: Playwright web crawler (Chromium, Chrome, Edge, Firefox, WebKit), Appium mobile explorer (Android/iOS) and a mitmproxy addon for traffic interception.
- **Protocols**: REST, GraphQL, SOAP, gRPC, WebSocket and SSE detection.
- **Catalog**: normalized endpoints (`/users/{id}`), versions, modules/services, duplicates, deprecated/unused/third-party detection, per-endpoint history and statistics.
- **AI analysis**: business purpose, descriptions, category, tags and test recommendations (OpenAI-compatible LLM, heuristic fallback).
- **Security analysis**: missing authentication, sensitive data exposure, weak headers, token leakage, broken object-level access, insecure transport - mapped to OWASP API Security 2023.
- **Exports**: OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF and a generated REST Assured + TestNG project.
- **Dashboard**: React + Material UI (inventory, explorer, discovery, service map, traffic analytics, security findings, Swagger viewer, export center).
- **Operations**: PostgreSQL + Flyway, Kafka, Redis, Prometheus/Grafana, Docker, Kubernetes, GitHub Actions.

## Status
All nine phases are implemented in source. The code has been statically checked in the IDE only; it has **not yet been built or run end to end** - see [docs/ROADMAP.md](docs/ROADMAP.md#verification-status). Run `mvn verify` and `npm run build` first.

## Quick start
```
cp .env.example .env         # set every change-me value
docker compose up -d --build # dashboard: http://localhost:3000, API: http://localhost:8080
```
Local development: `docker compose up -d postgres redis kafka`, `mvn spring-boot:run`, then `cd frontend && npm install && npm run dev`.
Details in [docs/INSTALLATION.md](docs/INSTALLATION.md).

## Project layout
```
src/main/java/com/apiatlas
  config  controller  service  repository  model  dto
  crawler  analyzer  interceptor  exporter  security  ai  utility
src/main/resources        application.yml, db/migration, qa-template (generated test project)
frontend/                 React + MUI dashboard (Vite)
docker/                   mitmproxy addon, Prometheus and Grafana config
k8s/                      Kubernetes manifests
.github/workflows/ci.yml  build, test, docker, deploy
docs/                     documentation
```

## Documentation
| Document | Content |
|---|---|
| [ARCHITECTURE](docs/ARCHITECTURE.md) | architecture, component, sequence and ER diagrams |
| [INSTALLATION](docs/INSTALLATION.md) | Docker, local, Kubernetes, CI/CD, mobile setup |
| [USER_GUIDE](docs/USER_GUIDE.md) | using the dashboard, exports, generated tests |
| [API_REFERENCE](docs/API_REFERENCE.md) | REST endpoints and Kafka topics |
| [DATABASE](docs/DATABASE.md) | schema and ER diagram |
| [CONFIGURATION](docs/CONFIGURATION.md) | `apiatlas.*` properties |
| [SECURITY](docs/SECURITY.md) | controls, deployment checklist, limitations |
| [ROADMAP](docs/ROADMAP.md) | phases and verification status |

## Responsible use
Only discover APIs of applications you own or are authorized to test. Captured traffic can contain personal data; review [docs/SECURITY.md](docs/SECURITY.md) before production use.

## License
Proprietary (update as needed).
