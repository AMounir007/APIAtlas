# Contributing and Release Process

## Versioning and releases
Every push to `main` is released automatically by `.github/workflows/auto-release.yml`: the patch version is bumped
(1.1.0 -> 1.1.1), `pom.xml` is updated, the tag `X.Y.Z` is pushed, a GitHub Release is published and JitPack builds it.
To jump to a new minor or major version, run the workflow manually with `next_version` (for example `1.2.0`).

## Commit messages shape the release feed
The release notes, and the card followers see in their GitHub feed, are built from your commit messages by
`.github/scripts/release-notes.sh`. Write each commit as `type(scope): what changed, in user terms`.

| Prefix | Appears in the release as | "Why it matters" line shown to readers |
|---|---|---|
| `feat:` | New features (counted in the summary) | New capabilities you can use in this release |
| `fix:` | Bug fixes (counted) | Problems resolved |
| `security:`, `fix(security):`, or a subject mentioning CVE/SSRF/XSS/injection | Security (counted, adds "Recommended upgrade") | Reduces risk; upgrading is recommended |
| `perf:` | Performance (counted) | Faster or lighter operation |
| `fix(deps):`, `build(deps):` | Dependency updates (counted) | Libraries kept current and patched |
| `refactor:` | Improvements | Internal quality, no behavior change |
| `docs:` | Documentation | Clearer guides |
| `test:`, `ci:`, `build:`, `chore:` | Tests, CI and build | Keeps the project verifiable |
| `feat!:` or a `BREAKING CHANGE: <what to do>` footer | Heads-up line + Upgrade notes | Tells users what to change before upgrading |

Good: `fix(crawler): stop sending Authorization headers to third-party hosts`
Weak: `fix: update WebCrawler.java`

The first three features, security items or fixes become the **Highlights** line, so put the most valuable change
first in the push.

## Curated notes (optional)
For a larger release, add a `## [X.Y.Z] - YYYY-MM-DD` section to `CHANGELOG.md` before pushing. It replaces the
grouped commit list in the release body; the summary, highlights and the full commit list (collapsed) are still added.

## Repository setting required (one time)
Settings -> Actions -> General -> Workflow permissions: select **Read and write permissions**.
