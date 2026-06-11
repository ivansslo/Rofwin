# Rofwin Android App

Modul Android utama untuk Rofwin.

## Yang diubah dari basis source
- `applicationId` diarahkan ke `com.rofwin`
- `app_name` menjadi **Rofwin**
- `BuildConfig.CONTENT_BASE_URL` dipakai untuk URL asset runtime
- `BuildConfig.REPO_URL` dipakai untuk link repo di app (`https://github.com/ivansslo/rofwin`)
- integrasi **OBB runtime** untuk `input_controls`, `installable_components`, dan `wine_addons`
- default baru untuk perangkat **low-end Mali / Oppo CPH1823**

## Build cepat
Dari root repo:
```bash
./scripts/prepare-github-release.sh
```

Dokumentasi lengkap ada di:
- [`../docs/BUILD_RELEASE.md`](../docs/BUILD_RELEASE.md)
- [`../docs/GITHUB_ACTIONS_AUTOBUILD.md`](../docs/GITHUB_ACTIONS_AUTOBUILD.md)
