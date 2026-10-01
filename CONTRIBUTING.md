# Contributing and Release Process

## Versioning
Semantic Versioning (`MAJOR.MINOR.PATCH`), derived automatically from commit messages.

## Commit format (Conventional Commits)
| Prefix | Effect |
|---|---|
| `fix: ...` | patch release (1.0.0 -> 1.0.1) |
| `feat: ...` | minor release (1.0.0 -> 1.1.0) |
| `feat!: ...` or a `BREAKING CHANGE:` footer | major release (1.0.0 -> 2.0.0) |
| `test:`, `docs:`, `ci:`, `refactor:`, `perf:` | listed in the changelog, no version bump on their own |
| `chore:` | not listed |

## Automated release flow
1. Merge work into `main` using Conventional Commit messages.
2. The `Release` workflow opens or updates a **Release PR**. It bumps the version in `pom.xml` and updates `CHANGELOG.md`.
3. Merge the Release PR. The workflow creates the `vX.Y.Z` tag and the GitHub Release (visible under **Releases**) automatically.

Do not edit `CHANGELOG.md` by hand. It is generated. `RELEASE_NOTES.md` is the hand-written v1.0.0 baseline.

## Repository setting required (one time)
Settings -> Actions -> General -> Workflow permissions: select **Read and write permissions** and tick
**Allow GitHub Actions to create and approve pull requests**.
