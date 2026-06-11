#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_REPO="${ROFWIN_UPSTREAM_REPO:-https://github.com/ivansslo/winlator.git}"
UPSTREAM_REF="${ROFWIN_UPSTREAM_REF:-main}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

PAYLOAD_PATHS=(
  "app/app/src/main/assets"
  "installable_components"
  "wine_addons"
)

echo "[rofwin] Fetching payloads from: ${UPSTREAM_REPO} (${UPSTREAM_REF})"
git clone --depth 1 --branch "${UPSTREAM_REF}" --filter=blob:none --sparse "${UPSTREAM_REPO}" "$WORK_DIR/upstream"
(
  cd "$WORK_DIR/upstream"
  git sparse-checkout set --no-cone "${PAYLOAD_PATHS[@]}"
)

mkdir -p "$ROOT_DIR/app/app/src/main" "$ROOT_DIR/installable_components" "$ROOT_DIR/wine_addons"
rm -rf "$ROOT_DIR/app/app/src/main/assets" "$ROOT_DIR/installable_components" "$ROOT_DIR/wine_addons"
mkdir -p "$ROOT_DIR/app/app/src/main/assets" "$ROOT_DIR/installable_components" "$ROOT_DIR/wine_addons"

rsync -a "$WORK_DIR/upstream/app/app/src/main/assets/" "$ROOT_DIR/app/app/src/main/assets/"
rsync -a "$WORK_DIR/upstream/installable_components/" "$ROOT_DIR/installable_components/"
rsync -a "$WORK_DIR/upstream/wine_addons/" "$ROOT_DIR/wine_addons/"

cat > "$ROOT_DIR/PAYLOAD_SOURCE.txt" <<EOF
repo=${UPSTREAM_REPO}
ref=${UPSTREAM_REF}
fetched_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

echo "[rofwin] Payloads fetched successfully."
