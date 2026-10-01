# API Atlas – Security Gap Review

Date: 2026-10-01. Static review plus a dependency CVE lookup. Nothing was compiled, run or penetration-tested.
This extends `docs/CODE_REVIEW.md` (findings S1–S10 there still apply and are not repeated here).

## Additionally reviewed
`docker-compose.yml`, `Dockerfile`, `frontend/nginx.conf`, `k8s/config.yaml`, `k8s/secret.example.yaml`, `k8s/deployment.yaml`, `MobileExplorer`, `StartDiscoveryRequest`, `.gitignore`, `.env.example`, `scripts/push-to-github.ps1`, a repo-wide search for committed secrets (none found by pattern), and a CVE lookup for the direct Maven dependencies.

## Not covered
Transitive dependencies (only direct dependencies were looked up; Spring Boot 3.3.4 returned no CVEs, but that lookup does not cover everything), `k8s/service.yaml`, the React pages, Flyway migrations, `.github/workflows` (my file search could not list the folder), and any runtime or penetration testing.

## Immediate action (outside the code)
| Sev | Gap | Action |
|---|---|---|
| **Critical** | Two GitHub personal access tokens were pasted into the chat earlier. Treat them as compromised. | Revoke both at GitHub → Settings → Developer settings → Personal access tokens. Check the repo's Security → Audit log for unexpected activity. |
| Medium | `.idea/` is in `.gitignore` but is already committed, so it still ships (visible in the repo screenshot). It can hold data-source names, paths and usernames. | `git rm -r --cached .idea` and commit. |

## New findings

### High
| # | Gap | Where | Fix |
|---|---|---|---|
| N1 | **Mobile discovery accepts arbitrary Appium capabilities and an arbitrary app target.** `req.capabilities()` is passed straight to the driver, and `target` becomes `appium:app`, which Appium can download from a URL or read from a path on the Appium host. An authenticated user can make the Appium server fetch internal URLs (SSRF) or load files from its disk, and can set capabilities such as custom driver or chromedriver paths. This is the mobile equivalent of S1. | `MobileExplorer.androidOptions/iosOptions/common` | Allow-list capability names. Accept only an allow-listed app source (uploaded file or a known package id). Reject `http(s)://`, `file:` and path traversal in `target`. |
| N2 | **The crawler's extra headers are sent to every origin.** `ctx.setExtraHTTPHeaders(req.headers())` applies to all requests the page makes, including third-party CDNs, analytics and redirects. A supplied `Authorization` header leaks to those hosts. | `WebCrawler.crawl` | Attach headers only to requests whose origin equals the start origin, using `ctx.route(...)`. |
| N3 | **`mitmproxy` is published on `0.0.0.0:8081` with no authentication.** This is an open proxy on the host. Anyone who can reach it can route traffic through it, and its CA material is kept in the `mitmdata` volume. | `docker-compose.yml` | Bind to `127.0.0.1:8081:8081`, add `--proxyauth`, or do not publish the port. |
| N4 | **The browser and the application share one container.** Chromium/Firefox visit untrusted sites in the same process space as the app, which has the DB password, JWT secret and ingest token in its environment. A browser exploit gives access to all of them. | `Dockerfile`, `WebCrawler` | Move the crawler to a separate, network-restricted worker with no application secrets. At minimum, block egress to cluster and metadata addresses with a NetworkPolicy. |

