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

## Example
```yaml
apiatlas:
  security:
    jwt:
      secret: ${JWT_SECRET:change-me}
      expiration: 12h
      issuer: api-atlas
    cors:
      allowed-origins: [http://localhost:3000]
  kafka:
    topics:
      captured-traffic: apiatlas.captured-traffic
      endpoint-discovered: apiatlas.endpoint-discovered
      analysis-requested: apiatlas.analysis-requested
  discovery:
    max-concurrent-sessions: 5
    web: {default-browser: chromium, headless: true, max-depth: 4, max-pages: 500, navigation-timeout: 20s, same-origin-only: true}
    mobile: {appium-url: "http://localhost:4723", max-actions: 200}
    proxy: {host: localhost, port: 8080, ingest-token: "${INGEST_TOKEN:local-token}"}
    capture:
      max-body-size: 1048576
      excluded-extensions: [png, jpg, css, js]
      masked-headers: [Authorization, Cookie, Set-Cookie]
  ai:
    enabled: true
    provider: openai
    base-url: https://api.openai.com
    api-key: ${AI_API_KEY:}
    model: gpt-4o-mini
    timeout: 30s
  analysis:
    unused-threshold: 30d
    duplicate-similarity: 0.9
```
