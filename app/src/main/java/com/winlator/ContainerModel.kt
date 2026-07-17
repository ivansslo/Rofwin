package com.winlator

import kotlinx.serialization.Serializable

@Serializable
data class EnvVar(val key: String, val value: String)

@Serializable
enum class DXWrapper(val displayName: String) {
    WINE_D3D("WineD3D (OpenGL based, best compatibility on low-end Mali)"),
    DXVK("DXVK (Vulkan based, heavy for Mali-G72)"),
    VKD3D("VKD3D (Vulkan based Direct3D 12)")
}

@Serializable
enum class OpenGLDriver(val displayName: String) {
    VIRGL("VirGL (Hardware OpenGL emulation via host, best on Mali-G72)"),
    TURNIP("Turnip (Adreno only, NOT compatible with CPH1823 Mali)"),
    LLVMPIPE("LLVMpipe (CPU-based software renderer, very slow)")
}

@Serializable
enum class Box64Preset(val displayName: String) {
    PERFORMANCE("Performance (Aggressive Dynarec, best FPS)"),
    BALANCED("Balanced (Recommended default)"),
    COMPATIBILITY("Compatibility (Strict instruction ordering, slow)")
}

@Serializable
data class WineContainer(
    val id: String,
    val name: String,
    val resolution: String = "800x600",
    val graphicsDriver: OpenGLDriver = OpenGLDriver.VIRGL,
    val dxWrapper: DXWrapper = DXWrapper.WINE_D3D,
    val box64Preset: Box64Preset = Box64Preset.PERFORMANCE,
    val audioDriver: String = "ALSA",
    val cpuAffinity: List<Int> = listOf(0, 1, 2, 3), // Helio P60 has 8 cores, let's bind first 4
    val envVars: List<EnvVar> = listOf(
        EnvVar("MESA_GL_VERSION_OVERRIDE", "3.1"),
        EnvVar("VIRGL_DEBUG", "no-framebuffer"),
        EnvVar("BOX64_DYNAREC", "1"),
        EnvVar("BOX64_NOPEDANTIC", "1"),
        EnvVar("WINE_HEAP_FACTOR", "2.0")
    )
)

object ContainerDefaults {
    val OPPO_TUNING_ENV_VARS = listOf(
        EnvVar("MESA_GL_VERSION_OVERRIDE", "3.1"),
        EnvVar("VIRGL_DEBUG", "no-framebuffer"),
        EnvVar("BOX64_DYNAREC", "1"),
        EnvVar("BOX64_NOPEDANTIC", "1"),
        EnvVar("WINE_HEAP_FACTOR", "2.0"),
        EnvVar("MALLOC_CONF", "background_thread:true")
    )

    val PRELOADED_CONTAINERS = listOf(
        WineContainer(
            id = "c1",
            name = "Oppo CPH1823 Default (Mali Tuning)",
            resolution = "800x600",
            graphicsDriver = OpenGLDriver.VIRGL,
            dxWrapper = DXWrapper.WINE_D3D,
            box64Preset = Box64Preset.PERFORMANCE,
            audioDriver = "ALSA",
            cpuAffinity = listOf(0, 1, 2, 3),
            envVars = OPPO_TUNING_ENV_VARS
        ),
        WineContainer(
            id = "c2",
            name = "Classic RPG Compatibility Container",
            resolution = "1024x768",
            graphicsDriver = OpenGLDriver.VIRGL,
            dxWrapper = DXWrapper.WINE_D3D,
            box64Preset = Box64Preset.BALANCED,
            audioDriver = "PulseAudio",
            cpuAffinity = listOf(4, 5, 6, 7), // Use BIG cores
            envVars = listOf(
                EnvVar("BOX64_DYNAREC", "1"),
                EnvVar("WINE_HEAP_FACTOR", "1.5")
            )
        )
    )

    val RESOLUTIONS = listOf(
        "640x480",
        "800x600",
        "1024x768",
        "1280x720",
        "1366x768"
    )

    val AUDIO_DRIVERS = listOf("ALSA", "PulseAudio")
}
