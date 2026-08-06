# ===== Rofwin v1.8.2 — ProGuard/R8 rules =====

# Kode aplikasi: keep penuh (Compose + kotlinx.serialization memakai refleksi)
-keep class com.rofwin.** { *; }
-keepclassmembers class com.rofwin.** { *; }

# kotlinx.serialization — serializer & Companion wajib utuh
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Enum (DesktopWindow.valueOf dipakai saat restore sesi)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# JSch (SSH) — library pihak ketiga, keep agar tidak dipangkas keliru
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
