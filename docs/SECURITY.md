# API Atlas - Security Notes

## Built-in controls
- Stateless JWT authentication (HS256, secret >= 32 bytes enforced at startup); all `/api/**` endpoints except login and traffic ingest require a token.
- Traffic ingest is protected by a shared token compared in constant time; batches are size-limited.
- Secrets are masked before storage: configured headers (`apiatlas.discovery.capture.masked-headers`), sensitive query parameters, and JSON/form fields such as `password`, `token`, `secret`. Cookie **values** are never stored, only names.
- Exports neutralize spreadsheet formula injection, HTML output is escaped and carries a restrictive CSP, and downloads are served as attachments with `nosniff`.
- Generated test projects contain only data plus fixed templates - captured data is never compiled as code. Mutating requests are disabled by default.
- Crawler safety: forms with password fields are skipped, destructive buttons are not clicked, form submission is opt-in.
- Container runs as a non-root user; Kubernetes manifests drop all capabilities and set a seccomp profile.

## Deployment checklist
1. Set strong values for `JWT_SECRET`, `MITM_INGEST_TOKEN`, `ATLAS_ADMIN_PASSWORD`, `DB_PASSWORD`, `GRAFANA_PASSWORD`. The defaults in `application.yml` exist for local development only.
2. Terminate TLS in front of the API (Ingress) and restrict `CORS_ORIGINS` to your dashboard origin.
3. Restrict network access to `/actuator/prometheus` (it is unauthenticated so that Prometheus can scrape it).
4. The dashboard stores the JWT in `sessionStorage`; keep the dashboard free of third-party scripts.
5. Discovery makes the server fetch URLs supplied by authenticated users (SSRF surface). Set `ATLAS_ALLOWED_HOSTS` (comma-separated, e.g. `example.com,staging.example.org`; subdomains are included) to restrict web discovery targets. It is empty (allow all) by default - set it in every shared environment. Also run the backend in a network segment that cannot reach sensitive internal services.
6. Captured payloads may contain personal data. Define a retention policy; the number of stored request/response samples per endpoint is capped at 25.

## Known limitations
- Single administrator account from environment variables; there is no user management or role separation yet.
- No rate limiting or lockout on `/api/auth/login`; put a WAF/ingress limit in front.
- Third-party detection compares the last two host labels, so multi-part public suffixes (e.g. `.co.uk`) can be misclassified.
