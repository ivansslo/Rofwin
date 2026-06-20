#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR="$ROOT_DIR/artifacts"
mkdir -p "$ARTIFACT_DIR"

eval "$(python3 "$ROOT_DIR/scripts/read_android_metadata.py" --file "$ROOT_DIR/app/app/build.gradle" --shell)"

if [[ ! -f "$ROOT_DIR/app/app/src/main/assets/rootfs.tzst" ]]; then
  "$ROOT_DIR/scripts/fetch-upstream-payloads.sh"
fi

chmod +x "$ROOT_DIR/app/gradlew"
pushd "$ROOT_DIR/app" >/dev/null

echo "[rofwin] Running Gradle release build..."
./gradlew :app:assembleRelease :app:bundleRelease --refresh-dependencies

popd >/dev/null

BUNDLE_SOURCE="$(find "$ROOT_DIR/app/app/build/outputs/bundle/release" -maxdepth 1 -type f -name '*.aab' | head -n 1)"
if [[ -z "${BUNDLE_SOURCE}" ]]; then
  echo "[rofwin] Bundle release output not found." >&2
  exit 1
fi

BUNDLE_TARGET="$ARTIFACT_DIR/Rofwin_${VERSION_NAME}_arm64-v8a.aab"
cp -f "$BUNDLE_SOURCE" "$BUNDLE_TARGET"

APK_SOURCE="$(find "$ROOT_DIR/app/app/build/outputs/apk/release" -maxdepth 1 -type f -name '*.apk' | head -n 1)"
if [[ -z "${APK_SOURCE}" ]]; then
  echo "[rofwin] APK release output not found." >&2
  exit 1
fi

APK_SUFFIX=""
if [[ "$APK_SOURCE" == *unsigned* ]]; then
  APK_SUFFIX="-unsigned"
fi
APK_TARGET="$ARTIFACT_DIR/Rofwin_${VERSION_NAME}_arm64-v8a${APK_SUFFIX}.apk"
cp -f "$APK_SOURCE" "$APK_TARGET"

"$ROOT_DIR/scripts/package-obb.sh" >/dev/null

TMP_SUMS="$ARTIFACT_DIR/.SHA256SUMS.tmp"
find "$ARTIFACT_DIR" -maxdepth 1 -type f \( -name '*.apk' -o -name '*.obb' \) -print0 | sort -z | xargs -0 sha256sum > "$TMP_SUMS"
mv "$TMP_SUMS" "$ARTIFACT_DIR/SHA256SUMS.txt"

"$ROOT_DIR/scripts/package-zip.sh" >/dev/null

echo "[rofwin] Build complete. Artifacts:"
ls -lh "$ARTIFACT_DIR"
