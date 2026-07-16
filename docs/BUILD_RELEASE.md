# Build & Release Rofwin

## Ringkasan
Repo **Rofwin** ini adalah turunan dari `ivansslo/winlator` yang difokuskan untuk perangkat **Oppo CPH1823 / Mali-G72**.

Agar repo tetap ringan, payload besar **tidak disimpan penuh di repo ini**. Sebelum build, jalankan script fetch payload untuk mengambil:
- `app/src/main/assets/`
- `installable_components/`
- `wine_addons/`

## Prasyarat lokal
- Linux/macOS atau WSL2
- Java **11**
- Android SDK Platform 34
- Android SDK Platform 28
- Build Tools 34.0.0
- NDK **24.0.8215888**
- CMake **3.22.1**
- `git`, `python3`, `zip`, `rsync`

## Build lokal
```bash
chmod +x scripts/*.sh gradlew
./scripts/prepare-github-release.sh
```

Output akan dibuat ke folder `artifacts/`:
- `Rofwin_<version>_arm64-v8a.apk`
- `main.<versionCode>.com.rofwin.obb`
- `rofwin-<version>-github-release.zip`
- `SHA256SUMS.txt`
- `RELEASE_NOTES.md`
- `release-manifest.json`
- `LATEST_RELEASE.txt`
- `GITHUB_UPLOAD_CHECKLIST.md`

## Tentang file OBB
Di repo ini, file **OBB** sudah dipakai langsung oleh aplikasi saat runtime untuk:
- `installable_components`
- `wine_addons`
- `input_controls`

Artinya:
- APK tetap artefak utama
- OBB bisa menjadi sumber payload lokal/offline
- App akan mencoba baca dari OBB lebih dulu, lalu fallback ke URL runtime jika diperlukan

## Custom source payload
Default payload source:
- Repo: `https://github.com/ivansslo/winlator.git`
- Branch: `main`

Bisa diganti dengan environment variable:
```bash
export ROFWIN_UPSTREAM_REPO="https://github.com/ivansslo/rofwin.git"
export ROFWIN_UPSTREAM_REF="main"
./scripts/fetch-upstream-payloads.sh
```

## Custom base URL untuk runtime download
Rofwin memakai `BuildConfig.CONTENT_BASE_URL` untuk download runtime asset seperti:
- `input_controls`
- `installable_components`
- `wine_addons`

Default:
```text
https://raw.githubusercontent.com/ivansslo/rofwin/main/
```

Override saat build:
```bash
export CONTENT_BASE_URL="https://raw.githubusercontent.com/ivansslo/rofwin/main/"
export REPO_URL="https://github.com/ivansslo/rofwin"
./scripts/prepare-github-release.sh
```

## Signing release APK
Jika ingin APK signed release, siapkan environment variable berikut:
```bash
export ANDROID_KEYSTORE_PATH="$PWD/release.keystore"
export ANDROID_STORE_PASSWORD="your-store-password"
export ANDROID_KEY_ALIAS="your-key-alias"
export ANDROID_KEY_PASSWORD="your-key-password"
```

Lalu build ulang:
```bash
./scripts/prepare-github-release.sh
```

## Profil default untuk Oppo CPH1823
Container baru pada device low-end Mali akan diarahkan ke:
- Vulkan: **None**
- OpenGL: **VirGL**
- DX Wrapper: **WineD3D**
- Box64: **Performance**
- Resolusi awal: **800x600**
- Env vars hemat: profil khusus low-end Mali

Lihat juga: [`docs/CPH1823-Mali-G72.md`](CPH1823-Mali-G72.md)

## Publish flow yang dirapikan
Sekarang flow utama cukup satu command:
```bash
./scripts/prepare-github-release.sh
```

Dokumen terkait:
- [`docs/GITHUB_UPLOAD_CHECKLIST.md`](GITHUB_UPLOAD_CHECKLIST.md)
- [`docs/GITHUB_ACTIONS_AUTOBUILD.md`](GITHUB_ACTIONS_AUTOBUILD.md)
