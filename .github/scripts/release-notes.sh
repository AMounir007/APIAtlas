#!/usr/bin/env bash
# Builds a reader-friendly GitHub Release body for one version.
#
# Usage: release-notes.sh <version> <owner/repo> <output-file>
#
# The first lines are what GitHub shows on the release card in followers' feeds, so they answer
# "what happened, should I care, what was fixed" in plain language:
#   ## API Atlas X.Y.Z
#   Heads-up / Recommended upgrade line (only when relevant)
#   One-sentence summary with counts
#   Highlights
# Below that: the curated CHANGELOG.md section (if present) or commits grouped by type, each group with a short
# "why it matters" line, upgrade notes for breaking changes, how to get the release, and the full changelog link.
#
# Commits are classified from Conventional Commit prefixes: feat, fix, perf, security, refactor, docs, test, ci,
# build, chore, revert; "!" or a "BREAKING CHANGE:" footer marks a breaking change; scope "deps" marks a
# dependency update; security keywords (CVE, SSRF, XSS, injection, vulnerability, security) mark security fixes.
set -euo pipefail

VERSION="$1"
REPO="$2"
OUT="$3"
URL="https://github.com/$REPO"
PRODUCT="API Atlas"
MAX_ITEMS=15
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# ---- previous release tag (accepts "1.2.3" and "v1.2.3"; compares numerically) -----------------------------------
prev=$(git tag --list \
  | grep -E '^v?[0-9]+\.[0-9]+\.[0-9]+$' \
  | awk -v v="$VERSION" '{ s = $0; sub(/^v/, "", s); if (s != v) print s " " $0 }' \
  | sort -V -k1,1 | tail -1 | cut -d' ' -f2 || true)

if [ -n "$prev" ]; then range="$prev..$VERSION"; else range="$VERSION"; fi

# ---- classify commits ---------------------------------------------------------------------------------------------
re='^([a-zA-Z]+)(\(([^)]+)\))?(!)?:[[:space:]]*(.+)$'
security_words='(cve-|vulnerab|ssrf|xss|csrf|injection|security)'

git log --no-merges --format='%H%x1f%h%x1f%an%x1f%s' $range > "$work/log" || true

while IFS=$'\x1f' read -r sha short author subject; do
  [ -z "${sha:-}" ] && continue
  [[ "$subject" =~ ^chore\(release\) ]] && continue

  type="other"; scope=""; bang=""; desc="$subject"
  if [[ "$subject" =~ $re ]]; then
    type="${BASH_REMATCH[1],,}"
    scope="${BASH_REMATCH[3]}"
    bang="${BASH_REMATCH[4]}"
    desc="${BASH_REMATCH[5]}"
  fi
  desc="${desc^}"

  case "$type" in
    feat|feature) cat="features" ;;
    fix|bugfix|revert) cat="fixes" ;;
    perf) cat="performance" ;;
    security|sec) cat="security" ;;
    refactor|style) cat="improvements" ;;
    docs|doc) cat="docs" ;;
    test|tests|ci|build|chore) cat="maintenance" ;;
    *) cat="other" ;;
  esac
  if [[ "${scope,,}" == deps* || "${scope,,}" == "dependencies" ]]; then cat="dependencies"; fi
  if [[ "$cat" =~ ^(features|fixes|performance|improvements|dependencies|other)$ ]] \
     && { [[ "${scope,,}" == "security" ]] || [[ "${subject,,}" =~ $security_words ]]; }; then
    cat="security"
  fi

  link="([\`$short\`]($URL/commit/$sha))"
  echo "- ${scope:+**$scope:** }$desc $link" >> "$work/$cat"
  echo "$author" >> "$work/authors"

  body=$(git log -1 --format=%B "$sha")
  if [ -n "$bang" ] || grep -qE '^BREAKING[ -]CHANGE:' <<< "$body"; then
    note=$(sed -n -E 's/^BREAKING[ -]CHANGE:[[:space:]]*//p' <<< "$body" | head -1)
    echo "- ${note:-$desc} $link" >> "$work/breaking"
  fi
done < "$work/log"

# ---- helpers ------------------------------------------------------------------------------------------------------
count() { if [ -f "$work/$1" ]; then wc -l < "$work/$1" | tr -d ' '; else echo 0; fi; }
plural() { if [ "$1" -eq 1 ]; then echo "$1 $2"; else echo "$1 $3"; fi; }
join_words() {
  local out="" i=1 n=$#
  for p in "$@"; do
    if [ "$i" -eq 1 ]; then out="$p"; elif [ "$i" -eq "$n" ]; then out="$out and $p"; else out="$out, $p"; fi
    i=$((i + 1))
  done
  echo "$out"
}
plain() { sed -E 's/^- //; s/ \(\[`[^`]*`\]\([^)]*\)\)$//; s/\*\*([^*]+)\*\*/\1/g'; }
section() { # <file> <heading> <why it matters>
  local f="$work/$1" n
  [ -f "$f" ] || return 0
  n=$(count "$1")
  printf '### %s\n_%s_\n\n' "$2" "$3"
  head -n "$MAX_ITEMS" "$f"
  if [ "$n" -gt "$MAX_ITEMS" ]; then
    printf -- '- ...and %s more, see the full changelog below.\n' "$((n - MAX_ITEMS))"
  fi
  printf '\n'
}

