# API Atlas REST API

All endpoints require `Authorization: Bearer <jwt>` except `/api/auth/login` and `/api/traffic/ingest` (which uses `X-Ingest-Token`).
Interactive documentation of the API itself: `/swagger-ui.html`. Errors use RFC 7807 problem details.

## Authentication
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/login` | `{"username","password"}` | `{"token","expiresInSeconds"}` or 401 |

## Catalog
| Method | Path | Description |
|---|---|---|
| GET | `/api/apis` | Paged list. Params: `page`, `size` (max 200), `q`, `method`, `module`, `service`, `auth` (NONE/BASIC/JWT/OAUTH2/API_KEY), `status` (ACTIVE/DEPRECATED/UNUSED), `protocol` (REST/GRAPHQL/SOAP/GRPC/WEBSOCKET/SSE). Sorted by last seen. |
| GET | `/api/apis/{id}` | `{endpoint, tags, findings}` with samples and AI texts |
| GET | `/api/apis/search?q=` | Text search over name, URL, module and description |
| POST | `/api/apis/{id}/analyze` | Regenerate description, category, tags, test recommendations |
| POST | `/api/apis/analyze-pending` | Enrich all endpoints without description |

Catalog entry fields: id, name, module, service, url, method, version, protocol, authType, status, thirdParty, description, businessPurpose, requestDescription, responseDescription, testRecommendations, sampleRequest, sampleResponse, hitCount, firstSeen, lastSeen.

## Insights
| Method | Path | Description |
|---|---|---|
| GET | `/api/statistics` | KPIs: totals, active/deprecated/duplicate/unused/third-party, sessions, findings, breakdowns by auth type, protocol, service, severity |
| GET | `/api/statistics/traffic` | `mostCalled`, `slowest`, `mostErrors` (top 10 each) |
| GET | `/api/security` | Open findings with counts by severity and category |
| GET | `/api/duplicates` | Groups of duplicate endpoints |
| GET | `/api/map/services` | Service dependency graph `{nodes, edges}` |

## Discovery
| Method | Path | Description |
|---|---|---|
| POST | `/api/discovery/start` | Body: `type` (WEB/MOBILE), `target`, optional `name`, `browser`, `platform`, `deviceName`, `udid`, `maxDepth`, `maxPages`, `submitForms`, `headers`, `flow`, `capabilities`. Returns 202 + session. 409 when the concurrency limit is reached. |
| POST | `/api/discovery/stop` | `{"sessionId": 1}` |
| GET | `/api/discovery/sessions` | All sessions, newest first |
| POST | `/api/traffic/ingest` | Header `X-Ingest-Token`; body: array (max 500) of captured transactions |

Captured transaction: `sessionId, url, method, requestHeaders, requestBody, statusCode, responseHeaders, responseBody, contentType, responseTimeMs, protocol`.

## Export
All accept an optional `?service=host[:port]`. Responses are downloads.

| Path | Format |
|---|---|
| `/api/export/swagger` | OpenAPI 3.0.3 JSON |
| `/api/export/postman` | Postman Collection v2.1 |
| `/api/export/excel` | XLSX (APIs + security findings) |
| `/api/export/csv` | CSV |
| `/api/export/json` | JSON |
| `/api/export/html` | HTML documentation (parameters, examples, error codes, authentication) |
| `/api/export/pdf` | PDF |
| `/api/export/tests` | ZIP: Maven project (Java 21, REST Assured, TestNG) |
| `/api/export/security-report` | HTML security report |

## Kafka topics
| Topic | Payload | Producer / consumer |
|---|---|---|
| `apiatlas.traffic.captured` | `CapturedTraffic` | external collectors / backend ingestion |
| `apiatlas.endpoint.discovered` | `EndpointDiscoveredEvent` | backend / downstream consumers |
| `apiatlas.analysis.requested` | `AnalysisRequest` | backend after a discovery session / AI enrichment |
