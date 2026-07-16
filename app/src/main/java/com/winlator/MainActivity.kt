package com.winlator

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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        DashboardScreen(
                            onLaunchContainer = { container, profile ->
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
