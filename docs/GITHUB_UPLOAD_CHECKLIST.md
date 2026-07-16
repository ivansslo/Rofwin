# Checklist Upload GitHub Final — Rofwin

## A. Sebelum upload
- [ ] Pastikan repo lokal ada di folder `rofwin/`
- [ ] Pastikan branch utama adalah `main`
- [ ] Pastikan remote GitHub target adalah `https://github.com/ivansslo/rofwin`
- [ ] Pastikan file branding sudah benar:
  - [ ] `logo.png`
  - [ ] `brand/rofwin-logo.png`
  - [ ] `brand/rofwin-icon.png`
- [ ] Pastikan versi aplikasi sudah benar di `app/build.gradle.kts`
  - [ ] `versionName = "1.0.0"`
  - [ ] `versionCode = 100`
- [ ] Pastikan release notes ada di `releases/v1.0.0.md`
- [ ] Pastikan tag release adalah `v1.0.0`

## B. Secrets GitHub Actions
Di GitHub repo → **Settings → Secrets and variables → Actions**:

### Secrets wajib jika ingin APK signed
- [ ] `ANDROID_KEYSTORE_BASE64`
- [ ] `ANDROID_STORE_PASSWORD`
- [ ] `ANDROID_KEY_ALIAS`
- [ ] `ANDROID_KEY_PASSWORD`

### Variables opsional
- [ ] `ROFWIN_UPSTREAM_REPO` (default sudah ada)
- [ ] `ROFWIN_UPSTREAM_REF` (default `main`)
- [ ] `CONTENT_BASE_URL` (default sudah diarahkan ke repo ini)
- [ ] `REPO_URL` (default sudah diarahkan ke repo ini)

## C. Push source ke GitHub
```bash
cd rofwin
git remote add origin https://github.com/ivansslo/rofwin.git
git push -u origin main
git push origin v1.0.0
```

Checklist:
- [ ] branch `main` berhasil push
- [ ] tag `v1.0.0` berhasil push

## D. Validasi workflow
Workflow yang harus muncul di tab **Actions**:
- [ ] `auto-build`
- [ ] `github-release`
- [ ] `sync-from-winlator-release` (jika memakai release chain upstream)

Checklist:
- [ ] `auto-build` sukses dan upload artifacts
- [ ] `github-release` sukses dan membuat GitHub Release

## E. Validasi artifacts release
Di halaman GitHub Release, pastikan ada:
- [ ] APK (`Rofwin_1.0.0_arm64-v8a.apk` atau `-unsigned`)
- [ ] OBB (`main.100.com.rofwin.obb`)
- [ ] ZIP (`rofwin-1.0.0-github-release.zip`)
- [ ] `SHA256SUMS.txt`
- [ ] `RELEASE_NOTES.md`
- [ ] `release-manifest.json`

## F. Validasi runtime setelah install
- [ ] App terinstall normal di Android
- [ ] App name tampil sebagai **Rofwin**
- [ ] Icon launcher sudah sesuai branding baru
- [ ] OBB berada di `Android/obb/com.rofwin/`
- [ ] Input controls bisa dibaca dari OBB
- [ ] Installable components bisa dibaca dari OBB
- [ ] Wine addons bisa fallback ke OBB
- [ ] Profil Oppo CPH1823 default berjalan sesuai target

## G. One-command local preparation
Untuk menyiapkan bundle release lokal sekali jalan:
```bash
./scripts/prepare-github-release.sh
```

## H. Jika release gagal
- [ ] cek tab **Actions logs**
- [ ] cek `artifacts/release-manifest.json`
- [ ] cek `artifacts/RELEASE_NOTES.md`
- [ ] cek file release notes di `releases/v1.0.0.md`
- [ ] cek secrets signing bila APK signed gagal
