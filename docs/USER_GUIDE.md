# API Atlas - User Guide

## Sign in
Open the dashboard and sign in with the administrator credentials configured for your installation.

## Discover APIs
1. **Discovery** -> choose *Web* or *Mobile*.
2. Web: enter the start URL, pick a browser (Chromium, Chrome, Edge, Firefox, WebKit/Safari) and optionally enable *Submit forms*.
   Forms containing password fields are never submitted and buttons labelled delete/logout/pay/... are never clicked.
3. Mobile: enter the app path (`.apk`/`.ipa`) or package/bundle id and the platform. Traffic is captured by mitmproxy (see the Installation Guide).
4. Click **Start**. Progress and the number of endpoints found appear in the sessions table; **Stop** ends a run.

Only scan applications you own or are explicitly authorized to test.

## Browse the catalog
- **API Inventory**: search and filter by method, authentication type and status. Click a row to open the **API Explorer**.
- **API Explorer**: description, business purpose, request/response descriptions, samples (secrets are masked), test recommendations, tags and security findings. *Re-run AI analysis* regenerates the texts.
- **Service Map**: services and the call sequences observed between them (orange = third-party).
- **Traffic Analytics**: most called, slowest and most failing endpoints.
- **Security Findings**: open findings mapped to OWASP API Security 2023, with a downloadable HTML report.
- **Swagger Viewer**: interactive OpenAPI of the whole catalog.

## Statuses
| Status | Meaning |
|---|---|
| ACTIVE | seen within the `unused-threshold` (default 30 days) |
| DEPRECATED | response has `Deprecation`/`Sunset` headers or the path contains `/deprecated` or `/legacy` |
| UNUSED | not seen for longer than the threshold (checked hourly) |

Duplicates are endpoints with the same method and path (ignoring version segments) whose path similarity is at least `apiatlas.analysis.duplicate-similarity`.

## Export Center
OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF and a generated **REST Assured + TestNG** project. Optionally restrict to one service.

### Generated test project
```
unzip api-atlas-tests.zip -d api-tests && cd api-tests
mvn test                                    # safe (GET/HEAD) endpoints only
mvn test -Dtoken=<bearer> -Denv=qa          # authenticated endpoints
mvn test -Dallow.mutating=true              # also POST/PUT/PATCH/DELETE - test systems only!
```
Suites: smoke, regression, contract, negative and boundary; data driven from `endpoints.json`, parallel, configurable per environment.

## Ingest from your own collectors
`POST /api/traffic/ingest` with header `X-Ingest-Token` and a JSON array of captured transactions (see `docker/mitm/atlas_addon.py` for the format). Batches are limited to 500 items.

## AI enrichment
Set `AI_ENABLED=true` and `AI_API_KEY`. Without it, deterministic heuristics fill the descriptions, categories, tags and test recommendations. Captured samples are sent to the configured provider - make sure that is acceptable for your data.

## Troubleshooting
| Symptom | Check |
|---|---|
| 401 on every call | token expired; sign in again |
| No endpoints after web discovery | target requires login (pass headers via the API `headers` field) or blocks headless browsers |
| Mobile: no traffic | proxy configured on the device, CA installed, correct `ATLAS_SESSION_ID`, ingest token matches |
| Events not published | Kafka unreachable - publishing pauses for 60 s and ingestion continues |
