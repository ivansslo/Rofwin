package com.winlator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Window Type
enum class DesktopWindow {
    NONE, MY_COMPUTER, REGISTRY_EDITOR, TASK_MANAGER, COMMAND_PROMPT, DX_DIAG, GAME_SIMULATION
}

data class SimFile(val name: String, val isDirectory: Boolean = false, val size: String = "1 KB")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineDesktopSim(
    container: WineContainer,
    profile: InputControlsProfile,
    onClose: () -> Unit
) {
    var isBooting by remember { mutableStateOf(true) }
    var bootProgress by remember { mutableFloatStateOf(0f) }

    // Desktop Window States
    var openWindow by remember { mutableStateOf(DesktopWindow.NONE) }
    var startMenuOpen by remember { mutableStateOf(false) }

    // Window offsets (simple dragging support)
    var windowOffset by remember { mutableStateOf(Offset(50f, 100f)) }

    // File Explorer State
    var currentPath by remember { mutableStateOf("C:\\") }
    
    val simulatedFiles = remember {
        mutableStateMapOf<String, List<SimFile>>().apply {
            putAll(
                mapOf(
                    "C:\\" to listOf(
                        SimFile("Windows", true),
                        SimFile("Program Files", true),
                        SimFile("users", true),
                        SimFile("boot.ini", false, "256 B")
                    ),
                    "C:\\Windows" to listOf(
                        SimFile("System32", true),
                        SimFile("wine.inf", false, "12 KB"),
                        SimFile("regedit.exe", false, "45 KB")
                    ),
                    "C:\\Windows\\System32" to listOf(
                        SimFile("kernel32.dll", false, "820 KB"),
                        SimFile("user32.dll", false, "640 KB"),
                        SimFile("gdi32.dll", false, "310 KB")
                    ),
                    "C:\\Program Files" to listOf(
                        SimFile("DirectX", true),
                        SimFile("WineD3D", true)
                    ),
                    "C:\\Program Files\\DirectX" to listOf(
                        SimFile("dxgi.dll", false, "120 KB"),
                        SimFile("d3d11.dll", false, "450 KB")
                    ),
                    "C:\\Program Files\\WineD3D" to listOf(
                        SimFile("wined3d.dll", false, "980 KB")
                    ),
                    "C:\\users" to listOf(
                        SimFile("Administrator", true)
                    ),
                    "C:\\users\\Administrator" to listOf(
                        SimFile("My Documents", true),
                        SimFile("Desktop", true)
                    ),
                    "D:\\" to listOf(
                        SimFile("Games", true),
                        SimFile("Downloads", true)
                    ),
                    "D:\\Games" to listOf(
                        SimFile("GTA 5 (Simulated)", false, "65 GB"),
                        SimFile("Skyrim (Simulated)", false, "12 GB"),
                        SimFile("Fallout 3 (Simulated)", false, "8 GB"),
                        SimFile("FlatOut 2 (Simulated)", false, "4 GB")
                    ),
                    "D:\\Downloads" to listOf(
                        SimFile("winetricks.exe", false, "3 MB")
                    )
                )
            )
        }
    }

    var newFileName by remember { mutableStateOf("") }
    var fileTypeFolder by remember { mutableStateOf(false) }

    // Registry State
    val registryKeys = remember {
        mutableStateListOf(
            Pair("HKCU\\Software\\Wine\\Direct3D\\csmt", "0x00000001 (1)"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\MaxShaderModel", "3"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\OffscreenRenderingMode", "fbo"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\StrictDrawOrdering", "disabled"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\UseGLSL", "enabled")
        )
    }
    var editingRegKey by remember { mutableStateOf<Pair<String, String>?>(null) }
    var regValueInput by remember { mutableStateOf("") }

    // Task Manager State
    val processes = remember {
        mutableStateListOf(
            Triple("explorer.exe", "Active", "12 MB"),
            Triple("services.exe", "Active", "4 MB"),
            Triple("wineserver", "Active", "32 MB"),
            Triple("box64", "Active", "48 MB"),
            Triple("virgl_renderer", "Active", "120 MB")
        )
    }

    // Command Prompt Logs
    val commandLogs = remember {
        mutableStateListOf(
            "Rofwin Wine Environment [Version 1.0.0]",
            "(C) Copyright ivansslo / Rofwin. All rights reserved.",
            "",
            "Type 'help' to list available commands.",
            ""
        )
    }
    var terminalInput by remember { mutableStateOf("") }

    // Game Simulation State
    var selectedGameName by remember { mutableStateOf("") }
    var simulatedFps by remember { mutableIntStateOf(0) }
    var gameTimeMs by remember { mutableLongStateOf(0L) }
    var buttonPressedLog by remember { mutableStateOf("Press virtual gamepad buttons") }

    // Booting sequence
    LaunchedEffect(Unit) {
        while (bootProgress < 1f) {
            delay(100)
            bootProgress += 0.05f
        }
        isBooting = false
    }

    // simulated real-time stats (FPS, Temps)
    LaunchedEffect(openWindow, selectedGameName) {
        if (openWindow == DesktopWindow.GAME_SIMULATION) {
            val baseFps = when (container.resolution) {
                "640x480" -> 60
                "800x600" -> 50
                "1024x768" -> 35
                "1280x720" -> 28
                else -> 18
            }
            // Performance preset yields more FPS
            val multiplier = when (container.box64Preset) {
                Box64Preset.PERFORMANCE -> 1.2f
                Box64Preset.BALANCED -> 1.0f
                Box64Preset.COMPATIBILITY -> 0.7f
            }

            while (openWindow == DesktopWindow.GAME_SIMULATION) {
                delay(800)
                simulatedFps = ((baseFps + (-3..3).random()) * multiplier).roundToInt().coerceAtLeast(5)
                gameTimeMs += 800
            }
        }
    }

    if (isBooting) {
        // Immersive Booting Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Laptop,
                    contentDescription = "Rofwin",
                    tint = PrimarySky,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ROFWIN EMULATOR",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Optimized for Helio P60 (Mali-G72)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(32.dp))
                LinearProgressIndicator(
                    progress = bootProgress,
                    color = SecondaryTeal,
                    trackColor = Color(0xFF1E293B),
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Initialising Wine + Box64 Dynarec... ${(bootProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }
    } else {
        // Desktop Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B), // Slate 800
                            Color(0xFF0F172A)  // Slate 900
                        )
                    )
                )
        ) {
            // Desktop Icon Grid
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DesktopIconButton(
                    name = "My Computer",
                    icon = Icons.Default.Computer,
                    onClick = { openWindow = DesktopWindow.MY_COMPUTER }
                )
                DesktopIconButton(
                    name = "Registry Editor",
                    icon = Icons.Default.Settings,
                    onClick = { openWindow = DesktopWindow.REGISTRY_EDITOR }
                )
                DesktopIconButton(
                    name = "Task Manager",
                    icon = Icons.Default.AlignVerticalBottom,
                    onClick = { openWindow = DesktopWindow.TASK_MANAGER }
                )
                DesktopIconButton(
                    name = "Command Prompt",
                    icon = Icons.Default.Terminal,
                    onClick = { openWindow = DesktopWindow.COMMAND_PROMPT }
                )
                DesktopIconButton(
                    name = "DirectX Diag (dxdiag)",
                    icon = Icons.Default.Info,
                    onClick = { openWindow = DesktopWindow.DX_DIAG }
                )
            }

            // Quick Launcher Panel for Games in Container (D:\Games)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color(0x33000000), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .width(140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "D:\\Games Shortcut",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf("GTA 5", "Skyrim", "Fallout 3", "FlatOut 2").forEach { game ->
                    Button(
                        onClick = {
                            selectedGameName = game
                            openWindow = DesktopWindow.GAME_SIMULATION
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySky.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(game, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Interactive Windows
            if (openWindow != DesktopWindow.NONE) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(windowOffset.x.roundToInt(), windowOffset.y.roundToInt()) }
                        .width(420.dp)
                        .height(350.dp)
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, PrimarySky, RoundedCornerShape(8.dp))
                ) {
                    Column {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimarySky)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        windowOffset += dragAmount
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getWindowIcon(openWindow),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = getWindowTitle(openWindow, selectedGameName),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            IconButton(
                                onClick = { openWindow = DesktopWindow.NONE },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                            }
                        }

                        // Window Content Area
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground)
                        ) {
                            when (openWindow) {
                                DesktopWindow.MY_COMPUTER -> {
                                    // File Explorer View
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Path bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkSurface)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (currentPath == "C:\\Windows\\System32") currentPath = "C:\\Windows"
                                                    else if (currentPath == "C:\\Windows") currentPath = "C:\\"
                                                    else if (currentPath == "D:\\Games") currentPath = "D:\\"
                                                    else if (currentPath == "C:\\Program Files\\DirectX" || currentPath == "C:\\Program Files\\WineD3D") currentPath = "C:\\Program Files"
                                                    else if (currentPath == "C:\\Program Files" || currentPath == "C:\\users") currentPath = "C:\\"
                                                    else if (currentPath == "C:\\users\\Administrator") currentPath = "C:\\users"
                                                },
                                                enabled = currentPath != "C:\\" && currentPath != "D:\\"
                                            ) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = currentPath,
                                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            )
                                        }

                                        // File Actions (Create / Delete / CRUD)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = newFileName,
                                                onValueChange = { newFileName = it },
                                                placeholder = { Text("New file name...", fontSize = 12.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = fileTypeFolder,
                                                    onCheckedChange = { fileTypeFolder = it }
                                                )
                                                Text("Folder", fontSize = 11.sp, color = Color.White)
                                            }
                                            Button(
                                                onClick = {
                                                    if (newFileName.isNotBlank()) {
                                                        val list = simulatedFiles[currentPath]?.toMutableList() ?: mutableListOf()
                                                        list.add(SimFile(newFileName, fileTypeFolder, if (fileTypeFolder) "Folder" else "1 KB"))
                                                        simulatedFiles[currentPath] = list
                                                        newFileName = ""
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("Add", fontSize = 11.sp)
                                            }
                                        }

                                        // Disk Selection list
                                        if (currentPath == "C:\\" || currentPath == "D:\\") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { currentPath = "C:\\" },
                                                    colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("C:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(32.dp))
                                                        Text("Local Disk (C:)", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { currentPath = "D:\\" },
                                                    colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("D:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(32.dp))
                                                        Text("OBB Space (D:)", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }

                                        // Files / Folders List
                                        val files = simulatedFiles[currentPath] ?: emptyList()
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(files) { file ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (file.isDirectory) {
                                                                currentPath = if (currentPath.endsWith("\\")) "$currentPath${file.name}" else "$currentPath\\${file.name}"
                                                            }
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = if (file.isDirectory) SecondaryTeal else Color.LightGray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(file.name, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(file.size, fontSize = 11.sp, color = TextSecondary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = {
                                                                val list = simulatedFiles[currentPath]?.toMutableList() ?: mutableListOf()
                                                                list.remove(file)
                                                                simulatedFiles[currentPath] = list
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                DesktopWindow.REGISTRY_EDITOR -> {
                                    // Regedit interface
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        Text(
                                            "Registry Editor (regedit.exe)",
                                            style = MaterialTheme.typography.titleSmall.copy(color = SecondaryTeal)
                                        )
                                        Text(
                                            "Tuning keys for Direct3D Rendering on Mali-G72:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        LazyColumn(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).background(DarkSurface)) {
                                            items(registryKeys) { key ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            editingRegKey = key
                                                            regValueInput = key.second
                                                        }
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(0.6f)) {
                                                        Text(key.first.substringAfterLast("\\"), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text(key.first, fontSize = 9.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    Text(key.second, fontSize = 11.sp, color = PrimarySky, modifier = Modifier.weight(0.4f), textAlign = TextAlign.End)
                                                }
                                            }
                                        }

                                        // Editing Key Overlay dialog
                                        editingRegKey?.let { key ->
                                            AlertDialog(
                                                onDismissRequest = { editingRegKey = null },
                                                title = { Text("Edit String Key", fontSize = 14.sp) },
                                                text = {
                                                    Column {
                                                        Text(key.first, fontSize = 11.sp, color = TextSecondary)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        TextField(
                                                            value = regValueInput,
                                                            onValueChange = { regValueInput = it },
                                                            singleLine = true
                                                        )
                                                    }
                                                },
                                                confirmButton = {
                                                    Button(onClick = {
                                                        val index = registryKeys.indexOfFirst { it.first == key.first }
                                                        if (index != -1) {
                                                            registryKeys[index] = Pair(key.first, regValueInput)
                                                        }
                                                        editingRegKey = null
                                                    }) {
                                                        Text("Save")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { editingRegKey = null }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                DesktopWindow.TASK_MANAGER -> {
                                    // Task Manager Sim
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Wine Task Manager", style = MaterialTheme.typography.titleSmall.copy(color = PrimarySky))
                                            Text("Mali-G72 GPU: ${if (selectedGameName.isNotEmpty()) "88%" else "12%"}", style = MaterialTheme.typography.labelSmall.copy(color = SecondaryTeal))
                                        }

                                        LazyColumn(modifier = Modifier.weight(1f).border(1.dp, Color.Gray)) {
                                            items(processes) { proc ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(DarkSurface)
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(proc.first, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text("Memory: ${proc.third}", fontSize = 10.sp, color = TextSecondary)
                                                    }
                                                    Button(
                                                        onClick = { processes.remove(proc) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("End Process", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                }
                                DesktopWindow.COMMAND_PROMPT -> {
                                    // Interactive Terminal Prompt
                                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(Color.Black)
                                                .padding(6.dp)
                                        ) {
                                            items(commandLogs) { log ->
                                                Text(
                                                    text = log,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.Green,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black)
                                                .padding(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("C:\\Windows\\System32>", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            TextField(
                                                value = terminalInput,
                                                onValueChange = { terminalInput = it },
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Black,
                                                    unfocusedContainerColor = Color.Black,
                                                    focusedTextColor = Color.Green,
                                                    unfocusedTextColor = Color.Green
                                                ),
                                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (terminalInput.isNotBlank()) {
                                                        val cmd = terminalInput.trim().lowercase()
                                                        commandLogs.add("C:\\Windows\\System32>$terminalInput")
                                                        when {
                                                            cmd == "help" -> {
                                                                commandLogs.add("Available commands:")
                                                                commandLogs.add("  help      - Show this help menu")
                                                                commandLogs.add("  dir       - List directory contents")
                                                                commandLogs.add("  wine --version - Query active Wine engine")
                                                                commandLogs.add("  winetricks     - Configure Windows DLLs")
                                                                commandLogs.add("  systeminfo     - Display specs for Mali-G72")
                                                                commandLogs.add("  clear     - Clear logs")
                                                            }
                                                            cmd == "dir" -> {
                                                                commandLogs.add(" Directory of C:\\Windows\\System32:")
                                                                commandLogs.add("07/16/2026  10:24 AM    <DIR>          .")
                                                                commandLogs.add("07/16/2026  10:24 AM    <DIR>          ..")
                                                                commandLogs.add("07/16/2026  10:24 AM           820,112 kernel32.dll")
                                                                commandLogs.add("07/16/2026  10:24 AM           640,992 user32.dll")
                                                                commandLogs.add("07/16/2026  10:24 AM           310,240 gdi32.dll")
                                                            }
                                                            cmd == "wine --version" -> {
                                                                commandLogs.add("wine-8.0.2 (Rofwin Dynamic Build x86_64)")
                                                            }
                                                            cmd == "winetricks" -> {
                                                                commandLogs.add("Winetricks loader:")
                                                                commandLogs.add("Installing corefonts, d3dx9, d3dcompiler_47... SUCCESS.")
                                                            }
                                                            cmd == "systeminfo" -> {
                                                                commandLogs.add("Host Device : Oppo CPH1823 (Oppo F9)")
                                                                commandLogs.add("Processor   : MediaTek Helio P60 (MT6771)")
                                                                commandLogs.add("GPU         : ARM Mali-G72 MP3")
                                                                commandLogs.add("Sys Memory  : 4 GB LPDDR4X")
                                                                commandLogs.add("Active Pres : ${container.box64Preset} mode")
                                                            }
                                                            cmd == "clear" -> {
                                                                commandLogs.clear()
                                                            }
                                                            else -> {
                                                                commandLogs.add("'$terminalInput' is not recognized as an internal command.")
                                                            }
                                                        }
                                                        commandLogs.add("")
                                                        terminalInput = ""
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Execute Command", tint = Color.Green)
                                            }
                                        }
                                    }
                                }
                                DesktopWindow.DX_DIAG -> {
                                    // dxdiag details
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp).background(DarkSurface)) {
                                        Text("DirectX Diagnostic Tool", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
                                        Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp))

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            item { Text("System Information", fontWeight = FontWeight.Bold, color = PrimarySky, fontSize = 12.sp) }
                                            item { DxDiagRow("Current Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) }
                                            item { DxDiagRow("Computer Name", "ROFWIN-VIRT-PC") }
                                            item { DxDiagRow("Operating System", "Wine 8.0.2 Emulation Layer (Windows 10 x64)") }
                                            item { DxDiagRow("Processor", "MediaTek Helio P60 (MT6771) @ 2.00GHz (8 CPUs)") }
                                            item { DxDiagRow("Memory", "4096MB RAM (Dedicated to VirGL)") }
                                            item { DxDiagRow("Page file", "512MB used, 3584MB available") }
                                            item { DxDiagRow("DirectX Version", "DirectX 11 (WineD3D Emulated)") }

                                            item { Spacer(modifier = Modifier.height(8.dp)) }
                                            item { Text("Graphics Display Optimization Advice", fontWeight = FontWeight.Bold, color = SecondaryTeal, fontSize = 12.sp) }
                                            item { DxDiagRow("Direct3D Renderer", "OpenGL VirGL Emulator (via Mali-G72)") }
                                            item { DxDiagRow("Vulkan status", "None (Deactivated to prevent Mali shader crash)") }
                                            item { DxDiagRow("Recommended Res", "800x600 for performance (Helio P60)") }
                                        }
                                    }
                                }
                                DesktopWindow.GAME_SIMULATION -> {
                                    // Game emulator overlay experience
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(0.4f)
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // game rendering canvas
                                            GameSimulationScreen(selectedGameName, simulatedFps, gameTimeMs)
                                        }

                                        // Virtual Gamepad Control Simulator
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(0.6f)
                                                .background(DarkBackground)
                                                .padding(6.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Active Layout: ${profile.name}",
                                                        fontSize = 11.sp,
                                                        color = Color.LightGray,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = buttonPressedLog,
                                                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryTeal)
                                                    )
                                                }

                                                // Display the virtual controls configured in the selected profile!
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                                        .background(Color(0x1AFFFFFF))
                                                ) {
                                                    profile.controls.forEach { control ->
                                                        Button(
                                                            onClick = {
                                                                buttonPressedLog = "Pressed ${control.name} [Map: ${control.keyMapping}]"
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = PrimarySky.copy(alpha = 0.5f)),
                                                            modifier = Modifier
                                                                .size(control.sizeDp.dp)
                                                                .offset(
                                                                    x = (control.relativeX * 3.2f).dp,
                                                                    y = (control.relativeY * 1.5f).dp
                                                                ),
                                                            shape = CircleShape,
                                                            contentPadding = PaddingValues(1.dp)
                                                        ) {
                                                            Text(
                                                                control.name.take(4),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // Bottom Taskbar (Retro Styling but Dark Slate Theme)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF0F172A))
                    .align(Alignment.BottomCenter)
                    .border(1.dp, Color(0xFF334155)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start button
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { startMenuOpen = !startMenuOpen }
                        .background(if (startMenuOpen) PrimarySky else Color(0xFF1E293B))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = "Start",
                        tint = if (startMenuOpen) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (startMenuOpen) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Center active taskbar icon
                if (openWindow != DesktopWindow.NONE) {
                    Row(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(getWindowIcon(openWindow), contentDescription = null, tint = PrimarySky, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(openWindow.name.replace("_", " "), color = Color.White, fontSize = 11.sp)
                    }
                }

                // Right panel clock, specs and close emulator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(DarkSurface, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.BatteryStd, contentDescription = "Battery", tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("100%", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Close/Power Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Red.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Shutdown", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Start Menu Dropup
            if (startMenuOpen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = (-48).dp)
                        .width(200.dp)
                        .background(DarkSurface)
                        .border(1.dp, Color.Gray)
                        .padding(8.dp)
                ) {
                    Text(
                        "ROFWIN OS v1.0",
                        fontWeight = FontWeight.Bold,
                        color = PrimarySky,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                    )
                    Divider()
                    StartMenuItem("My Computer", Icons.Default.Computer) {
                        openWindow = DesktopWindow.MY_COMPUTER
                        startMenuOpen = false
                    }
                    StartMenuItem("Registry Editor", Icons.Default.Settings) {
                        openWindow = DesktopWindow.REGISTRY_EDITOR
                        startMenuOpen = false
                    }
                    StartMenuItem("Task Manager", Icons.Default.AlignVerticalBottom) {
                        openWindow = DesktopWindow.TASK_MANAGER
                        startMenuOpen = false
                    }
                    StartMenuItem("Command Prompt", Icons.Default.Terminal) {
                        openWindow = DesktopWindow.COMMAND_PROMPT
                        startMenuOpen = false
                    }
                    StartMenuItem("DirectX Diag", Icons.Default.Info) {
                        openWindow = DesktopWindow.DX_DIAG
                        startMenuOpen = false
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    StartMenuItem("Log Out / Close", Icons.Default.PowerSettingsNew, Color.Red) {
                        onClose()
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopIconButton(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(PrimarySky.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, PrimarySky.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = PrimarySky, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StartMenuItem(
    text: String,
    icon: ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
fun DxDiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.4f))
        Text(value, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}

@Composable
fun GameSimulationScreen(gameName: String, fps: Int, timeMs: Long) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Simple graphics demo simulating gameplay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render a simulated space grid scrolling
            val offset = (timeMs * 0.05f) % height
            for (i in 0..10) {
                drawLine(
                    color = Color(0x330EA5E9),
                    start = Offset(0f, offset + i * 40f),
                    end = Offset(width, offset + i * 40f),
                    strokeWidth = 2f
                )
            }

            // Render simulated player cube/ship
            drawCircle(
                color = Color(0xFFEF4444),
                center = Offset(width / 2f + (timeMs * 0.1f % 200 - 100), height - 60f),
                radius = 16f
            )

            // Render falling game objects (enemies / barriers)
            drawRect(
                color = Color(0xFF10B981),
                topLeft = Offset(width / 3f, (timeMs * 0.15f) % height),
                size = androidx.compose.ui.geometry.Size(30f, 30f)
            )

            drawRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(width * 2/3f, ((timeMs + 500) * 0.12f) % height),
                size = androidx.compose.ui.geometry.Size(24f, 24f)
            )
        }

        // Overlay game name, FPS counter and tuning telemetry
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wine Engine: $gameName.exe [x86_64]",
                    color = Color.Yellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "FPS: $fps",
                    color = if (fps >= 40) Color.Green else if (fps >= 24) Color.Yellow else Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mesa GL Driver: VirGL 3.1",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Oppo CPH1823 Temp: 42°C",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

fun getWindowIcon(window: DesktopWindow): ImageVector {
    return when (window) {
        DesktopWindow.MY_COMPUTER -> Icons.Default.Computer
        DesktopWindow.REGISTRY_EDITOR -> Icons.Default.Settings
        DesktopWindow.TASK_MANAGER -> Icons.Default.AlignVerticalBottom
        DesktopWindow.COMMAND_PROMPT -> Icons.Default.Terminal
        DesktopWindow.DX_DIAG -> Icons.Default.Info
        DesktopWindow.GAME_SIMULATION -> Icons.Default.Gamepad
        else -> Icons.Default.Laptop
    }
}

fun getWindowTitle(window: DesktopWindow, gameName: String): String {
    return when (window) {
        DesktopWindow.MY_COMPUTER -> "My Computer"
        DesktopWindow.REGISTRY_EDITOR -> "Registry Editor (regedit.exe)"
        DesktopWindow.TASK_MANAGER -> "Wine Task Manager"
        DesktopWindow.COMMAND_PROMPT -> "Command Prompt"
        DesktopWindow.DX_DIAG -> "DirectX Diagnostic Tool (dxdiag)"
        DesktopWindow.GAME_SIMULATION -> "Wine Render Engine - Running $gameName"
        else -> "Wine Window"
    }
}
