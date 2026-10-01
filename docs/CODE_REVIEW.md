# API Atlas – Code Review

Date: 2026-10-01. Scope: static review only. Nothing was compiled, run or tested for this review.

## Coverage

**Read in full:**
- `SecurityConfig`, `JwtService`, `JwtAuthFilter`
- `AuthController`, `TrafficController`, `CatalogController`, `ExportController`, `DiscoveryController`, `GlobalExceptionHandler`
- `TrafficIngestionService`, `DiscoveryService`, `CatalogService`
- `WebCrawler`, `AiClient`, `AiAnalysisService`
- `SecurityAnalyzer`, `Masker`, `TestSuiteGenerator`, `TrafficListener`
- `application.yml`, `k8s/deployment.yaml`, `docker/mitm/atlas_addon.py`, `frontend/src/api.js`

**Partly read:** `HtmlExporter` (escaping only).

**Not reviewed:**
- `MobileExplorer`, `StatisticsService`, `TrafficPublisher`, `KafkaConfig`, `PathNormalizer`
- `ApiAnalyzer`, the other exporters, the JPA models, repositories and DTOs
- Flyway migrations, `Dockerfile`, `docker-compose.yml`, the other k8s files
- The React pages, `.github/workflows` and the tests

## What is done well
- Secrets are masked before persistence.
- The ingest token is compared in constant time (`MessageDigest.isEqual`).
- Login returns a generic error and does not reveal whether the user exists.
- HTML exports are escaped.
- Generated test code comes from fixed templates, so user data is never compiled.
- JSON deserialization is restricted through trusted packages.
- Page size is capped at 200.
- `LIKE` input is escaped.
- Hibernate runs with `ddl-auto: validate`.
- The k8s pod runs as non-root with dropped capabilities.
- The AI prompt treats traffic as data.

## Findings

Severity: **H** = high, **M** = medium, **L** = low.

### Security
| # | Sev | Finding | Location | Recommendation |
|---|---|---|---|---|
| S1 | H | **SSRF in web discovery.** `ATLAS_ALLOWED_HOSTS` is empty by default, so any authenticated user can point the crawler at `localhost`, cluster services or `169.254.169.254`. | `DiscoveryService.validate` | Deny loopback, link-local and private ranges by default. Resolve the DNS name and check the IP. Require an explicit allow-list in `prod`. |
| S2 | H | **Crawler can change data on the target.** It clicks up to 8 buttons per page. The `DESTRUCTIVE` pattern does not cover "Save", "Submit", "Send" or "Update". It is also a substring match, so words that merely contain `pay` or `confirm` are skipped. | `WebCrawler.interact` | Make clicking opt-in. Match whole words. Add a safe mode that observes only, and document that discovery should run against test environments. |
| S3 | H | **Insecure defaults can reach production.** The defaults are `admin123`, a JWT secret in `application.yml` and the ingest token `change-me`. Only the admin password logs a warning. | `SecurityConfig`, `application.yml` | In the `prod` profile, fail on startup if any of the three equals its default. |
| S4 | M | **Masking runs after truncation.** If the 1 MB cut falls inside a JSON value, the closing quote is missing and the regex does not match, so a secret can be stored unmasked. | `TrafficIngestionService` (`Masker.maskBody(Masker.truncate(...))`) | Mask first, then truncate. |
| S5 | M | **Masking has gaps.** `maskBody` handles flat JSON and form patterns only (no XML, and nested arrays are covered only through the regex). The key list lacks `x-auth-token`, `proxy-authorization`, `key`, `signature`, `otp`, `pin` and `card`. Header masking depends on the config list. PII such as emails and card numbers in response bodies is stored in samples. | `Masker`, `application.yml` | Mask by recursive JSON traversal. Widen the key list. Add PII patterns (PAN with Luhn check) and an option to drop bodies. |
| S6 | M | **Public endpoints.** `/actuator/prometheus`, `/v3/api-docs/**` and `/swagger-ui/**` are `permitAll`. `/api/auth/login` has no rate limit or lockout. `/api/traffic/ingest` has no rate limit. | `SecurityConfig` | Require auth or network restriction for metrics and docs. Add login throttling (for example Bucket4j). |
| S7 | M | **Ingest body is not size-limited.** A batch of 500 items with 1 MB bodies is about 500 MB in memory. | `TrafficController` | Set a request size limit (filter or Tomcat) and validate field sizes. |
| S8 | M | **Data sent to an external LLM.** Masked samples can still contain PII. | `AiClient`, `AiAnalysisService` | Keep `AI_ENABLED=false` by default. Document the data flow. Offer a redaction step. |
| S9 | L | JWT carries no role and the filter grants `ROLE_ADMIN` to any valid token. There is no revocation. | `JwtAuthFilter` | Put roles in claims. Add a token version or deny-list if more users are added. |
| S10 | L | `IllegalStateException` is mapped to 409 with its raw message. Any unexpected `IllegalStateException` would leak internals. | `GlobalExceptionHandler` | Use a dedicated exception type. |

