# Changelog

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
