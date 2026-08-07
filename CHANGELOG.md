# Changelog

## v1.8.3 — Hotfix FC setelah boot (Double-Scroll)
- **FIX UTAMA (dari log crash user)**: `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints` — Desktop Icon Grid punya `.verticalScroll()` GANDA dalam satu rantai modifier; scrollable dalam diukur dengan max-height tak hingga → FC deterministik tepat setelah boot (Safe Mode pun tak menolong, karena grid ikon selalu dirender). Duplikat dihapus.
- Semua perbaikan v1.8.2 dipertahankan (anti-ANR, SecureBox, SSH TOFU pinning, WebView guard, namespace com.rofwin, CI Java 21)
- versionCode 183


## v1.8.2 — Security & Anti-ANR (rescue build)
- **Anti-ANR (akar masalah "stuck/lanjut macet")**: restore sesi & autosave tiap 5 dtk yang sebelumnya merakit/parse JSON besar di UI thread kini dipindah ke thread IO/Default (snapshot di Main → rakit di Default → tulis di IO)
- **Anti-FC WebView**: provider WebView rusak/hilang di ROM tertentu kini menampilkan pesan fallback, bukan force close; hardening `allowFileAccess/allowContentAccess = false`
- **SSH OCI Bridge anti-MITM**: host-key pinning TOFU (trust-on-first-use) — fingerprint SHA-256 server dipin & diverifikasi tiap koneksi; perubahan fingerprint = koneksi ditolak
- **API key AI terenkripsi**: disimpan AES-256/GCM via Android Keystore (SecureBox); nilai legacy plaintext tetap terbaca & otomatis migrasi saat diedit
- **Manifest**: `allowBackup=false` (lindungi key & sesi dari ekstraksi backup), `usesCleartextTraffic=false` (wajib HTTPS)
- **Build/CI**: workflow kini Java 21 + SDK 35 (sebelumnya Java 17 + SDK 34 → build gagal dengan `compileSdk 35`/`jvmTarget 21`); JitPack dihapus (JSch dari Maven Central)
- **Namespace** `com.winlator` → **`com.rofwin`** (konsisten dengan applicationId)
- **R8/minify + shrinkResources aktif** untuk release (rules aman untuk serialization/JSch/enum)
- **Housekeeping**: `metadata.json` (artefak AI Studio) dihapus; `FUNDING.yml` dibersihkan dari akun upstream; README ditulis ulang sesuai kondisi repo
- versionCode 182


## v1.8.1 — Recovery & Diagnosis (anti-FC total)
- Error boundary: exception komposisi desktop TAMPIL DI LAYAR (+ tombol salin log), bukan FC senyap
- Crash Shield JVM → prefs RofwinCrash + filesDir/last_crash.txt + Panel Crash di Dashboard (BAGIKAN LOG / HAPUS & NORMAL)
- Breadcrumb crumbs: A:onCreate > A:startPressed > D1:composed > R:start > R:ok > D2:live
- Safe Mode (otomatis setelah crash): skip restore sesi, bubble & pinned lanjutan off, banner oranye + pulihkan normal; juga tile Safe Mode di Quick Settings
- Taskbar: combinedClickable + DropdownMenu (Popup) → detectTapGestures + panel Box biasa (anti crash Popup ColorOS)
- Dialog overlay auto frame-1 DIHAPUS → tile Overlay manual di Quick Settings
- Bubble AI: shadow dihapus, dimatikan saat Safe Mode
- SEMUA fitur v1.7.0 dipertahankan: MT5 Setup (download nyata) + login + WebTerminal, VM Builder + Auth + OCI Bridge SSH (JSch), APK Studio, AI Bubble/plugins, critical text, landscape, sesi autosave
- versionCode 181


## v1.8.0 — Pro Foundation (basis stabil 1.6.0)
- Kode dikembalikan ke v1.6.0 yang terbukti OK di device; SEMUA fitur di-porting dengan aturan anti-FC
- Auto-popup overlay frame-1 DIHAPUS (kini tile Overlay manual di QS); menu taskbar DropdownMenu→panel Box biasa (detectTapGestures); bubble disederhanakan + ditunda 1,5 dtk
- Dipertahankan: landscape, taskbar move/lock, AI bubble+plugins, critical text, MT5 Setup (download nyata), MT5 login + WebTerminal REAL, VM Builder + Auth + OCI Bridge SSH nyata, APK Studio, crash shield + breadcrumbs + CrashAlertPanel, kernel-aware CPH1823 + OBB +50MB
- versionCode 180


## v1.7.2 — Hotfix & Diagnostic (Force Close v2)
- **Dialog overlay DITUNDA 1,5 dtk** setelah desktop live — tersangka utama FC frame-1 di ColorOS (plugin popup saat boot desktop)
- **Breadcrumb tracer NYATA**: jejak init (A:onCreate > A:startPressed > D1 > R > T > D2 > S) tersimpan — posisi mati terbaca setelah crash
- **CrashAlertPanel di Dashboard**: log crash + jejak tampil langsung saat app dibuka — tombol BAGIKAN LOG (share intent), MULAI SAFE MODE, HAPUS
- Safe Mode lebih minimal: bubble & pinned taskbar disembunyikan, bubble default mati
- (re-apply penuh seluruh perbaikan v1.7.1: Crash Shield, Safe Mode, auto low-RAM, OBB tuning)
- versionCode 172

