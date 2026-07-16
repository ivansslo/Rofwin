<p align="center">
  <img src="logo.png" alt="Rofwin logo" width="720" />
</p>

# Rofwin

Rofwin adalah fork/turunan bergaya Winlator untuk menjalankan aplikasi Windows (x86_64) di Android dengan Wine + Box64, dengan fokus tuning awal untuk **Oppo CPH1823 (Oppo F9/F9 Pro, Mali-G72, Helio P60, RAM 4 GB)**.

## Fokus repo ini
- basis source mengikuti struktur Winlator
- default container lebih aman untuk **low-end Mali**
- branding repo: **rofwin**
- siap untuk build dan rilis **APK + OBB + ZIP** ke GitHub
- payload besar dipisahkan dari repo agar clone tetap ringan

## Default tuning untuk Oppo CPH1823
Untuk device low-end Mali, container baru diarahkan ke:
- Vulkan: **None**
- OpenGL: **VirGL**
- DX Wrapper: **WineD3D**
- Box64 preset: **Performance**
- Resolusi awal: **800x600**
- Env vars hemat untuk Mali

Panduan detail: [`docs/CPH1823-Mali-G72.md`](docs/CPH1823-Mali-G72.md)

## Struktur penting
- `app/` → project Android/Gradle utama
- `docs/` → dokumentasi tuning dan release
- `scripts/` → helper fetch payload, build, package OBB, package ZIP
- `.github/workflows/` → CI build dan GitHub release

## Kenapa repo ini ringan?
Folder payload besar tidak dibundel penuh di repo kerja ini. Saat build, script akan mengambil payload upstream:
- `app/src/main/assets`
- `installable_components`
- `wine_addons`

Jalankan:
```bash
chmod +x scripts/*.sh gradlew
./scripts/prepare-github-release.sh
```

## Artefak release
Script publish bundle menghasilkan:
- APK: `Rofwin_<version>_arm64-v8a.apk`
- OBB: `main.<versionCode>.com.rofwin.obb`
- ZIP: `rofwin-<version>-github-release.zip`
- checksum: `SHA256SUMS.txt`
- `RELEASE_NOTES.md`
- `release-manifest.json`
- `LATEST_RELEASE.txt`
- `GITHUB_UPLOAD_CHECKLIST.md`

## GitHub Actions
Workflow yang disediakan:
- `android-build.yml` → **auto build otomatis** pada push / PR / tag / manual
- `github-release.yml` → publish release saat push tag `v*` atau manual dispatch

Workflow YAML sudah diperbarui agar kompatibel dengan migrasi Node.js 24 di GitHub Actions.

Dokumentasi auto build:
- [`docs/GITHUB_ACTIONS_AUTOBUILD.md`](docs/GITHUB_ACTIONS_AUTOBUILD.md)

Workflow sudah disiapkan untuk membentuk URL runtime GitHub secara otomatis dari owner repo saat dijalankan di GitHub Actions. Untuk build lokal, Anda bisa override:
```bash
export CONTENT_BASE_URL="https://raw.githubusercontent.com/ivansslo/rofwin/main/"
export REPO_URL="https://github.com/ivansslo/rofwin"
```

## Catatan OBB
Di implementasi repo ini, OBB **sudah diintegrasikan ke runtime app** untuk payload berikut:
- `input_controls`
- `installable_components`
- `wine_addons`

Urutan sumber data runtime:
1. baca dari **OBB** jika tersedia
2. fallback ke **CONTENT_BASE_URL**

Jadi APK tetap artefak utama, tetapi OBB kini bisa dipakai langsung oleh app saat runtime.

## Dokumen release
- Checklist upload GitHub: [`docs/GITHUB_UPLOAD_CHECKLIST.md`](docs/GITHUB_UPLOAD_CHECKLIST.md)
- Release notes v1.0.0: [`releases/v1.0.0.md`](releases/v1.0.0.md)
- Alur release berantai Winlator ➜ Rofwin: [`docs/UPSTREAM_RELEASE_CHAIN.md`](docs/UPSTREAM_RELEASE_CHAIN.md)

## Lisensi
Lihat [`LICENSE`](LICENSE) dan [`NOTICE.md`](NOTICE.md). Repo ini tetap membawa atribusi upstream yang relevan.
