# Changelog

## v1.0.1
- **Restrukturisasi project Android** ke root repo: `settings.gradle.kts` (rootProject `Rofwin`) + modul `:app` (Kotlin DSL) — menggantikan nested project lama (`app/app`)
- UI modul `:app` kini berbasis **Kotlin + Jetpack Compose** (package `com.winlator`, app id `com.rofwin`)
- **Toolchain fix**: AGP `9.1.1` (tidak tersedia di Maven) → **8.13.1**; Gradle wrapper resmi **8.14.2** di root; CI pakai **JDK 21** + SDK **android-35** + build-tools **35.0.0**
- `read_android_metadata.py` dukung Kotlin DSL; script build/fetch/release pakai path modul baru (`app/src/main/assets`)
- Gradle wrapper sekarang di root (`./gradlew`) — workflow + docs diperbarui

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
