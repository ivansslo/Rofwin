#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR="$ROOT_DIR/artifacts"
STAGE_DIR="$ROOT_DIR/.tmp/obb-stage"
mkdir -p "$ARTIFACT_DIR" "$ROOT_DIR/.tmp"
rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR"

eval "$(python3 "$ROOT_DIR/scripts/read_android_metadata.py" --file "$ROOT_DIR/app/app/build.gradle" --shell)"

for path in installable_components wine_addons input_controls; do
  if [[ -d "$ROOT_DIR/$path" ]]; then
    cp -a "$ROOT_DIR/$path" "$STAGE_DIR/"
  fi
done

cat > "$STAGE_DIR/README.txt" <<EOF
Rofwin companion OBB package

Isi paket:
- installable_components
- wine_addons
- input_controls

Catatan:
Paket ini disiapkan untuk distribusi GitHub/offline mirror. Aplikasi inti tetap dibangun sebagai APK.
EOF

OBB_NAME="main.${VERSION_CODE}.${APPLICATION_ID}.obb"
(
  cd "$STAGE_DIR"
  zip -qr "$ARTIFACT_DIR/$OBB_NAME" .
)

echo "$ARTIFACT_DIR/$OBB_NAME"
