# API Atlas - Installation Guide

## 1. Prerequisites
| Tool | Version | Needed for |
|---|---|---|
| JDK | 21 | backend |
| Maven | 3.9+ | backend build |
| Node.js | 20+ | frontend |
| Docker + Docker Compose | recent | full stack |
| PostgreSQL 16, Redis 7, Kafka 3.8 | | provided by `docker-compose.yml` |
| Appium 2 + Android SDK / Xcode | | mobile discovery only |

## 2. Quick start with Docker Compose
```
cp .env.example .env        # then edit every "change-me" value
docker compose up -d --build
```
| Service | URL |
|---|---|
| Dashboard | http://localhost:3000 |
| Backend API / Swagger UI | http://localhost:8080 / http://localhost:8080/swagger-ui.html |
| mitmproxy | localhost:8081 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (user `admin`, password `GRAFANA_PASSWORD`) |

Sign in with `ATLAS_ADMIN_USER` / `ATLAS_ADMIN_PASSWORD` from `.env`.
The compose file refuses to start when `DB_PASSWORD`, `JWT_SECRET`, `MITM_INGEST_TOKEN`, `ATLAS_ADMIN_PASSWORD` or `GRAFANA_PASSWORD` are unset.

## 3. Local development
```
# infrastructure only
docker compose up -d postgres redis kafka

# backend (Flyway creates the schema on start)
mvn spring-boot:run

# frontend (proxies /api to localhost:8080)
cd frontend
npm install
npm run dev            # http://localhost:3000
```
Default local credentials (`admin` / `admin123`) are for development only and produce a warning in the log.
Set `ATLAS_ADMIN_PASSWORD` and `JWT_SECRET` for any shared environment.

The first web discovery downloads the Playwright browsers. To do it ahead of time:
`mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"`

## 4. Kubernetes
```
kubectl apply -f k8s/config.yaml
kubectl -n api-atlas create secret generic api-atlas-secrets \
  --from-literal=DB_PASSWORD=... --from-literal=JWT_SECRET=... \
  --from-literal=MITM_INGEST_TOKEN=... --from-literal=ATLAS_ADMIN_PASSWORD=...
kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml
```
PostgreSQL, Redis and Kafka are expected as managed services or separate releases (hosts are set in `k8s/config.yaml`).
Adjust the image name in `k8s/deployment.yaml` and the host in `k8s/service.yaml`.

## 5. CI/CD (GitHub Actions)
`.github/workflows/ci.yml` runs: build -> unit tests -> integration tests -> frontend build -> Docker build/push (GHCR, on `main`) -> deploy.
Deployment is skipped unless the repository secret `KUBE_CONFIG` (base64 kubeconfig) is set. Create a `production` environment with required reviewers for manual approval.

## 6. Configuration
All settings live under `apiatlas.*` (see [CONFIGURATION.md](CONFIGURATION.md)) and can be overridden with environment variables (`DB_URL`, `JWT_SECRET`, `MITM_INGEST_TOKEN`, `AI_ENABLED`, `AI_API_KEY`, ...).

## 7. Mobile setup (mitmproxy)
1. Start Appium (`appium`) and a device/emulator.
2. Start a discovery session (type MOBILE) and note its id.
3. Run mitmproxy with `ATLAS_SESSION_ID=<id>`; point the device Wi-Fi proxy to `<host>:8081` and install the mitmproxy CA certificate on the device (http://mitm.it).
4. For Android 7+ apps that do not trust user CAs, use a debuggable build / emulator with a system CA, or a certificate-pinning bypass in your own test builds.
