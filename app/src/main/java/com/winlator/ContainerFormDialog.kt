package com.winlator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerFormDialog(
    container: WineContainer?, // null for create
    onDismiss: () -> Unit,
    onSave: (WineContainer) -> Unit
) {
    var name by remember { mutableStateOf(container?.name ?: "Mali Optimized Container") }
    var resolution by remember { mutableStateOf(container?.resolution ?: "800x600") }
    var graphicsDriver by remember { mutableStateOf(container?.graphicsDriver ?: OpenGLDriver.VIRGL) }
    var dxWrapper by remember { mutableStateOf(container?.dxWrapper ?: DXWrapper.WINE_D3D) }
    var box64Preset by remember { mutableStateOf(container?.box64Preset ?: Box64Preset.PERFORMANCE) }
    var audioDriver by remember { mutableStateOf(container?.audioDriver ?: "ALSA") }

    val envVars = remember {
        mutableStateListOf<EnvVar>().apply {
            addAll(container?.envVars ?: ContainerDefaults.OPPO_TUNING_ENV_VARS)
        }
    }

    var newEnvKey by remember { mutableStateOf("") }
    var newEnvVal by remember { mutableStateOf("") }

    var resolutionMenuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = if (container == null) "Create New Container" else "Configure ${container.name}",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Settings Form
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Auto-Tuning Assistant Header Banner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SecondaryTeal, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryTeal)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Oppo CPH1823 Tuning Assistant",
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTeal,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Your Oppo F9 runs Helio P60 (low-end Mali-G72). Click below to apply optimal presets (800x600 resolution, VirGL driver, WineD3D and optimized memory allocation configs).",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        resolution = "800x600"
                                        graphicsDriver = OpenGLDriver.VIRGL
                                        dxWrapper = DXWrapper.WINE_D3D
                                        box64Preset = Box64Preset.PERFORMANCE
                                        envVars.clear()
                                        envVars.addAll(ContainerDefaults.OPPO_TUNING_ENV_VARS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Apply Oppo F9 Tuning Presets", fontSize = 11.sp, color = Color.Black)
                                }
                            }
                        }
                    }

                    // Container Name
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Container Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Resolution Selection
                    item {
                        Box {
                            OutlinedTextField(
                                value = resolution,
                                onValueChange = {},
                                label = { Text("Screen Resolution") },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionMenuExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { resolutionMenuExpanded = !resolutionMenuExpanded },
                                enabled = false, // Disable typing, click to trigger dropdown
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.White,
                                    disabledBorderColor = Color.Gray,
                                    disabledLabelColor = Color.LightGray
                                )
                            )
                            // Custom dropdown layer
                            DropdownMenu(
                                expanded = resolutionMenuExpanded,
                                onDismissRequest = { resolutionMenuExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f).background(DarkSurfaceVariant)
                            ) {
                                ContainerDefaults.RESOLUTIONS.forEach { res ->
                                    DropdownMenuItem(
                                        text = { Text(res, color = Color.White) },
                                        onClick = {
                                            resolution = res
                                            resolutionMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Graphics / OpenGL Driver
                    item {
                        Column {
                            Text("Graphics Driver (OpenGL emulation)", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OpenGLDriver.values().forEach { driver ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { graphicsDriver = driver }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = graphicsDriver == driver, onClick = { graphicsDriver = driver })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(driver.displayName, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // DX Wrapper Selection
                    item {
                        Column {
                            Text("DX Wrapper (DirectX translation)", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            DXWrapper.values().forEach { wrapper ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { dxWrapper = wrapper }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = dxWrapper == wrapper, onClick = { dxWrapper = wrapper })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(wrapper.displayName, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Box64 Emulator Preset
                    item {
                        Column {
                            Text("Box64 CPU Preset (Helio P60 emulation mode)", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box64Preset.values().forEach { prst ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { box64Preset = prst }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = box64Preset == prst, onClick = { box64Preset = prst })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(prst.displayName, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Audio Driver Selection
                    item {
                        Column {
                            Text("Audio Server Subsystem", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            ContainerDefaults.AUDIO_DRIVERS.forEach { driver ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { audioDriver = driver }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = audioDriver == driver, onClick = { audioDriver = driver })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(driver, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Environmental Variables Configuration Panel
                    item {
                        Column {
                            Text("Environment Variables (Optimized)", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Adding input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = newEnvKey,
                                    onValueChange = { newEnvKey = it },
                                    placeholder = { Text("Key", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfaceVariant),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                                )
                                TextField(
                                    value = newEnvVal,
                                    onValueChange = { newEnvVal = it },
                                    placeholder = { Text("Value", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1.2f).height(48.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfaceVariant),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                                )
                                IconButton(
                                    onClick = {
                                        if (newEnvKey.isNotBlank()) {
                                            envVars.add(EnvVar(newEnvKey.trim().uppercase(Locale.ROOT), newEnvVal.trim()))
                                            newEnvKey = ""
                                            newEnvVal = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Env Var", tint = PrimarySky)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Display current env vars
                            envVars.forEach { ev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(0.8f)) {
                                        Text(ev.key, color = PrimarySky, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(ev.value, color = Color.White, fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = { envVars.remove(ev) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    WineContainer(
                                        id = container?.id ?: "c_" + System.currentTimeMillis(),
                                        name = name.trim(),
                                        resolution = resolution,
                                        graphicsDriver = graphicsDriver,
                                        dxWrapper = dxWrapper,
                                        box64Preset = box64Preset,
                                        audioDriver = audioDriver,
                                        envVars = envVars.toList()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save Container")
                    }
                }
            }
        }
    }
}
