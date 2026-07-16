#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR="$ROOT_DIR/artifacts"
STAGE_DIR="$ROOT_DIR/.tmp/zip-stage"
mkdir -p "$ARTIFACT_DIR" "$ROOT_DIR/.tmp"
rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR"

eval "$(python3 "$ROOT_DIR/scripts/read_android_metadata.py" --file "$ROOT_DIR/app/build.gradle.kts" --shell)"

RELEASE_NOTES_SOURCE=""
for candidate in \
  "$ROOT_DIR/releases/${VERSION_NAME}.md" \
  "$ROOT_DIR/releases/v${VERSION_NAME}.md"
do
  if [[ -f "$candidate" ]]; then
    RELEASE_NOTES_SOURCE="$candidate"
    break
  fi
done

find "$ARTIFACT_DIR" -maxdepth 1 -type f \( -name '*.apk' -o -name '*.obb' -o -name 'SHA256SUMS.txt' -o -name 'RELEASE_NOTES.md' -o -name 'release-manifest.json' -o -name 'LATEST_RELEASE.txt' -o -name 'GITHUB_UPLOAD_CHECKLIST.md' \) -exec cp -a {} "$STAGE_DIR/" \;
cp -a "$ROOT_DIR/docs/CPH1823-Mali-G72.md" "$STAGE_DIR/"
cp -a "$ROOT_DIR/docs/BUILD_RELEASE.md" "$STAGE_DIR/"
if [[ -n "$RELEASE_NOTES_SOURCE" ]]; then
  cp -a "$RELEASE_NOTES_SOURCE" "$STAGE_DIR/release-notes-source.md"
fi
if [[ -f "$ROOT_DIR/PAYLOAD_SOURCE.txt" ]]; then
  cp -a "$ROOT_DIR/PAYLOAD_SOURCE.txt" "$STAGE_DIR/"
fi

ZIP_NAME="rofwin-${VERSION_NAME}-github-release.zip"
(
  cd "$STAGE_DIR"
  zip -qr "$ARTIFACT_DIR/$ZIP_NAME" .
)

echo "$ARTIFACT_DIR/$ZIP_NAME"
