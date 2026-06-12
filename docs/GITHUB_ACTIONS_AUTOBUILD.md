# GitHub Actions Auto Build — Rofwin

Workflow auto build berada di:
- `.github/workflows/android-build.yml`

## Trigger otomatis
Workflow ini akan berjalan otomatis saat:
- push ke `main`
- push ke `master`
- push tag `v*`
- pull request
- manual run via **workflow_dispatch**

## Hasil build
Workflow menghasilkan bundle artifact yang berisi:
- APK
- OBB
- ZIP release
- `SHA256SUMS.txt`
- `RELEASE_NOTES.md`
- `release-manifest.json`
- `LATEST_RELEASE.txt`
- `GITHUB_UPLOAD_CHECKLIST.md`

## Signing behavior
- Jika secrets signing tersedia, workflow bisa build signed APK
- Jika secrets tidak ada, workflow tetap jalan dan menghasilkan unsigned APK
- Saat manual dispatch, Anda bisa memilih apakah signing dipakai atau tidak

## Manual run
Dari tab **Actions** di GitHub:
1. pilih workflow **auto-build**
2. klik **Run workflow**
3. opsional isi:
   - `signed_release`
   - `artifact_suffix`

## Secrets yang dipakai
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Variables yang bisa dioverride
- `ROFWIN_UPSTREAM_REPO`
- `ROFWIN_UPSTREAM_REF`
- `CONTENT_BASE_URL`
- `REPO_URL`

Default repo URL sekarang diarahkan ke:
- `https://github.com/ivansslo/rofwin`
- `https://raw.githubusercontent.com/ivansslo/rofwin/main/`

## Catatan
Workflow ini memakai script lokal:
```bash
./scripts/prepare-github-release.sh
```
Jadi flow build lokal dan flow GitHub Actions tetap konsisten.

Jika ingin build dipicu otomatis setelah release upstream Winlator, lihat:
- [`UPSTREAM_RELEASE_CHAIN.md`](UPSTREAM_RELEASE_CHAIN.md)
