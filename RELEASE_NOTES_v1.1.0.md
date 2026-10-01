# API Atlas v1.1.0 – Security Hardening

**Heads-up:** 3 behavior changes; read "Behavior changes to note" below before upgrading (internal discovery targets are blocked by default, `prod` will not start with default secrets, mitmproxy is no longer reachable from other machines).

This release brings 7 security fixes, 2 dependency upgrades and 2 new review documents for API Atlas users.

**Highlights:** SSRF guard for web discovery; fail-fast on default secrets in `prod`; crawler headers limited to the start origin; mobile target and capability allow-list.

---

Date: 2026-10-01. Previous release: v1.0.0 (`RELEASE_NOTES.md`).

> **Status: not yet built or tested.** These changes were made without a working terminal, so `mvn clean verify` has not been run. Run it and confirm the items marked "verify" before tagging this release.

> **Publishing:** the first three lines above are what GitHub shows in the Releases section and in followers' feeds. Paste this whole file as the description when creating the `v1.1.0` release by hand. Later releases get the same header added automatically by `.github/workflows/release.yml`.

## Security fixes
- **SSRF guard for web discovery.** Targets that are, or resolve to, loopback, private, link-local, cloud-metadata (169.254.x.x) or carrier-grade NAT addresses are rejected. Set `ATLAS_ALLOW_PRIVATE_TARGETS=true` to allow them in local test environments (new `UrlGuard`, `DiscoveryService`).
- **Fail-fast on default secrets in the `prod` profile.** The app refuses to start if the JWT secret, ingest token or admin password is empty or a default/placeholder (new `ProductionSecretsGuard`).
- **Crawler headers are sent to the start origin only.** Caller-supplied headers such as `Authorization` are no longer sent to third-party hosts (`WebCrawler`). Verify: the file was corrupted once during editing.
- **Mobile discovery input validation.** The target must be a package/bundle id or a plain `.apk`/`.ipa`/`.app` file name. Only allow-listed Appium capabilities are accepted, which blocks `directConnect` and driver-path settings (`MobileExplorer`).
- **Masking happens before truncation.** A cut in the middle of a JSON value can no longer leave a secret unmasked (`TrafficIngestionService`).
- **Log injection.** Control characters are stripped from logged method and URL values.
- **Narrower public routes.** Only `/api/auth/login` is public under `/api/auth`, not the whole path (`SecurityConfig`).

## Dependencies
- `org.apache.poi:poi-ooxml` 5.3.0 → 5.4.0 (CVE-2025-31672).
- `io.appium:java-client` 9.3.0 → 10.1.1 (CVE-2026-43910). Verify: `pom.xml` still showed 9.3.0 when last read back. This is a major version, so `MobileExplorer` may need adjustments.

## Deployment
- `docker-compose.yml`: Prometheus and Grafana bound to `127.0.0.1`. Verify: the API port 8080 should no longer be published, and mitmproxy (8081) should be bound to `127.0.0.1`. Both still showed as open when last read back.
- `.env.example`: documents `ATLAS_ALLOW_PRIVATE_TARGETS`.

## Tests
- New `HardeningTest` covers the URL guard, the secrets guard, mobile validation and mask-before-truncate. Verify: the file was scrambled when last read back and may need to be recreated.

## CI/CD and automation
- **CI/CD** (`ci.yml`): can now be started manually (`workflow_dispatch`); a newer push cancels an older run on the same branch.
- **Security scans** (new `security.yml`): CodeQL for Java and JavaScript, dependency review on pull requests (fails on high severity), and Trivy for dependencies, secrets and config. Runs on push, pull request, weekly and on demand; results appear in the Security tab.
- **Dependabot** (new `dependabot.yml`): weekly update pull requests for Maven, npm, GitHub Actions and both Dockerfiles.
- **Automatic releases** (new `publish-release.yml`): pushing a `RELEASE_NOTES_v*.md` file to `main` publishes the matching GitHub Release with its summary header. It can also be run by hand or by pushing a `v*` tag.
- **Release summary header** (`release.yml`): releases created by release-please get a "Heads-up / counts / Highlights" header at the top of their notes.
- **README**: release, last-commit and changelog badges, plus a "What's new / Watch → Releases" line.

## Documentation and process
- Added `docs/CODE_REVIEW.md` and `docs/SECURITY_REVIEW.md`.
- Added the automated release workflow (release-please), `CONTRIBUTING.md` and the Conventional Commits convention.

## Behavior changes to note
- Discovery of internal or `localhost` targets is blocked by default.
- A `prod` deployment with default secrets will not start.
- Real devices can no longer reach the mitmproxy port over the network once it is bound to `127.0.0.1`. Use a tunnel or VPN.

## Known open items
Proxy authentication, Kafka and Redis authentication, Kubernetes network policies, a separate crawler worker, rate limiting on login and ingest, `replicas: 2` safety, and a retention policy for captured data. See `docs/CODE_REVIEW.md` and `docs/SECURITY_REVIEW.md`.
