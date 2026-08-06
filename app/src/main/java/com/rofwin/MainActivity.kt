package com.rofwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ===== v1.8.1 — Crash Shield: tangkap SEMUA uncaught exception sebelum app mati =====
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                val txt = ("THREAD: " + thread.name + "\n" + sw.toString()).take(6000)
                val p = getSharedPreferences("RofwinCrash", MODE_PRIVATE)
                p.edit()
                    .putString("last_crash", txt)
                    .putLong("crash_time", System.currentTimeMillis())
                    .putBoolean("safe_next_mode", true)
                    .commit()
                java.io.File(filesDir, "last_crash.txt").writeText(txt)
            } catch (_: Exception) {}
            prevHandler?.uncaughtException(thread, throwable)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try { crumb(this, "A:onCreate") } catch (_: Exception) {}

        setContent {
            RofwinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    var activeSessionContainer by remember { mutableStateOf<WineContainer?>(null) }
                    var activeSessionProfile by remember { mutableStateOf<InputControlsProfile?>(null) }

                    if (activeSessionContainer != null && activeSessionProfile != null) {
                        // Immersive Wine Windows Simulator!
                        WineDesktopSim(
                            container = activeSessionContainer!!,
                            profile = activeSessionProfile!!,
                            onClose = {
                                activeSessionContainer = null
                                activeSessionProfile = null
                            }
                        )
                    } else {
                        // Main Cockpit Dashboard
                        val act = this@MainActivity
                        DashboardScreen(
                            onLaunchContainer = { container, profile ->
                                try { crumb(act, "A:startPressed") } catch (_: Exception) {}
                                activeSessionContainer = container
                                activeSessionProfile = profile
                            }
                        )
                    }
                }
            }
        }
    }
}