### Medium
| # | Gap | Where | Fix |
|---|---|---|---|
| N5 | **Vulnerable dependency: `io.appium:java-client` 9.3.0 (CVE-2026-43910, High).** With `directConnect` enabled, a malicious Appium server can redirect session traffic to an arbitrary host (internal ranges and cloud metadata included). User-supplied capabilities (N1) make enabling it reachable. | `pom.xml` | Upgrade to 10.1.1 or later (major version, so re-test `MobileExplorer`). Do not allow `directConnect` in capabilities. |
| N6 | **Vulnerable dependency: `org.apache.poi:poi-ooxml` 5.3.0 (CVE-2025-31672, Medium).** Duplicate zip entries in OOXML input. API Atlas only writes workbooks, so exposure is low. | `pom.xml` | Upgrade to 5.4.0 or later. |
| N7 | **Kafka and Redis are unauthenticated and unencrypted.** Kafka uses `PLAINTEXT` and Redis has no password. Anyone with network access to Kafka can publish to `apiatlas.traffic.captured`, which bypasses the ingest token and poisons the catalog and findings. | `docker-compose.yml`, `k8s/config.yaml` | Enable SASL/TLS on Kafka, set a Redis password, and add NetworkPolicies that allow only the app. |
| N8 | **Prometheus (9090) and Grafana (3001) are published to all interfaces.** Prometheus has no authentication, and `/actuator/prometheus` is also `permitAll` (S6). | `docker-compose.yml` | Bind to `127.0.0.1` or put them behind the reverse proxy with authentication. |
| N9 | **Captured data is stored without retention or encryption.** Request and response samples (masked, but PII can remain) are kept indefinitely. Any authenticated user can export everything, because there is a single `ADMIN` role. | DB, `ExportController` | Add a retention job and a configurable "do not store bodies" mode. Enable database encryption at rest. Introduce roles (viewer/analyst/admin). |
| N10 | **The frontend nginx config is minimal.** There is no `Content-Security-Policy`, no HSTS and no TLS. The server also has no `client_max_body_size` and no rate limiting, and it forwards no `X-Forwarded-*` headers. The nginx image runs as root by default. The JWT is held in `sessionStorage`, so an XSS bug would expose it. A search found no `innerHTML`, `dangerouslySetInnerHTML` or `eval` in `frontend/src`, which helps. | `frontend/nginx.conf`, `frontend/Dockerfile` | Add a strict CSP, HSTS (when behind TLS), a body size limit and `limit_req` on `/api/auth/`. Use `nginxinc/nginx-unprivileged`. |
| N11 | **Compose publishes the API directly on `8080` as well as through the frontend.** This bypasses any controls added in nginx. | `docker-compose.yml` | Publish only the frontend port. |
| N12 | **Kubernetes hardening is missing.** No `NetworkPolicy` exists in `k8s/`. No `readOnlyRootFilesystem` (O3), `automountServiceAccountToken` is not disabled, and secrets are injected as environment variables. Pod egress is unrestricted, which compounds S1 and N1. | `k8s/` | Add default-deny NetworkPolicies with explicit egress rules. Disable the service-account token mount. Consider mounting secrets as files or using an external secret store. |

### Low
| # | Gap | Where | Fix |
|---|---|---|---|
| N13 | **Log injection.** `log.warn("Failed to ingest {} {}", t.method(), t.url())` writes attacker-controlled values to the log. | `TrafficIngestionService` | Strip control characters before logging. |
| N14 | **`/api/auth/**` is `permitAll`.** Any future endpoint added under that path is public by default. | `SecurityConfig` | Permit only `/api/auth/login`. |
| N15 | **Unpinned base images.** The Dockerfile and compose use floating tags (`postgres:16-alpine`, `redis:7-alpine`, `maven:3.9...`, `eclipse-temurin:21-jre-jammy`). | `Dockerfile`, `docker-compose.yml` | Pin by digest and rebuild regularly. Scan images in CI (Trivy/Grype). |
| N16 | **Supply chain in CI.** The release workflow uses `googleapis/release-please-action@v4` by tag, with `contents: write` and `pull-requests: write`. | `.github/workflows/release.yml` | Pin third-party actions to a commit SHA. Add Dependabot for actions and Maven, plus dependency review and CodeQL. |
| N17 | **`.env.example` default is risky.** It says "Empty = allow all" for `ATLAS_ALLOWED_HOSTS`, which documents the SSRF-prone default (S1). | `.env.example` | Make the value required, or default to a deny-all policy. |
| N18 | **No Content-Type / size validation on ingest.** The JSON payload is accepted as-is, with no per-field length limits beyond truncation in the service (see S7). | `TrafficController` | Add bean validation and a request size cap. |

## Dependency CVE lookup (direct dependencies)
| Dependency | Version | Result |
|---|---|---|
| `io.appium:java-client` | 9.3.0 | CVE-2026-43910 (High) → 10.1.1+ |
| `org.apache.poi:poi-ooxml` | 5.3.0 | CVE-2025-31672 (Medium) → 5.4.0+ |
| `spring-boot-starter-parent` | 3.3.4 | none reported |
| `jjwt-api`, `springdoc-openapi-starter-webmvc-ui`, `playwright`, `swagger-parser`, `openpdf`, `commons-csv`, `testcontainers`, `rest-assured` | as in `pom.xml` | none reported |

## Recommended order
1. **Now:** revoke the two exposed tokens, and untrack `.idea/`.
2. **Before any shared deployment:** S1, S3 (from `CODE_REVIEW.md`), then N1, N2, N3 and N11.
3. **Dependencies:** N5 (appium 10.1.1) and N6 (poi 5.4.0), then run the unit and integration tests.
4. **Platform:** N7, N8, N12, then N4 (separate crawler worker).
5. **Hardening:** N9, N10, then the low items N13–N18.
6. **Pipeline:** add CodeQL, Dependabot and image scanning to GitHub Actions.
