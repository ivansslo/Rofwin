# Rofwin

Rofwin adalah aplikasi Android **Jetpack Compose** bernuansa desktop Windows 11 untuk device
low-end (fokus tuning: **Oppo CPH1823 / Oppo F9, Helio P60, Mali-G72, RAM 4 GB**), turunan
konsep dari ekosistem Winlator (lihat `NOTICE.md`).

> **Status penting:** source Winlator penuh (Wine/Box64 native) **tidak** lagi dibundel di repo
> ini sejak v1.2.0 — `app/` adalah aplikasi Compose mandiri. Folder native `gladio/` dan
> `android_alsa/` adalah referensi upstream dan **tidak** ikut terbuild.

## Fitur utama (v1.8.2)
- **Desktop Windows 11 simulasi**: taskbar interaktif (pin/geser/kunci), Start Menu, Quick Settings, Widget, Notif Center, landscape semua device
- **Terminal sim (cmd/PowerShell)**, File Explorer sim, Registry sim, Task Manager sim
- **MT5 sim**: tick engine 24 simbol, order F9, SL/TP, EA auto-trade sim, chart MA overlay
- **ROC AI assistant**: online (OpenAI-compatible, default `api.groq.com`, key milik user — **tersimpan terenkripsi Android Keystore**) + offline brain, EA generator, plugin, deteksi teks kritis
- **MT5 Setup**: unduh `mt5setup.exe` asli dari CDN MetaQuotes + WebTerminal via WebView
- **VM Builder + OCI Bridge**: eksekusi **SSH nyata** (JSch) ke VM Oracle Cloud — dengan **host-key pinning TOFU anti-MITM**
- **Browser WebView** (dengan fallback aman bila provider WebView rusak)
- **Crash Shield + Safe Mode + breadcrumb** (panel crash di Dashboard, bagikan log)
- **Sesi tersimpan** (autosave 5 dtk, **anti-ANR** — proses berat di luar UI thread)

## Build

**Syarat:** JDK **21**, Android SDK Platform **35**, Build-Tools **35.0.0**.

```bash
export ANDROID_HOME=/path/to/sdk
./gradlew :app:assembleRelease   # atau :app:assembleDebug
```

Output:
- release: `app/build/outputs/apk/release/app-release-unsigned.apk` (signing via secrets di CI)
- debug: `app/build/outputs/apk/debug/app-debug.apk` (debug-signed, langsung bisa diinstall)

> App lama yang FC/stuck: **uninstall dulu** sebelum install build baru (tanda tangan bisa beda
> dan data sesi lama tetap kompatibel dibaca).

## GitHub Actions
- `android-build.yml` — auto build pada push/PR/tag/manual (Java 21, SDK 35)
- `github-release.yml` — publish release saat push tag `v*` (signing via `secrets.ANDROID_*`)
- `sync-from-winlator-release.yml` — sync dari repo upstream bila diperlukan

## Struktur
```
app/                  # aplikasi utama (namespace com.rofwin, applicationId com.rofwin)
  src/main/java/com/rofwin/
    WineDesktopSim.kt     # desktop sim + MT5 + AI + SSH + browser
    DashboardScreen.kt    # dashboard/cockpit
    ContainerFormDialog.kt, ContainerModel.kt, InputProfileModel.kt
    MainActivity.kt       # Crash Shield JVM
    SecureBox.kt          # enkripsi API key (Android Keystore, v1.8.2)
    Theme.kt
gladio/, android_alsa/    # referensi native upstream (TIDAK terbuild)
input_controls/           # 43 profil kontrol game (.icp, JSON)
docs/                     # dokumen tuning & release
```

## Atribusi
Turunan/konsep dari `brunodev85/winlator`, `winlator-app`, `gladio`, `vortek` — lihat
`NOTICE.md` dan `LICENSE` (LGPL-2.1).