## v1.7.1 — Stability & OBB (Anti Force-Close)
- Crash Shield NYATA: FC ditangkap → stack trace ke storage → boot berikutnya Safe Mode (loop FC putus)
- Safe Mode: skip restore sesi + paksa low-RAM + banner penyebab crash (Pulihkan/Hapus log)
- Auto low-RAM kernel-aware CPH1823 (isLowRamDevice/memoryClass/RAM ≤4,8GB) + largeHeap + resizeableActivity
- Data OBB +50MB (`main.171.com.rofwin.obb`): kernel_tuning.json dibaca on-demand (tick/candle/autosave/bubble) — data pack keluar heap
- Pacing adaptif low-RAM: tick 2600ms, candle 40, autosave 10 dtk; status OBB & mode kernel di boot screen
- versionCode 171

## v1.7.0 — Pro Bridge Edition
- Landscape NYATA semua device (manifest fullUser + tile Landscape QS + jendela adaptif wideScreen)
- Taskbar interaktif NYATA: long-press → Buka/Geser◄►/Pin-Unpin/🔒Kunci (tersimpan di sesi)
- Izin "Tampil di atas aplikasi lain" diminta saat app dibuka (SYSTEM_ALERT_WINDOW)
- AI Bubble (drag bebas + badge critical) & 4 AI Plugins modular (toggle, tersimpan)
- Deteksi Critical text: journal MT5, chat AI, log build, output SSH → Notification Center
- VM Builder: OS image Windows (sim) + Rofwin Auth (integrasi) + **OCI Bridge SSH NYATA (JSch)** — test/perintah/sync sesi → VM Oracle
- MT5 Setup: download NYATA mt5setup.exe (HTTP + progress) + install ke Program Files (live badge)
- MT5 Login akun (sim + 🌐 WebTerminal REAL untuk akun live asli); strip "bot BEKERJA" di editor
- APK Studio: pipeline compile Android APK (sim realistis, error dikenali critical) → build nyata via OCI
- Ikon desktop bisa scroll; 3 tool baru di desktop/Start/Quick Launch
- versionCode 170

## v1.6.0 — AI + Compact + Persistence
- UI compact untuk CPH1823 (~360dp): jendela 0.96f×0.72f, panel 0.8–0.94f, taskbar tengah horizontalScroll
- **ROC AI** (jendela baru): assistant trading/coding/compile/EA — online (OpenAI-compatible, key user-side saja) + offline brain + market digest + generator EA ke `D:\Work`
- Sinkron otomatis repo `ivansslo/rocagents` (git tree HEAD → FS + README terbaru)
- Git Bash menjadi terminal **rocd multi-OS** (`ivansslo/rocd`): ubuntu/debian/alpine/fedora/arch — list/ps/create/start/stop/enter/remove
- **Persistensi sesi unlimited**: autosave tiap 5 dtk ke storage, restore penuh saat boot (FS, file, EA, balance, posisi, history, journal)
- MT5 lebih full: tick engine global (EA jalan walau jendela tertutup), SL/TP auto-close per tick, dialog New Order F9 (lot + SL/TP), overlay MA7/MA21, state trading shared
- versionCode 160

## v1.5.0 — Coder & Trader Edition
- Rofwin Code (multi-tab, autosave, Find & Replace, ▶Run Python mini, ⚙Compile MQL5)
- Repo `rocagents` tertanam di FS + `C:\MQL5\{Experts,Include,Scripts}`
- MT5 24 simbol, candlestick, positions live P/L, Navigator EA Attach/Detach, Toolbox lengkap
- WineCfg + Winetricks; taskbar absolute center

## v1.4.0 — Windows 11 Edition
- Palet Win11 + Bloom gradient, jendela rounded + shadow, taskbar 3-align dengan jam hidup
- Start Menu W11 (search 13 apps, power Shut down/Restart), Widgets, Quick Settings (volume & brightness NYATA), Notification Center
- Explorer Up + address bar editable + mkdir sinkron FS, Task Manager live, Registry editor, WinRAR extract, Python eval, Git Bash & SSH interaktif

## v1.3.0 — Desktop Windows Experience
- Caption buttons lengkap (min/max/fullscreen/close) + drag terkunci saat maximize
- Terminal cmd + PowerShell (~30 perintah), MT5 7 pair + Sell/Buy, MQL5 editor Compile, Chrome navigasi
- Low-RAM Mode toggle

## v1.2.0
- Source AI Studio terbaru (Kotlin DSL + Compose), namespace app `com.rofwin`

## v1.0.0
- Initial Rofwin release
- Branding updated from base source to **Rofwin**
- Default tuning for **Oppo CPH1823 / Mali-G72**
- Runtime **OBB integration** for:
  - `input_controls`
  - `installable_components`
  - `wine_addons`
- GitHub Actions for APK / OBB / ZIP build and release
- New neon green tech logo and launcher icon
- Release packaging scripts and build documentation
