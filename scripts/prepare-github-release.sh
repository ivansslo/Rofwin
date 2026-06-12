#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR="$ROOT_DIR/artifacts"
mkdir -p "$ARTIFACT_DIR"

eval "$(python3 "$ROOT_DIR/scripts/read_android_metadata.py" --file "$ROOT_DIR/app/app/build.gradle" --shell)"

RELEASE_TAG="${RELEASE_TAG:-v$VERSION_NAME}"
RELEASE_NOTES_TARGET="$ARTIFACT_DIR/RELEASE_NOTES.md"
RELEASE_NOTES_SOURCE=""

for candidate in \
  "$ROOT_DIR/releases/${VERSION_NAME}.md" \
  "$ROOT_DIR/releases/v${VERSION_NAME}.md" \
  "$ROOT_DIR/releases/${RELEASE_TAG}.md"
do
  if [[ -f "$candidate" ]]; then
    RELEASE_NOTES_SOURCE="$candidate"
    break
  fi
done

if [[ -z "$RELEASE_NOTES_SOURCE" && -f "$ROOT_DIR/CHANGELOG.md" ]]; then
  RELEASE_NOTES_SOURCE="$ARTIFACT_DIR/.generated-release-notes.md"
  {
    echo "# Rofwin ${VERSION_NAME}"
    echo
    echo "Release notes fallback generated from CHANGELOG.md because no specific release notes file was found."
    echo
    cat "$ROOT_DIR/CHANGELOG.md"
  } > "$RELEASE_NOTES_SOURCE"
fi

if [[ -z "$RELEASE_NOTES_SOURCE" ]]; then
  echo "[rofwin] Missing release notes file. Expected one of:" >&2
  echo "  - $ROOT_DIR/releases/${VERSION_NAME}.md" >&2
  echo "  - $ROOT_DIR/releases/v${VERSION_NAME}.md" >&2
  echo "  - $ROOT_DIR/releases/${RELEASE_TAG}.md" >&2
  exit 1
fi

"$ROOT_DIR/scripts/build-release.sh"
cp -f "$RELEASE_NOTES_SOURCE" "$RELEASE_NOTES_TARGET"
cp -f "$ROOT_DIR/docs/GITHUB_UPLOAD_CHECKLIST.md" "$ARTIFACT_DIR/GITHUB_UPLOAD_CHECKLIST.md"

cat > "$ARTIFACT_DIR/LATEST_RELEASE.txt" <<EOF
TAG=$RELEASE_TAG
VERSION_NAME=$VERSION_NAME
VERSION_CODE=$VERSION_CODE
APPLICATION_ID=$APPLICATION_ID
EOF

python3 - <<PY
import json
from pathlib import Path
artifacts = Path(${ARTIFACT_DIR@Q})
manifest = {
    "tag": ${RELEASE_TAG@Q},
    "versionName": ${VERSION_NAME@Q},
    "versionCode": ${VERSION_CODE@Q},
    "applicationId": ${APPLICATION_ID@Q},
    "repoUrl": "https://github.com/ivansslo/rofwin",
    "contentBaseUrl": "https://raw.githubusercontent.com/ivansslo/rofwin/main/",
    "artifacts": sorted([p.name for p in artifacts.iterdir() if p.is_file()]),
}
(artifacts / "release-manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
PY

"$ROOT_DIR/scripts/package-zip.sh" >/dev/null

echo "[rofwin] Release bundle is ready in: $ARTIFACT_DIR"
ls -lh "$ARTIFACT_DIR"
