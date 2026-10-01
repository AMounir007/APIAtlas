# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).
Releases are created automatically on every push to `main` (see "Releasing" in the README). The release workflow
uses the section whose heading matches the version being released, for example `## [1.1.0]`; if there is none,
GitHub generates the notes from the commits.

## [1.1.0] - 2026-10-01

### Security
- SSRF guard for web discovery: targets resolving to loopback, private, link-local, metadata or carrier-grade NAT
  addresses are rejected (`ATLAS_ALLOW_PRIVATE_TARGETS=true` overrides it for test environments).
- The `prod` profile refuses to start with a default or placeholder JWT secret, ingest token or admin password.
- Caller-supplied crawler headers are sent only to the start origin.
- Mobile discovery accepts only package/bundle ids or plain app file names, and only allow-listed Appium capabilities.
- Request and response bodies are masked before truncation; control characters are stripped from logged values.
- Only `/api/auth/login` is public under `/api/auth`.

### Changed
- Upgraded `org.apache.poi:poi-ooxml` to 5.4.0 (CVE-2025-31672) and `io.appium:java-client` to 10.1.1 (CVE-2026-43910).
- `docker-compose.yml`: Prometheus, Grafana and mitmproxy bound to `127.0.0.1`; the API is no longer published directly.

### Added
- CI/CD, CodeQL, Trivy and dependency-review workflows, plus Dependabot.
- Automatic release pipeline (patch bump, tag, GitHub Release, JitPack).
- `docs/CODE_REVIEW.md`, `docs/SECURITY_REVIEW.md` and `HardeningTest`.

> Note: this release was prepared without a working build environment; confirm the first CI run is green.

## [1.0.0] - 2026-09-30

### Added
- Initial release: discovery (Playwright, Appium, mitmproxy), API catalog, AI analysis, security analysis,
  exports (OpenAPI, Postman, Excel, CSV, JSON, HTML, PDF, generated tests), React dashboard, Docker and Kubernetes files.