### Correctness and reliability
| # | Sev | Finding | Location | Recommendation |
|---|---|---|---|---|
| C1 | H | **Not safe with 2 replicas.** `k8s/deployment.yaml` runs `replicas: 2`, but the ingestion lock, the `running` session map and `lastEndpointBySession` are per JVM. `POST /discovery/stop` can reach the other pod and return 404. Relationship tracking is split across pods. Concurrent inserts rely on a single retry. | `DiscoveryService`, `TrafficIngestionService`, `deployment.yaml` | Short term: set `replicas: 1`. Longer term: keep session state in the DB or Redis, use a DB advisory lock or upsert (`ON CONFLICT`), and use a shared cancel flag. |
| C2 | M | **Global `ReentrantLock`** serializes all ingestion. The lock is also held while the Kafka publish runs. | `TrafficIngestionService.ingest` | Use per-endpoint upserts. Publish events after the lock is released. |
| C3 | M | **Sessions stay `RUNNING` after a crash or restart.** `shutdown()` does not wait for the pool, and nothing resets orphaned sessions at startup. | `DiscoveryService` | Mark stale `RUNNING` sessions as `FAILED` at startup. Use `awaitTermination`. |
| C4 | M | **Memory leak in `lastEndpointBySession`.** Proxy or Kafka ingestion with a `sessionId` never calls `endSession`. | `TrafficIngestionService` | Use an expiring cache (Caffeine, TTL). |
| C5 | M | **`AiAnalysisService.enrichPending()` runs inline.** It can make up to 5000 sequential LLM calls. `POST /api/apis/analyze-pending` blocks the HTTP thread and the Kafka listener. | `AiAnalysisService`, `CatalogController` | Run it asynchronously with a job status. Add a per-call retry and a rate limit. |
| C6 | M | **Crawler reads whole response bodies** (`r.text()`) before truncation. | `WebCrawler.capture` | Skip bodies above a size limit using `Content-Length`. |
| C7 | M | **Crawler "same origin" compares host only**, not scheme or port. There is also no overall session time limit and no allow-list check on redirects. | `WebCrawler.allowed` | Compare the full origin. Add a time budget. |
| C8 | M | **`ExportController` loads the whole catalog** (`listAll`). | `CatalogService.listAll` | Stream or page large exports. |
| C9 | L | `GlobalExceptionHandler` replaces every `IllegalArgumentException` with "Invalid request". The useful messages from `DiscoveryService.validate` never reach the user. | `GlobalExceptionHandler` | Use a dedicated validation exception whose message is exposed. |
| C10 | L | `running.remove(id)` happens before the final status is saved, which allows a short window of a wrong concurrency count. | `DiscoveryService.run` | Remove after saving the status. |
| C11 | L | Heuristics produce false positives: the card regex has no Luhn check, the SSN regex is loose, and `/legacy` marks an endpoint deprecated. `BROKEN_ACCESS_CONTROL` adds a LOW finding for every ID path. | `SecurityAnalyzer`, `TrafficIngestionService` | Add a Luhn check. Make the BOLA advisory opt-in or group it. |
| C12 | L | `registrableDomain` is naive (`co.uk` is wrong). Third-party status is computed only at creation. | `TrafficIngestionService` | Use the public suffix list. |
| C13 | L | `CatalogController.get` returns the JPA entity directly. | `CatalogController` | Return a DTO. |

### Build and operations
| # | Sev | Finding | Recommendation |
|---|---|---|---|
| O1 | M | `image: ...:latest` with `imagePullPolicy: Always` makes rollouts and rollbacks unreproducible. | Tag images with the release version (this fits the new release workflow). |
| O2 | L | `baseline-on-migrate: true` can silently baseline a non-empty database. | Disable it outside the first rollout. |
| O3 | L | The k8s container has no `readOnlyRootFilesystem`. Debug logging is on by default outside the `prod` profile. | Enable the read-only root filesystem. Set `LOG_LEVEL`. |
| O4 | L | `.idea/` is committed (visible in the repo screenshot). | Add it to `.gitignore` and remove it with `git rm -r --cached .idea`. Check `target/` too. |
| O5 | L | `atlas_addon.py` has no retry or backoff. Failed batches are dropped. It also fails to start without `ATLAS_INGEST_TOKEN`, which is intentional. | Add a bounded retry queue. |

### Tests
- `IngestionFlowIT` shares database state, and one test covers too much.
- There are no tests for the cases above: SSRF, masking after truncation, concurrent ingestion, and multi-replica behaviour.

## Suggested order of work
1. **S1, S3, S2:** SSRF guard, fail-fast on default secrets in prod, and an opt-in crawler click mode.
2. **S4, S5:** mask before truncating, then recursive masking.
3. **C1:** set `replicas: 1` now, then move session state out of the JVM.
4. **S6, S7:** rate limits, size limits, and protecting metrics and docs.
5. **C2–C5:** lock scope, startup recovery, a TTL cache, async AI enrichment.
6. **Tests** for each fix above, then the remaining low-severity items.
