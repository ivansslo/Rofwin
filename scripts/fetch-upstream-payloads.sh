#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_REPO="${ROFWIN_UPSTREAM_REPO:-https://github.com/ivansslo/winlator.git}"
UPSTREAM_REF="${ROFWIN_UPSTREAM_REF:-main}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

UPSTREAM_DIR="$WORK_DIR/upstream"
PARENT_PATHS=(
  ".gitmodules"
  "installable_components"
  "wine_addons"
  "app"
)

echo "[rofwin] Fetching payloads from: ${UPSTREAM_REPO} (${UPSTREAM_REF})"

git clone \
  --depth 1 \
  --single-branch \
  --branch "$UPSTREAM_REF" \
  --filter=blob:none \
  --sparse \
  "$UPSTREAM_REPO" \
  "$UPSTREAM_DIR"

(
  cd "$UPSTREAM_DIR"
  git sparse-checkout set --no-cone "${PARENT_PATHS[@]}"
  git submodule update --init app
)

APP_ASSETS_DIR="$UPSTREAM_DIR/app/app/src/main/assets"
INSTALLABLE_COMPONENTS_DIR="$UPSTREAM_DIR/installable_components"
WINE_ADDONS_DIR="$UPSTREAM_DIR/wine_addons"

for required_dir in "$APP_ASSETS_DIR" "$INSTALLABLE_COMPONENTS_DIR" "$WINE_ADDONS_DIR"; do
  if [[ ! -d "$required_dir" ]]; then
    echo "[rofwin] Required upstream path not found: $required_dir" >&2
    echo "[rofwin] Upstream repo/ref may not contain the expected structure or submodule checkout failed." >&2
    exit 23
  fi
done

mkdir -p "$ROOT_DIR/app/src/main" "$ROOT_DIR/installable_components" "$ROOT_DIR/wine_addons"
rm -rf "$ROOT_DIR/app/src/main/assets" "$ROOT_DIR/installable_components" "$ROOT_DIR/wine_addons"
mkdir -p "$ROOT_DIR/app/src/main/assets" "$ROOT_DIR/installable_components" "$ROOT_DIR/wine_addons"

rsync -a "$APP_ASSETS_DIR/" "$ROOT_DIR/app/src/main/assets/"
rsync -a "$INSTALLABLE_COMPONENTS_DIR/" "$ROOT_DIR/installable_components/"
rsync -a "$WINE_ADDONS_DIR/" "$ROOT_DIR/wine_addons/"

cat > "$ROOT_DIR/PAYLOAD_SOURCE.txt" <<EOF
repo=${UPSTREAM_REPO}
ref=${UPSTREAM_REF}
fetched_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

echo "[rofwin] Payloads fetched successfully."
