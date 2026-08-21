#!/usr/bin/env bash
# =============================================================================
# bump-version.sh — bump release version (single source: anvil.yaml)
#
# Updates BOTH anvil.yaml (project.version) and pom.xml (project <version>)
# so the bundle filename (derived from jar name) stays in sync.
#
# Usage:
#   scripts/bump-version.sh <major|minor|patch> [--dry-run]
#
# Examples:
#   scripts/bump-version.sh patch          # 0.4.0 -> 0.4.1
#   scripts/bump-version.sh minor          # 0.4.0 -> 0.5.0
#   scripts/bump-version.sh major          # 0.4.0 -> 1.0.0
#   scripts/bump-version.sh minor --dry-run
#
# What it does:
#   1. read current version from anvil.yaml project.version
#   2. compute next version by bump type
#   3. rewrite anvil.yaml + pom.xml (project version only, not parent/deps)
#   4. git commit "chore(release): vX.Y.Z"
#   5. git tag -a vX.Y.Z + push master and tag  -> triggers Release workflow
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

die() { printf '[bump][ERROR] %s\n' "$*" >&2; exit 1; }
info() { printf '[bump] %s\n' "$*"; }

BUMP_TYPE="${1:-}"
DRY_RUN="${2:-}"

[ -n "$BUMP_TYPE" ] || die "usage: scripts/bump-version.sh <major|minor|patch> [--dry-run]"
case "$BUMP_TYPE" in
    major|minor|patch) ;;
    *) die "unknown bump type '$BUMP_TYPE' (use major|minor|patch)" ;;
esac
[ -z "$DRY_RUN" ] || [ "$DRY_RUN" = "--dry-run" ] || die "second arg must be --dry-run"

command -v git >/dev/null || die "git not found"

# --- working tree must be clean ---------------------------------------------
if [ -n "$(git status --porcelain)" ]; then
    die "working tree not clean — commit or stash first"
fi

# --- read current version from anvil.yaml ------------------------------------
CURRENT="$(sed -nE 's/^[[:space:]]*version:[[:space:]]*["'"'"']?([0-9]+\.[0-9]+\.[0-9]+)["'"'"']?.*/\1/p' anvil.yaml | head -1)"
[ -n "$CURRENT" ] || die "cannot parse project.version from anvil.yaml"

MAJOR="$(printf '%s' "$CURRENT" | cut -d. -f1)"
MINOR="$(printf '%s' "$CURRENT" | cut -d. -f2)"
PATCH="$(printf '%s' "$CURRENT" | cut -d. -f3)"

case "$BUMP_TYPE" in
    major) MAJOR=$((MAJOR+1)); MINOR=0; PATCH=0 ;;
    minor) MINOR=$((MINOR+1)); PATCH=0 ;;
    patch) PATCH=$((PATCH+1)) ;;
esac
NEXT="${MAJOR}.${MINOR}.${PATCH}"

info "current: $CURRENT -> next: $NEXT ($BUMP_TYPE)"

if [ "$DRY_RUN" = "--dry-run" ]; then
    info "dry-run: no files modified, no commit/tag/push"
    exit 0
fi

# --- rewrite anvil.yaml -------------------------------------------------------
sed -i.bak -E "0,/^(^[[:space:]]*)version:[[:space:]]*[\"']?[0-9]+\.[0-9]+\.[0-9]+[\"']?/s//\1version: ${NEXT}/" anvil.yaml
rm -f anvil.yaml.bak

NEW_ANVIL="$(sed -nE 's/^[[:space:]]*version:[[:space:]]*["'"'"']?([0-9]+\.[0-9]+\.[0-9]+)["'"'"']?.*/\1/p' anvil.yaml | head -1)"
[ "$NEW_ANVIL" = "$NEXT" ] || die "anvil.yaml update failed (got '$NEW_ANVIL')"

# --- rewrite pom.xml project version (first <version> AFTER </parent>) --------
awk -v v="$NEXT" '
    /<\/parent>/ { inParent=0; done=done; print; next }
    !done && !inParent && /<version>[^<]*<\/version>/ {
        sub(/<version>[^<]*<\/version>/, "<version>" v "</version>")
        done=1; print; next
    }
    { print }
    BEGIN { inParent=1; done=0 }
' pom.xml > pom.xml.new

# sanity: parent version untouched, project version bumped
grep -q "<version>3.3.3</version>" pom.xml.new || die "pom parent version corrupted, aborting"
grep -q "<version>${NEXT}</version>" pom.xml.new || die "pom project version not updated, aborting"
mv pom.xml.new pom.xml

info "updated: anvil.yaml, pom.xml"

# --- commit + tag + push -------------------------------------------------------
git add anvil.yaml pom.xml
git commit -m "chore(release): v${NEXT}"
git tag -a "v${NEXT}" -m "Release v${NEXT}"

info "pushing master + tag v${NEXT} (triggers Release workflow)..."
git push origin master
git push origin "v${NEXT}"

info "done. Release workflow: https://github.com/$(git remote get-url origin | sed -E 's#.*github.com[:/]##; s#\.git$##')/actions"
