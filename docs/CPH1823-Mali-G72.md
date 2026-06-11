# Profil Setelan Rofwin — Oppo F9 / F9 Pro (CPH1823)

> Panduan tuning untuk perangkat **GPU ARM Mali-G72 MP3** dengan **SoC MediaTek Helio P60 (MT6771)** dan **RAM 4 GB**, Android 8.1 Oreo.
>
> Di fork **Rofwin**, default container baru untuk perangkat low-end Mali diarahkan ke **Vulkan = None**, **OpenGL = VirGL**, **DX Wrapper = WineD3D**, **Box64 = Performance**, dan **resolusi 800x600**.

---

## 1. Pahami batasan perangkat ini (baca dulu)

| Komponen | Spesifikasi | Implikasi untuk Rofwin |
|---|---|---|
| GPU | **Mali-G72 MP3 @800 MHz** | **TIDAK bisa pakai Turnip** (Turnip = driver Vulkan khusus Adreno/Qualcomm). |
| SoC | Helio P60, 4× A73 + 4× A53 @2.0 GHz | Mid-range 2018, tanpa AVX → emulasi x86_64 berat. |
| RAM | 4 GB | Mudah kehabisan memori → wajib hemat. |
| OS | Android 8.1 | Dukungan Vulkan driver Mali terbatas/lawas. |

**Konsekuensi penting:**
- Jalur performa modern **Turnip + DXVK (Vulkan)** TIDAK tersedia di perangkat ini.
- Grafis hanya bisa lewat **VirGL** atau **LLVMpipe** (rendering via CPU/OpenGL), yang lambat untuk 3D.
- **Target realistis:** aplikasi Windows ringan, installer, emulator, dan **game 2D / lawas (pra-2008)**. Game 3D modern tidak akan berjalan layak — ini batasan hardware, bukan setelan.

---

## 2. Setelan Container yang direkomendasikan

Buat container baru, lalu atur:

### Tab Graphics
| Opsi | Nilai | Catatan |
|---|---|---|
| **Graphics Driver** | `VirGL` | Pilihan utama untuk Mali. Jika tidak jalan, coba `LLVMpipe`. **JANGAN** Turnip. |
| **DX Wrapper** | `WineD3D (OpenGL)` | Lebih aman di Mali daripada DXVK (yang butuh Vulkan). |
| **Screen Resolution** | `800x600` atau `960x540` | Resolusi rendah = beban GPU jauh lebih ringan. |

### Tab Advanced (Box64)
| Opsi | Nilai |
|---|---|
| **Box64 Version** | `0.3.7` (terbaru). Jika crash, turun ke `0.3.5`. |
| **Box64 Preset** | `Performance` untuk app stabil; `Stability`/`Compatibility` jika sering crash. |

### Tab Wine / System
| Opsi | Nilai | Catatan |
|---|---|---|
| **RAM container** | sekecil mungkin | 4 GB total; sisakan untuk Android. |
| **Process Affinity / CPU** | aktifkan semua core | Manfaatkan 4× A73. |

---

## 3. Environment Variables yang berguna

Tambahkan di **Container Settings → Environment Variables**:

```
MESA_EXTENSION_MAX_YEAR=2003      # bantu game lawas terbuka di VirGL/LLVMpipe
WINEDEBUG=-all                    # matikan log Wine → sedikit lebih ringan
ZINK_DESCRIPTORS=lazy             # hanya jika mencoba Zink (umumnya tidak untuk Mali)
```

> `MESA_EXTENSION_MAX_YEAR=2003` sangat membantu game tua (DirectX 7–9 awal) agar tidak gagal start.

---

## 4. Setelan per-shortcut (game)

Gunakan shortcut di home screen Rofwin untuk setelan per-game:
- Aktifkan **Force Fullscreen** untuk game resolusi rendah agar tampil benar.
- Game Unity tua: tambah exec argument `-force-gfx-direct` + preset Box64 `Stability`.
- Audio crackling: naikkan **average latency** audio ke ~90 ms.

---

## 5. Checklist troubleshooting cepat

| Gejala | Coba |
|---|---|
| Layar hitam / crash saat start | Ganti VirGL → LLVMpipe; DX Wrapper → WineD3D; turunkan resolusi. |
| Out of memory / app tertutup | Tutup app lain, kecilkan RAM container, resolusi 800x600. |
| Game tua tidak mau buka | Set `MESA_EXTENSION_MAX_YEAR=2003`. |
| FPS sangat rendah di 3D | Ini batasan Mali tanpa Turnip — pilih game lebih ringan/2D. |
| .NET app tidak jalan | Install **Wine Mono** dari Start Menu → System Tools → Installers. |

---

## 6. Apa yang TIDAK perlu dicoba (buang waktu di Mali-G72)

- ❌ Driver **Turnip** (semua versi) — tidak mendukung GPU Mali.
- ❌ **DXVK / VKD3D** untuk game berat — bergantung Vulkan yang lemah/absen di Mali Android 8.1.
- ❌ Game 3D AAA / pasca-2010 — di luar kemampuan SoC + GPU ini.

---

## 7. Rekomendasi jujur

CPH1823 cocok untuk: aplikasi produktivitas ringan, software lawas, emulator, dan game 2D/retro lewat Wine. Untuk pengalaman gaming 3D yang layak di Rofwin, dibutuhkan perangkat ber-GPU **Qualcomm Adreno** (mis. seri Snapdragon 8xx) agar bisa memakai Turnip + DXVK.

*Profil ini diadaptasi untuk repo Rofwin yang diturunkan dari ivansslo/winlator. Setelan bersifat panduan; sesuaikan per-game.*
