# Alur Release Berantai: `ivansslo/winlator` ➜ `ivansslo/rofwin`

Dokumen ini menjelaskan cara agar setelah release dibuat di repo **`ivansslo/winlator`**, proses lanjut otomatis ke repo **`ivansslo/rofwin`**.

## File yang disiapkan
### Untuk repo upstream `ivansslo/winlator`
Salin file ini ke repo upstream:
- `templates/ivansslo-winlator-dispatch-rofwin.yml`

Tempat tujuan di repo upstream:
- `.github/workflows/dispatch-rofwin-after-release.yml`

### Untuk repo downstream `ivansslo/rofwin`
Workflow ini sudah disiapkan di repo ini:
- `.github/workflows/sync-from-winlator-release.yml`

## Cara kerja
1. Release di `ivansslo/winlator` dipublish.
2. Workflow upstream mengirim `repository_dispatch` ke `ivansslo/rofwin`.
3. Workflow `sync-from-winlator-release.yml` di `rofwin` jalan otomatis.
4. `rofwin` fetch payload dari:
   - repo: `https://github.com/ivansslo/winlator.git`
   - ref/tag: tag release upstream
5. `rofwin` build APK/OBB/ZIP lalu publish release di repo `ivansslo/rofwin`.

## Secret yang dibutuhkan di repo upstream (`ivansslo/winlator`)
Tambahkan secret berikut di:
**Settings → Secrets and variables → Actions**

- `ROFWIN_REPO_DISPATCH_TOKEN`

Token ini dipakai untuk memanggil API GitHub ke repo `ivansslo/rofwin`.

### Saran scope token
Jika pakai **classic PAT**, gunakan scope minimal:
- `repo`

Jika pakai **fine-grained PAT**, beri akses ke repo `ivansslo/rofwin` setidaknya:
- Contents: Read and write
- Actions: Read and write
- Metadata: Read

## Variable opsional di repo upstream
- `ROFWIN_REPO_OWNER` = `ivansslo`
- `ROFWIN_REPO_NAME` = `rofwin`

Kalau tidak diisi, workflow default ke nilai di atas.

## Secret yang dibutuhkan di repo downstream (`ivansslo/rofwin`)
Agar hasil build menjadi **signed release APK**, isi juga secret berikut di repo `rofwin`:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Kalau secret signing belum diisi, build tetap bisa jalan tetapi APK bisa menjadi unsigned.

## Trigger manual
Kalau mau test tanpa menunggu release upstream, Anda bisa:

### Di repo upstream
Jalankan workflow `dispatch-rofwin-after-release` secara manual dan isi:
- `tag_name`
- `release_name` (opsional)

### Di repo downstream
Jalankan workflow `sync-from-winlator-release` secara manual dan isi:
- `upstream_tag`
- `upstream_repo` (opsional)
- `release_tag` (opsional)
- `release_name` (opsional)

## Nama tag di Rofwin
Secara default, workflow downstream akan membuat tag release:
```text
<upstream_tag>-rofwin
```

Contoh:
- upstream: `v11.0`
- downstream release tag: `v11.0-rofwin`

## Catatan
Workflow chain ini tidak mengubah source repo upstream. Ia hanya memakai tag release upstream sebagai patokan fetch payload saat build di `rofwin`.
