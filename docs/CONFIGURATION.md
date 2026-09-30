a# API Atlas Configuration Reference

Bound by `src/main/java/com/apiatlas/config/AtlasProperties.java` under the prefix `apiatlas`.
Enable binding with `@ConfigurationPropertiesScan` on the application class or `@EnableConfigurationProperties(AtlasProperties.class)`.
`Duration` values accept `10s`, `5m`, `12h`, `7d`.

## security
| Key | Type | Description |
|---|---|---|
| `jwt.secret` | String | JWT signing secret (use an env var, never commit) |
| `jwt.expiration` | Duration | Token lifetime |
| `jwt.issuer` | String | Issuer claim |
| `cors.allowed-origins` | List<String> | Allowed CORS origins |

## kafka.topics
| Key | Description |
|---|---|
| `captured-traffic` | Raw captured traffic |
| `endpoint-discovered` | Newly discovered endpoints |
| `analysis-requested` | Analysis jobs |

## discovery
| Key | Type | Description |
|---|---|---|
| `max-concurrent-sessions` | int | Parallel discovery sessions |
| `web.default-browser` | String | chromium / firefox / webkit |
| `web.headless` | boolean | Headless mode |
| `web.max-depth` | int | Crawl depth |
| `web.max-pages` | int | Max pages per run |
| `web.navigation-timeout` | Duration | Navigation timeout |
| `web.same-origin-only` | boolean | Restrict crawl to origin |
| `mobile.appium-url` | String | Appium server URL |
| `mobile.max-actions` | int | Max actions per session |
| `proxy.host` / `proxy.port` | String / int | MITMProxy endpoint |
| `proxy.ingest-token` | String | Token for traffic ingestion |
| `capture.max-body-size` | int | Max payload bytes stored |
| `capture.excluded-extensions` | List<String> | Static assets to skip |
| `capture.masked-headers` | List<String> | Headers masked in storage/logs |

## ai
| Key | Type | Description |
|---|---|---|
| `enabled` | boolean | Toggle AI features |
| `provider` | String | Provider name |
| `base-url` | String | Provider endpoint |
| `api-key` | String | Provider key (env var) |
| `model` | String | Model id |
| `timeout` | Duration | Request timeout |

## analysis
| Key | Type | Description |
|---|---|---|
| `unused-threshold` | Duration | Inactivity age to flag an API unused |
| `duplicate-similarity` | double | Duplicate threshold (0..1) |

## Environment variables
| Variable | Default | Purpose |
```
    duplicate-similarity: 0.9
    unused-threshold: 30d
  analysis:
    timeout: 30s
    model: gpt-4o-mini
    api-key: ${AI_API_KEY:}
    base-url: https://api.openai.com
    provider: openai
    enabled: true
  ai:
      masked-headers: [Authorization, Cookie, Set-Cookie]
      excluded-extensions: [png, jpg, css, js]
      max-body-size: 1048576
    capture:
    proxy: {host: localhost, port: 8080, ingest-token: "${INGEST_TOKEN:local-token}"}
    mobile: {appium-url: "http://localhost:4723", max-actions: 200}
    web: {default-browser: chromium, headless: true, max-depth: 4, max-pages: 500, navigation-timeout: 20s, same-origin-only: true}
    max-concurrent-sessions: 5
  discovery:
      analysis-requested: apiatlas.analysis-requested
      endpoint-discovered: apiatlas.endpoint-discovered
      captured-traffic: apiatlas.captured-traffic
    topics:
  kafka:
      allowed-origins: [http://localhost:3000]
    cors:
      issuer: api-atlas
      expiration: 12h
      secret: ${JWT_SECRET:change-me}
    jwt:
  security:
apiatlas:
```yaml
## Example (structure of `apiatlas.*`; see `application.yml` for the real defaults)

| `DISCOVERY_MAX_SESSIONS` | 5 | concurrent discovery sessions |
| `AI_ENABLED`, `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` | disabled | LLM enrichment |
| `CORS_ORIGINS` | `http://localhost:3000` | allowed dashboard origins |
| `ATLAS_ALLOWED_HOSTS` | empty (allow all) | comma-separated hosts (subdomains included) allowed as web discovery targets |
| `MITM_INGEST_TOKEN` | `change-me` | token for `/api/traffic/ingest` |
| `ATLAS_ADMIN_USER` / `ATLAS_ADMIN_PASSWORD` | `admin` / `admin123` | dashboard login (change the password) |
| `JWT_SECRET` | dev value | JWT signing key (>= 32 bytes, required in production) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | messaging |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | localhost | cache |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local PostgreSQL | database connection |
|---|---|---|