nf=$(count features); nx=$(count fixes); ns=$(count security); nd=$(count dependencies)
np=$(count performance); nb=$(count breaking); ni=$(count improvements)
total=$(grep -c . "$work/log" 2>/dev/null || echo 0)

parts=()
if [ "$nf" -gt 0 ]; then parts+=("$(plural "$nf" "new feature" "new features")"); fi
if [ "$nx" -gt 0 ]; then parts+=("$(plural "$nx" "fix" "fixes")"); fi
if [ "$ns" -gt 0 ]; then parts+=("$(plural "$ns" "security update" "security updates")"); fi
if [ "$np" -gt 0 ]; then parts+=("$(plural "$np" "performance improvement" "performance improvements")"); fi
if [ "$nd" -gt 0 ]; then parts+=("$(plural "$nd" "dependency update" "dependency updates")"); fi
if [ "$ni" -gt 0 ]; then parts+=("$(plural "$ni" "internal improvement" "internal improvements")"); fi

if [ "${#parts[@]}" -gt 0 ]; then
  summary="This release brings $(join_words "${parts[@]}") for $PRODUCT users."
else
  summary="Maintenance release: documentation, tests and build pipeline updates only; no change in application behavior."
fi

highlights=$(cat "$work/features" "$work/security" "$work/fixes" "$work/performance" 2>/dev/null \
  | head -3 | plain | paste -sd ';' - | sed 's/;/; /g' || true)

# Curated notes from CHANGELOG.md, if a "## [VERSION]" section exists.
curated=""
if [ -f CHANGELOG.md ]; then
  curated=$(awk -v v="$VERSION" '
    index($0, "## [" v "]") == 1 { f = 1; next }
    /^## \[/ { f = 0 }
    f' CHANGELOG.md)
fi
has_curated=false
echo "Release notes for $VERSION written to $OUT (previous tag: ${prev:-none}, commits: $total)."

} > "$OUT"
  fi
    echo "**Contributors:** $contributors"
    echo
  if [ -n "$contributors" ]; then
  echo "**Full changelog:** [$compare_text]($compare)"
  echo
  echo '```'
  echo "</dependency>"
  echo "  <version>$VERSION</version>"
  echo "  <artifactId>${REPO##*/}</artifactId>"
  echo "  <groupId>com.github.${REPO%%/*}</groupId>"
  echo "<dependency>"
  echo '```xml'
  echo
  echo "- **Maven via JitPack:**"
  echo "- **Source:** [$VERSION]($URL/releases/tag/$VERSION) (zip/tar.gz below)"
  echo
  echo "## How to get it"

  fi
    echo
    echo "</details>"
  if $has_curated; then

  section other        "Other changes"           "Changes without a Conventional Commit prefix."
  section maintenance  "Tests, CI and build"     "Keeps the project verifiable and the release process reliable."
  section docs         "Documentation"           "Clearer guides and reference material."
  section improvements "Improvements"            "Internal quality work that makes future changes safer; no change in behavior."
  section dependencies "Dependency updates"      "Third-party libraries kept current and patched."
  section performance  "Performance"             "Faster or lighter operation with no change in behavior."
  section fixes        "Bug fixes"               "Problems resolved; affected features now behave as documented."
  section features     "New features"            "New capabilities you can use in this release."
  section security     "Security"                "Reduces risk for anyone running $PRODUCT. Upgrading is recommended."

  fi
    echo
    echo "## What changed and why it matters"
  else
    echo
    echo "<details><summary>All $(plural "$total" "commit" "commits") in this release</summary>"
    echo
    echo "$curated"
    echo
    echo "## What changed and why it matters"
  if $has_curated; then

  fi
    echo
    cat "$work/breaking"
    echo
    echo "_Review these before upgrading; existing setups may need changes._"
    echo "### Upgrade notes (breaking changes)"
  if [ "$nb" -gt 0 ]; then

  echo
  echo "---"
  fi
    echo
    echo "**Highlights:** $highlights"
  if [ -n "$highlights" ]; then
  echo
  echo "$summary"
  fi
    echo
    echo "**Recommended upgrade:** includes $(plural "$ns" "security update" "security updates")."
  elif [ "$ns" -gt 0 ]; then
    echo
    echo "**Heads-up:** $(plural "$nb" "breaking change" "breaking changes"); read the upgrade notes below before upgrading."
  if [ "$nb" -gt 0 ]; then
  echo
  echo "## $PRODUCT $VERSION"
{
# ---- write the body -----------------------------------------------------------------------------------------------

contributors=$(sort -u "$work/authors" 2>/dev/null | paste -sd ',' - | sed 's/,/, /g' || true)
fi
  compare_text="all commits up to $VERSION"
  compare="$URL/commits/$VERSION"
else
  compare_text="$prev...$VERSION"
  compare="$URL/compare/$prev...$VERSION"
if [ -n "$prev" ]; then

if grep -q '[^[:space:]]' <<< "$curated"; then has_curated=true; fi
