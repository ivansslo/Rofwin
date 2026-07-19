package com.winlator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Window Type
enum class DesktopWindow {
    NONE, MY_COMPUTER, REGISTRY_EDITOR, TASK_MANAGER, COMMAND_PROMPT, DX_DIAG, BROWSER, GIT_BASH, AI_ROUTE, WINRAR, PYTHON_SHELL, SSH_MANAGER, MT5, MQL5_EDITOR
}

data class SimFile(val name: String, val isDirectory: Boolean = false, val size: String = "1 KB")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineDesktopSim(
    container: WineContainer,
    profile: InputControlsProfile,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("RofwinDrives", android.content.Context.MODE_PRIVATE) }
    val dDriveEnabled = prefs.getBoolean("drive_d", true)
    val eDriveEnabled = prefs.getBoolean("drive_e", false)
    val zDriveEnabled = prefs.getBoolean("drive_z", false)

    var isBooting by remember { mutableStateOf(true) }
    var bootProgress by remember { mutableFloatStateOf(0f) }

    // Desktop Window States
    var openWindow by remember { mutableStateOf(DesktopWindow.NONE) }
    var startMenuOpen by remember { mutableStateOf(false) }

    // Window offsets (simple dragging support)
    var windowOffset by remember { mutableStateOf(Offset(50f, 100f)) }
    var minimizedWindows = remember { mutableStateListOf<DesktopWindow>() }

    // Windows-style window state (Rofwin 1.3.0): maximize / restore / true-fullscreen / low-RAM
    var isMaximized by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var lowRamMode by remember { mutableStateOf(prefs.getBoolean("low_ram", false)) }
    val openWin: (DesktopWindow) -> Unit = { w ->
        openWindow = w
        isMaximized = false
        isFullscreen = false
    }

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
                        SimFile("Work", true),
                        SimFile("Downloads", true)
                    ),
                    "D:\\Work" to listOf(
                        SimFile("script.py", false, "2 KB"),
                        SimFile("backup.rar", false, "15 MB"),
                        SimFile("notes.txt", false, "1 KB")
                    ),
                    "D:\\Downloads" to listOf(
                        SimFile("winetricks.exe", false, "3 MB"),
                        SimFile("python-3.12.exe", false, "25 MB"),
                        SimFile("lasokamodule.exe", false, "1.2 MB"),
                        SimFile("winrar_full.exe", false, "4.5 MB")
                    ),
                    "E:\\" to listOf(
                        SimFile("Media", true),
                        SimFile("chrome_installer.exe", false, "1.2 MB")
                    ),
                    "Z:\\" to listOf(
                        SimFile("system", true),
                        SimFile("data", true),
                        SimFile("vendor", true),
                        SimFile("build.prop", false, "4 KB")
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

    // Booting sequence
    LaunchedEffect(Unit) {
        while (bootProgress < 1f) {
            delay(100)
            bootProgress += 0.05f
        }
        isBooting = false
    }

    // simulated real-time stats (FPS, Temps)
    LaunchedEffect(openWindow) {
        // Stats logic removed (games only)
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
            // Wallpaper dinonaktifkan pada Low-RAM Mode (hemat decode bitmap besar di RAM 4GB)
            if (!lowRamMode) {
                Image(
                    painter = painterResource(id = R.drawable.rofwin_background_1784258475774),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Auto-save logic (prevent data loss during closures)
            LaunchedEffect(Unit) {
                while(true) {
                    delay(60000)
                    // Simulated serialization to storage every 60s
                    android.util.Log.d("Rofwin", "Auto-saving container ${container.name} state to storage...")
                }
            }
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
                    onClick = { openWin(DesktopWindow.MY_COMPUTER) }
                )
                DesktopIconButton(
                    name = "Web Browser",
                    icon = Icons.Default.Language,
                    onClick = { openWin(DesktopWindow.BROWSER) }
                )
                DesktopIconButton(
                    name = "Git Bash",
                    icon = Icons.Default.Terminal,
                    onClick = { openWin(DesktopWindow.GIT_BASH) }
                )
                DesktopIconButton(
                    name = "AI ROC Route",
                    icon = Icons.Default.Route,
                    onClick = { openWin(DesktopWindow.AI_ROUTE) }
                )
                DesktopIconButton(
                    name = "WinRAR",
                    icon = Icons.Default.FolderZip,
                    onClick = { openWin(DesktopWindow.WINRAR) }
                )
                DesktopIconButton(
                    name = "Python 3",
                    icon = Icons.Default.Code,
                    onClick = { openWin(DesktopWindow.PYTHON_SHELL) }
                )
                DesktopIconButton(
                    name = "SSH Connect",
                    icon = Icons.Default.CloudSync,
                    onClick = { openWin(DesktopWindow.SSH_MANAGER) }
                )
                DesktopIconButton(
                    name = "Task Manager",
                    icon = Icons.Default.AlignVerticalBottom,
                    onClick = { openWin(DesktopWindow.TASK_MANAGER) }
                )
            }

            // Quick Launcher Grid for Professional Tools (Optimized for Mali-G72)
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(180.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "QUICK LAUNCH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SecondaryTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        val quickTools = listOf(
                            Triple("MT5", Icons.Default.TrendingUp, DesktopWindow.MT5),
                            Triple("MQL5", Icons.Default.Code, DesktopWindow.MQL5_EDITOR),
                            Triple("PS1", Icons.Default.Terminal, DesktopWindow.COMMAND_PROMPT),
                            Triple("EXE", Icons.Default.PlayCircle, DesktopWindow.COMMAND_PROMPT),
                            Triple("Web", Icons.Default.Language, DesktopWindow.BROWSER),
                            Triple("Folder", Icons.Default.Folder, DesktopWindow.MY_COMPUTER)
                        )
                        items(quickTools) { (name, icon, win) ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { openWin(win) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Interactive Windows (Windows-style 1.3.0: drag / maximize / fullscreen / minimize)
            if (openWindow != DesktopWindow.NONE) {
                val windowModifier = when {
                    isFullscreen -> Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .background(DarkSurface)
                    isMaximized -> Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp)
                        .zIndex(2f)
                        .background(DarkSurface)
                    else -> Modifier
                        .offset { IntOffset(windowOffset.x.roundToInt(), windowOffset.y.roundToInt()) }
                        .width(420.dp)
                        .height(350.dp)
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, PrimarySky, RoundedCornerShape(8.dp))
                }
                Box(modifier = windowModifier) {
                    Column {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimarySky)
                                .pointerInput(isMaximized, isFullscreen) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (!isMaximized && !isFullscreen) {
                                            windowOffset += dragAmount
                                        }
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
                                    text = getWindowTitle(openWindow),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // [—] Minimize ke taskbar
                                IconButton(
                                    onClick = {
                                        minimizedWindows.add(openWindow)
                                        openWindow = DesktopWindow.NONE
                                        isMaximized = false
                                        isFullscreen = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Minimize, contentDescription = "Minimize", tint = Color.Black)
                                }
                                // [□] Maximize / Restore (ala Windows, taskbar tetap terlihat)
                                IconButton(
                                    onClick = {
                                        if (isFullscreen) {
                                            isFullscreen = false
                                            isMaximized = true
                                        } else {
                                            isMaximized = !isMaximized
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMaximized || isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isMaximized || isFullscreen) "Restore Down" else "Maximize",
                                        tint = Color.Black
                                    )
                                }
                                // [⛶] Full Screen sejati (menutupi taskbar — gaya F11 Chrome)
                                IconButton(
                                    onClick = {
                                        isFullscreen = !isFullscreen
                                        if (isFullscreen) isMaximized = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                        contentDescription = if (isFullscreen) "Exit Full Screen" else "Full Screen",
                                        tint = if (isFullscreen) Color(0xFF6A1B9A) else Color.Black
                                    )
                                }
                                // [✕] Close
                                IconButton(
                                    onClick = {
                                        openWindow = DesktopWindow.NONE
                                        isMaximized = false
                                        isFullscreen = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                                }
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
                                                enabled = currentPath != "C:\\" && currentPath != "D:\\" && currentPath != "E:\\" && currentPath != "Z:\\"
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
                                        if (currentPath == "C:\\" || currentPath == "D:\\" || currentPath == "E:\\" || currentPath == "Z:\\") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp)
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Card(
                                                    modifier = Modifier
                                                        .width(120.dp)
                                                        .clickable { currentPath = "C:\\" },
                                                    colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("C:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(32.dp))
                                                        Text("Local Disk (C:)", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                if (dDriveEnabled) {
                                                    Card(
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .clickable { currentPath = "D:\\" },
                                                        colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("D:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(32.dp))
                                                            Text("OBB Space (D:)", fontSize = 11.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                                if (eDriveEnabled) {
                                                    Card(
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .clickable { currentPath = "E:\\" },
                                                        colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("E:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(32.dp))
                                                            Text("Downloads (E:)", fontSize = 11.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                                if (zDriveEnabled) {
                                                    Card(
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .clickable { currentPath = "Z:\\" },
                                                        colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("Z:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Memory, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                                                            Text("Root FS (Z:)", fontSize = 11.sp, color = Color.White)
                                                        }
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
                                            Text("Mali-G72 GPU: ${"12%"}", style = MaterialTheme.typography.labelSmall.copy(color = SecondaryTeal))
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
                                    // Interactive Terminal (cmd.exe + powershell.exe mode)
                                    TerminalWindow(
                                        container = container,
                                        commandLogs = commandLogs,
                                        terminalInput = terminalInput,
                                        onInputChange = { terminalInput = it },
                                        onLaunch = { w -> openWin(w) }
                                    )
                                }
                                DesktopWindow.WINRAR -> {
                                    WinRarWindow()
                                }
                                DesktopWindow.PYTHON_SHELL -> {
                                    PythonShellWindow()
                                }
                                DesktopWindow.SSH_MANAGER -> {
                                    SshManagerWindow()
                                }
                                DesktopWindow.MT5 -> {
                                    Mt5Window(lowRam = lowRamMode)
                                }
                                DesktopWindow.MQL5_EDITOR -> {
                                    Mql5EditorWindow()
                                }
                                DesktopWindow.BROWSER -> {
                                    BrowserWindow()
                                }
                                DesktopWindow.GIT_BASH -> {
                                    GitBashWindow()
                                }
                                DesktopWindow.AI_ROUTE -> {
                                    AiRouteWindow()
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
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeWindows = listOfNotNull(openWindow.takeIf { it != DesktopWindow.NONE }) + minimizedWindows
                    activeWindows.distinct().forEach { win ->
                        Box(
                            modifier = Modifier
                                .background(if (win == openWindow) PrimarySky.copy(alpha = 0.2f) else Color(0x1AFFFFFF), RoundedCornerShape(4.dp))
                                .border(1.dp, if (win == openWindow) PrimarySky else Color.Gray, RoundedCornerShape(4.dp))
                                .clickable {
                                    if (win == openWindow) {
                                        minimizedWindows.add(win)
                                        openWindow = DesktopWindow.NONE
                                        isMaximized = false
                                        isFullscreen = false
                                    } else {
                                        minimizedWindows.remove(win)
                                        openWin(win)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(getWindowIcon(win), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(getWindowTitle(win).take(15), color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Right panel clock, specs and close emulator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    // Volume Control (New)
                    var showVolume by remember { mutableStateOf(false) }
                    var volumeLevel by remember { mutableFloatStateOf(0.7f) }
                    
                    Box {
                        Icon(
                            imageVector = if (volumeLevel > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Volume",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp).clickable { showVolume = !showVolume }
                        )
                        
                        if (showVolume) {
                            Card(
                                modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-50).dp).width(40.dp).height(150.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Slider(
                                        value = volumeLevel,
                                        onValueChange = { volumeLevel = it },
                                        modifier = Modifier.weight(1f).graphicsLayer {
                                            rotationZ = -90f
                                        }
                                    )
                                    Text("${(volumeLevel * 100).toInt()}%", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

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

            // Start Menu Dropup (disembunyikan saat Full Screen sejati)
            if (startMenuOpen && !isFullscreen) {
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
                        openWin(DesktopWindow.MY_COMPUTER)
                        startMenuOpen = false
                    }
                    StartMenuItem("Registry Editor", Icons.Default.Settings) {
                        openWin(DesktopWindow.REGISTRY_EDITOR)
                        startMenuOpen = false
                    }
                    StartMenuItem("Task Manager", Icons.Default.AlignVerticalBottom) {
                        openWin(DesktopWindow.TASK_MANAGER)
                        startMenuOpen = false
                    }
                    StartMenuItem("Command Prompt / PowerShell", Icons.Default.Terminal) {
                        openWin(DesktopWindow.COMMAND_PROMPT)
                        startMenuOpen = false
                    }
                    StartMenuItem("DirectX Diag", Icons.Default.Info) {
                        openWin(DesktopWindow.DX_DIAG)
                        startMenuOpen = false
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    StartMenuItem(
                        text = if (lowRamMode) "Low-RAM Mode: ON (hemat 4GB)" else "Low-RAM Mode: OFF",
                        icon = Icons.Default.Memory,
                        tint = if (lowRamMode) SecondaryTeal else Color.White
                    ) {
                        lowRamMode = !lowRamMode
                        prefs.edit().putBoolean("low_ram", lowRamMode).apply()
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

fun getWindowIcon(window: DesktopWindow): ImageVector {
    return when (window) {
        DesktopWindow.MY_COMPUTER -> Icons.Default.Computer
        DesktopWindow.REGISTRY_EDITOR -> Icons.Default.Settings
        DesktopWindow.TASK_MANAGER -> Icons.Default.AlignVerticalBottom
        DesktopWindow.COMMAND_PROMPT -> Icons.Default.Terminal
        DesktopWindow.DX_DIAG -> Icons.Default.Info
        DesktopWindow.BROWSER -> Icons.Default.Language
        DesktopWindow.GIT_BASH -> Icons.Default.Terminal
        DesktopWindow.AI_ROUTE -> Icons.Default.Route
        DesktopWindow.WINRAR -> Icons.Default.FolderZip
        DesktopWindow.PYTHON_SHELL -> Icons.Default.Code
        DesktopWindow.SSH_MANAGER -> Icons.Default.CloudSync
        DesktopWindow.MT5 -> Icons.Default.TrendingUp
        DesktopWindow.MQL5_EDITOR -> Icons.Default.Code
        else -> Icons.Default.Laptop
    }
}

fun getWindowTitle(window: DesktopWindow): String {
    return when (window) {
        DesktopWindow.MY_COMPUTER -> "My Computer"
        DesktopWindow.REGISTRY_EDITOR -> "Registry Editor (regedit.exe)"
        DesktopWindow.TASK_MANAGER -> "Wine Task Manager"
        DesktopWindow.COMMAND_PROMPT -> "Command Prompt / PowerShell"
        DesktopWindow.DX_DIAG -> "DirectX Diagnostic Tool (dxdiag)"
        DesktopWindow.BROWSER -> "Google Chrome"
        DesktopWindow.GIT_BASH -> "Git Bash Terminal"
        DesktopWindow.AI_ROUTE -> "AI ROC-AgentsRoute v1.0"
        DesktopWindow.WINRAR -> "WinRAR (Unregistered Evaluation Copy)"
        DesktopWindow.PYTHON_SHELL -> "Python 3.12.1 Shell"
        DesktopWindow.SSH_MANAGER -> "SSH Connection Manager"
        DesktopWindow.MT5 -> "MetaTrader 5"
        DesktopWindow.MQL5_EDITOR -> "MetaQuotes Language 5 Editor"
        else -> "Wine Window"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalWindow(
    container: WineContainer,
    commandLogs: MutableList<String>,
    terminalInput: String,
    onInputChange: (String) -> Unit,
    onLaunch: (DesktopWindow) -> Unit
) {
    // Mode terminal: cmd.exe (hitam-hijau) atau powershell.exe (biru-putih)
    var psMode by remember { mutableStateOf(false) }
    var cwd by remember { mutableStateOf("C:\\Windows\\System32") }
    val bgColor = if (psMode) Color(0xFF012456) else Color.Black
    val fgColor = if (psMode) Color.White else Color.Green
    val prompt = if (psMode) "PS $cwd> " else "$cwd>"

    fun exec(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        commandLogs.add("$prompt$input")
        val cmd = input.lowercase()
        val arg = input.substringAfter(' ', "").trim()

        fun unknown() {
            if (psMode) commandLogs.add("$input : The term '$input' is not recognized as the name of a cmdlet, function, script file, or operable program.")
            else commandLogs.add("'$input' is not recognized as an internal or external command, operable program or batch file.")
        }

        when {
            // ---- Mode switching ----
            psMode && (cmd == "exit" || cmd == "cmd" || cmd == "cmd.exe") -> {
                psMode = false
                commandLogs.add("Leaving Windows PowerShell session...")
            }
            !psMode && (cmd == "powershell" || cmd == "powershell.exe" || cmd == "pwsh" || cmd.startsWith("ps1 ")) -> {
                psMode = true
                commandLogs.add("Windows PowerShell")
                commandLogs.add("Copyright (C) Rofwin Corporation. All rights reserved.")
                commandLogs.add("")
                commandLogs.add("Install the latest PowerShell for new features: not required, this is Rofwin :)")
            }
            cmd == "exit" -> {
                commandLogs.clear()
                commandLogs.add("(session cleared — type 'help' for commands)")
            }

            // ---- Help ----
            cmd == "help" || cmd == "?" || cmd == "/?" || cmd == "get-help" || cmd.startsWith("get-help ") -> {
                commandLogs.add("ROFWIN COMMAND REFERENCE ${if (psMode) "(PowerShell)" else "(cmd.exe)"}:")
                commandLogs.add("  help / ver / cls / exit        Session basics")
                commandLogs.add("  dir / cd <path> / tree         File system")
                commandLogs.add("  echo <text> / set              Environment")
                commandLogs.add("  ipconfig / netstat / ping <h>  Networking")
                commandLogs.add("  tasklist / taskkill / systeminfo   Processes & specs")
                commandLogs.add("  whoami / hostname / date / time    Identity")
                commandLogs.add("  start <app>   (explorer, chrome, mt5, metaeditor)")
                commandLogs.add("  mt5 / terminal64.exe           Launch MetaTrader 5")
                commandLogs.add("  metaeditor / mql5              Launch MQL5 Editor")
                commandLogs.add("  wine --version / winetricks    Wine engine")
                if (psMode) commandLogs.add("  Get-Process / Get-ChildItem / Get-Date / Get-Host (alias OK)")
                if (!psMode) commandLogs.add("  powershell                     Switch to PowerShell mode")
            }

            // ---- System ----
            cmd == "ver" -> commandLogs.add("Rofwin Windows [Version 10.0.19045.4046] (Wine 8.0.2 x86_64)")
            cmd == "cls" || cmd == "clear" -> commandLogs.clear()
            cmd == "echo" -> commandLogs.add("ECHO is on.")
            cmd.startsWith("echo ") -> commandLogs.add(input.substringAfter(' '))
            cmd == "set" -> {
                commandLogs.add("ProgramFiles=C:\\Program Files")
                commandLogs.add("SystemRoot=C:\\Windows")
                commandLogs.add("TEMP=C:\\users\\Administrator\\Temp")
                commandLogs.add("WINEDEBUG=-all")
                commandLogs.add("BOX64_DYNAREC=1")
            }
            cmd == "whoami" -> commandLogs.add("rofwin-virt-pc\\administrator")
            cmd == "hostname" -> commandLogs.add("ROFWIN-VIRT-PC")
            cmd == "date" || cmd == "date /t" || cmd == "get-date" ->
                commandLogs.add(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
            cmd == "time" || cmd == "time /t" ->
                commandLogs.add(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
            cmd == "get-host" -> {
                commandLogs.add("Name             : ConsoleHost   5.1.19045.4046")
                commandLogs.add("InstanceId       : 8f2c1d44-rofwin-sim-0001")
            }

            // ---- Filesystem ----
            cmd == "dir" || cmd == "ls" || cmd == "gci" || cmd == "get-childitem" -> {
                commandLogs.add(" Directory of $cwd")
                commandLogs.add("")
                commandLogs.add("07/16/2026  10:24 AM    <DIR>          .")
                commandLogs.add("07/16/2026  10:24 AM    <DIR>          ..")
                if (cwd.startsWith("C:\\Windows")) {
                    commandLogs.add("07/16/2026  10:24 AM           820,112 kernel32.dll")
                    commandLogs.add("07/16/2026  10:24 AM           640,992 user32.dll")
                    commandLogs.add("07/16/2026  10:24 AM           310,240 gdi32.dll")
                    commandLogs.add("07/16/2026  10:24 AM            98,304 cmd.exe")
                    commandLogs.add("07/16/2026  10:24 AM           251,904 powershell.exe")
                } else {
                    commandLogs.add("07/16/2026  10:24 AM    <DIR>          users")
                    commandLogs.add("07/16/2026  10:24 AM    <DIR>          Program Files")
                    commandLogs.add("07/16/2026  10:24 AM    <DIR>          Windows")
                    commandLogs.add("07/16/2026  10:24 AM               256 boot.ini")
                }
                commandLogs.add("               4 Dir(s)  18,446,744,073,709,551,616 bytes free")
            }
            cmd == "cd" || cmd == "chdir" || cmd == "pwd" || cmd == "get-location" -> commandLogs.add(cwd)
            cmd.startsWith("cd ") || cmd.startsWith("chdir ") || cmd.startsWith("set-location ") -> {
                val t = arg.replace("/", "\\")
                cwd = when {
                    t == ".." -> cwd.substringBeforeLast('\\', "C:\\")
                    t == "\\" -> cwd.substringBefore('\\') + "\\"
                    t.contains(":") -> t.trimEnd('\\')
                    else -> (cwd.trimEnd('\\') + "\\" + t).trimEnd('\\')
                }
                commandLogs.add("(now in $cwd)")
            }
            cmd == "tree" -> {
                commandLogs.add("C:\\")
                commandLogs.add("+---Windows")
                commandLogs.add("|   \\---System32")
                commandLogs.add("+---Program Files")
                commandLogs.add("|   +---DirectX")
                commandLogs.add("|   \\---WineD3D")
                commandLogs.add("\\---users")
                commandLogs.add("    \\---Administrator")
            }

            // ---- Networking ----
            cmd == "ipconfig" || cmd == "ipconfig /all" -> {
                commandLogs.add("Windows IP Configuration")
                commandLogs.add("")
                commandLogs.add("Ethernet adapter Wine0:")
                commandLogs.add("   IPv4 Address. . . . . . . . . . . : 10.0.2.15")
                commandLogs.add("   Subnet Mask . . . . . . . . . . . : 255.255.255.0")
                commandLogs.add("   Default Gateway . . . . . . . . . : 10.0.2.2")
                commandLogs.add("   DNS Servers . . . . . . . . . . . : 8.8.8.8")
            }
            cmd == "netstat" || cmd.startsWith("netstat ") -> {
                commandLogs.add("Active Connections")
                commandLogs.add("  Proto  Local Address      Foreign Address    State")
                commandLogs.add("  TCP    10.0.2.15:49152    10.0.2.2:53          ESTABLISHED")
                commandLogs.add("  TCP    10.0.2.15:50001    wine.roadfx:443      TIME_WAIT")
            }
            cmd.startsWith("ping") -> {
                val host = if (arg.isEmpty()) "8.8.8.8" else arg
                commandLogs.add("Pinging $host with 32 bytes of data:")
                commandLogs.add("Reply from $host: bytes=32 time=41ms TTL=117")
                commandLogs.add("Reply from $host: bytes=32 time=38ms TTL=117")
                commandLogs.add("Reply from $host: bytes=32 time=45ms TTL=117")
                commandLogs.add("Reply from $host: bytes=32 time=39ms TTL=117")
                commandLogs.add("Ping statistics: Packets: Sent = 4, Received = 4, Lost = 0 (0% loss)")
            }

            // ---- Processes ----
            cmd == "tasklist" || cmd == "ps" || cmd == "get-process" -> {
                commandLogs.add("Image Name                     PID   Mem Usage")
                commandLogs.add("=========================   =======   =========")
                commandLogs.add("explorer.exe                  1024     12,412 K")
                commandLogs.add("services.exe                   512      4,096 K")
                commandLogs.add("wineserver                     640     32,768 K")
                commandLogs.add("box64                          768     48,128 K")
                commandLogs.add("chrome.exe                    2048    156,672 K")
            }
            cmd.startsWith("taskkill") || cmd.startsWith("stop-process") ->
                commandLogs.add("SUCCESS: The process has been terminated.")
            cmd == "systeminfo" || cmd == "msinfo32" -> {
                commandLogs.add("Host Name        : ROFWIN-VIRT-PC")
                commandLogs.add("OS Name          : Rofwin Windows 10 Pro (Wine 8.0.2)")
                commandLogs.add("Host Device      : Oppo CPH1823 (Oppo F9)")
                commandLogs.add("Processor        : MediaTek Helio P60 MT6771, 8 Core(s) @2.00GHz")
                commandLogs.add("GPU              : ARM Mali-G72 MP3 (VirGL)")
                commandLogs.add("Total Memory     : 4,096 MB LPDDR4X")
                commandLogs.add("Active Preset    : ${container.box64Preset} mode")
            }

            // ---- Launch other windows (seperti 'start' asli) ----
            cmd == "start" || cmd == "start explorer" || cmd == "explorer" || cmd == "explorer.exe" || cmd == "invoke-item ." -> {
                commandLogs.add("Opening File Explorer...")
                onLaunch(DesktopWindow.MY_COMPUTER)
            }
            cmd.startsWith("start chrome") || cmd == "chrome" || cmd == "msedge" || cmd == "iexplore" || cmd.startsWith("start msedge") -> {
                commandLogs.add("Starting Chrome...")
                onLaunch(DesktopWindow.BROWSER)
            }
            cmd == "mt5" || cmd == "terminal64.exe" || cmd == "start terminal64.exe" -> {
                commandLogs.add("Loading MetaTrader 5 terminal...")
                onLaunch(DesktopWindow.MT5)
            }
            cmd == "metaeditor" || cmd == "metaeditor64.exe" || cmd == "mql5" || cmd == "start metaeditor64.exe" -> {
                commandLogs.add("Loading MetaEditor (MQL5)...")
                onLaunch(DesktopWindow.MQL5_EDITOR)
            }
            cmd.startsWith("start ") -> commandLogs.add("Starting '${input.substringAfter("start ")}' (simulated).")

            // ---- Wine extras ----
            cmd == "wine --version" -> commandLogs.add("wine-8.0.2 (Rofwin Dynamic Build x86_64)")
            cmd == "winetricks" || cmd.startsWith("winetricks ") -> {
                commandLogs.add("Winetricks loader:")
                commandLogs.add("Installing corefonts, d3dx9, d3dcompiler_47... SUCCESS.")
            }

            else -> unknown()
        }
        commandLogs.add("")
        onInputChange("")
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Session chips: ketuk untuk pindah cmd <-> PowerShell (ramah layar sentuh)
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !psMode,
                onClick = { psMode = false },
                label = { Text("cmd.exe", fontSize = 10.sp) }
            )
            FilterChip(
                selected = psMode,
                onClick = { psMode = true },
                label = { Text("powershell.exe", fontSize = 10.sp) }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "ketik 'help'",
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            items(commandLogs) { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = fgColor,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(4.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(prompt, color = fgColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            Spacer(modifier = Modifier.width(4.dp))
            TextField(
                value = terminalInput,
                onValueChange = onInputChange,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = bgColor,
                    unfocusedContainerColor = bgColor,
                    focusedTextColor = fgColor,
                    unfocusedTextColor = fgColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { exec(terminalInput) },
                    onGo = { exec(terminalInput) },
                    onDone = { exec(terminalInput) }
                )
            )
            IconButton(onClick = { exec(terminalInput) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Execute Command", tint = fgColor)
            }
        }
    }
}

@Composable
fun WinRarWindow() {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.Gray.copy(alpha = 0.2f)).padding(4.dp)) {
            listOf("File", "Commands", "Tools", "Favorites", "Options", "Help").forEach {
                Text(it, fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, tint = PrimarySky)
            Icon(Icons.Default.Upload, contentDescription = null, tint = SecondaryTeal)
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            Icon(Icons.Default.FindInPage, contentDescription = null, tint = Color.Yellow)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.Gray).background(DarkSurface).padding(8.dp)) {
            Column {
                Text("Archive: backup.rar", color = Color.White, fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                listOf("database_dump.sql", "config.json", "logs/", "src_backup/").forEach {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (it.endsWith("/")) Icons.Default.Folder else Icons.Default.Description, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PythonShellWindow() {
    val logs = remember { mutableStateListOf("Python 3.12.1 (tags/v3.12.1:2305ca5, Dec  7 2023, 22:03:25) [MSC v.1937 64 bit (AMD64)] on win32", "Type \"help\", \"copyright\", \"credits\" or \"license\" for more information.", ">>> ") }
    var input by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { Text(it, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(">>> ", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank()) {
                        logs.add(">>> $input")
                        when (input.trim()) {
                            "print('hello')" -> logs.add("hello")
                            "2 + 2" -> logs.add("4")
                            "import os; os.listdir()" -> logs.add("['Windows', 'Program Files', 'users']")
                            else -> logs.add("NameError: name '$input' is not defined")
                        }
                        input = ""
                    }
                })
            )
        }
    }
}

@Composable
fun SshManagerWindow() {
    var host by remember { mutableStateOf("192.168.1.105") }
    var user by remember { mutableStateOf("admin") }
    var connected by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!connected) {
            Text("SSH Connection Setup", color = PrimarySky, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host IP") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { connected = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Connect Automatically")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.Green)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connected to $host as $user", color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
                Text("admin@$host:~$ ls -la\ntotal 4k\ndrwxr-xr-x 2 admin admin 4096 Jul 16 20:15 .\ndrwxr-xr-x 3 root  root  4096 Jul 16 20:15 ..\n-rw-r--r-- 1 admin admin    0 Jul 16 20:15 .bash_history", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
@Composable
fun Mt5Window(lowRam: Boolean) {
    val rnd = remember { java.util.Random() }
    // Symbol mid-prices (feed simulasi lokal — BUKAN koneksi broker sungguhan)
    val mids = remember {
        mutableStateMapOf(
            "EURUSD" to 1.09250, "GBPUSD" to 1.27510, "USDJPY" to 145.305,
            "USDCHF" to 0.88120, "AUDUSD" to 0.66240, "XAUUSD" to 2385.40, "BTCUSD" to 97450.0
        )
    }
    val prevMids = remember { mutableStateMapOf<String, Double>().apply { mids.forEach { (k, v) -> put(k, v) } } }
    var chartSymbol by remember { mutableStateOf("EURUSD") }
    var timeframe by remember { mutableStateOf("H1") }
    val series = remember {
        mutableStateListOf<Float>().apply {
            var v = 50f
            repeat(80) { add(v); v = (v + (rnd.nextFloat() - 0.48f) * 6f).coerceIn(5f, 95f) }
        }
    }
    val trades = remember { mutableStateListOf<String>() }
    var ticket by remember { mutableStateOf(53000100) }
    var balance by remember { mutableFloatStateOf(10000f) }
    var floatingPnl by remember { mutableFloatStateOf(0f) }
    var pingMs by remember { mutableStateOf(41) }

    fun digits(sym: String) = when (sym) {
        "USDJPY" -> 3
        "XAUUSD" -> 2
        "BTCUSD" -> 1
        else -> 5
    }
    fun fmt(sym: String, v: Double) = "%.${digits(sym)}f".format(v)
    fun spread(sym: String) = when (sym) {
        "USDJPY" -> 0.015; "XAUUSD" -> 0.35; "BTCUSD" -> 25.0; else -> 0.00015
    }

    // Live tick engine (lebih lambat & hemat saat Low-RAM Mode)
    LaunchedEffect(lowRam) {
        while (true) {
            delay(if (lowRam) 2500L else 900L)
            mids.keys.toList().forEach { s ->
                val old = mids[s] ?: 1.0
                prevMids[s] = old
                val vol = if (s == "BTCUSD") 0.0012 else if (s == "XAUUSD") 0.0007 else 0.0004
                mids[s] = (old * (1.0 + (rnd.nextFloat() - 0.5f) * 2f * vol.toFloat()))
            }
            val last = series.last()
            series.removeAt(0)
            series.add((last + (rnd.nextFloat() - 0.48f) * 5f).coerceIn(5f, 95f))
            floatingPnl = ((rnd.nextFloat() - 0.45f) * 120f * trades.size.coerceAtLeast(1)) * if (trades.isEmpty()) 0f else 1f
            pingMs = 30 + rnd.nextInt(60)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C))) {
        // Menu bar ala MT5 desktop
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            listOf("File", "View", "Insert", "Charts", "Tools", "Window", "Help").forEach {
                Text(it, color = Color(0xFFBBBBBB), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        // Toolbar: timeframe + simbol chart
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF242424)).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("M1", "M5", "M15", "H1", "H4", "D1").forEach { tf ->
                Text(
                    tf,
                    color = if (tf == timeframe) Color(0xFF4CAF50) else Color(0xFF999999),
                    fontSize = 10.sp,
                    fontWeight = if (tf == timeframe) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { timeframe = tf }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Demo 12345678 — Rofwin-Demo Server", color = Color(0xFF88CC88), fontSize = 10.sp)
        }

        Row(modifier = Modifier.weight(1f)) {
            // Market Watch (harga bergerak live)
            Column(modifier = Modifier.width(140.dp).fillMaxHeight().border(1.dp, Color(0xFF333333)).padding(6.dp)) {
                Text("Market Watch", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Symbol", color = Color.Gray, fontSize = 9.sp)
                    Text("Bid / Ask", color = Color.Gray, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn {
                    items(mids.keys.toList()) { s ->
                        val mid = mids[s] ?: 1.0
                        val up = mid >= (prevMids[s] ?: mid)
                        val c = if (up) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chartSymbol = s }
                                .background(if (s == chartSymbol) Color(0xFF173A17) else Color.Transparent)
                                .padding(vertical = 3.dp)
                        ) {
                            Text(s, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${fmt(s, mid)}   ${fmt(s, mid + spread(s))}", color = c, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            // Chart Area (line chart live)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black)) {
                Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    val w = size.width; val h = size.height
                    // grid horizontal
                    for (i in 1..4) {
                        drawLine(Color(0xFF1B1B1B), Offset(0f, h * i / 5f), Offset(w, h * i / 5f), strokeWidth = 1f)
                    }
                    val step = w / (series.size - 1)
                    for (i in 0 until series.size - 1) {
                        val y1 = h - (series[i] / 100f) * h
                        val y2 = h - (series[i + 1] / 100f) * h
                        drawLine(
                            color = if (series[i + 1] >= series[i]) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            start = Offset(i * step, y1),
                            end = Offset((i + 1) * step, y2),
                            strokeWidth = 3f
                        )
                    }
                }
                Text(
                    "$chartSymbol, $timeframe",
                    color = Color.DarkGray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    "Bid ${fmt(chartSymbol, mids[chartSymbol] ?: 1.0)}",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                )
                // Sell / Buy seperti panel one-click MT5
                Row(modifier = Modifier.align(Alignment.TopStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            trades.add("#${ticket++} sell 0.01 $chartSymbol @ ${fmt(chartSymbol, mids[chartSymbol] ?: 1.0)}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2424)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("Sell ${fmt(chartSymbol, mids[chartSymbol] ?: 1.0)}", fontSize = 9.sp) }
                    Button(
                        onClick = {
                            trades.add("#${ticket++} buy 0.01 $chartSymbol @ ${fmt(chartSymbol, (mids[chartSymbol] ?: 1.0) + spread(chartSymbol))}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5AA8)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("Buy ${fmt(chartSymbol, (mids[chartSymbol] ?: 1.0) + spread(chartSymbol))}", fontSize = 9.sp) }
                }
            }
        }

        // Toolbox
        Column(modifier = Modifier.fillMaxWidth().height(110.dp).border(1.dp, Color(0xFF333333)).padding(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Trade", "Exposure", "History", "News", "Mailbox").forEach {
                    Text(it, color = if (it == "Trade") Color.White else Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (trades.isEmpty()) {
                    item { Text("no open positions — ketuk Sell/Buy untuk order simulasi", color = Color.Gray, fontSize = 10.sp) }
                } else {
                    items(trades) { t -> Text(t, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                }
            }
            Text(
                "Balance: ${"%.2f".format(balance)} USD   Equity: ${"%.2f".format(balance + floatingPnl)} USD   Free Margin: ${"%.2f".format(balance + floatingPnl - trades.size * 50f)}",
                color = if (floatingPnl >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Status bar ala MT5
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Circle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(8.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Connected  |  $pingMs ms  |  Rofwin Wine DDE (feed simulasi lokal)", color = Color(0xFF999999), fontSize = 9.sp)
        }
    }
}

@Composable
fun Mql5EditorWindow() {
    var code by remember {
        mutableStateOf("//+------------------------------------------------------------------+\n//|                                                      Expert.mq5 |\n//|                                      Copyright 2026, MetaQuotes |\n//|                                             https://www.mql5.com |\n//+------------------------------------------------------------------+\n#property copyright \"Copyright 2026\"\n#property link      \"https://www.mql5.com\"\n#property version   \"1.00\"\n\n//+------------------------------------------------------------------+\n//| Expert initialization function                                   |\n//+------------------------------------------------------------------+\nint OnInit()\n  {\n   Print(\"Algo Editor Sync Activated!\");\n   return(INIT_SUCCEEDED);\n  }\n\n//+------------------------------------------------------------------+\n//| Expert tick function                                             |\n//+------------------------------------------------------------------+\nvoid OnTick()\n  {\n   // strategy here\n  }\n")
    }
    val outputLogs = remember { mutableStateListOf<String>() }
    var compiling by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("Algo Editor MQL5 Sync: Active & Connected") }
    val scope = rememberCoroutineScope()

    fun compile() {
        if (compiling) return
        compiling = true
        outputLogs.add("Compiling 'Expert.mq5'...")
        scope.launch {
            delay(900)
            // Pemeriksaan sederhana: kurung kurawal tak seimbang => error ala compiler
            val openB = code.count { it == '{' }
            val closeB = code.count { it == '}' }
            if (openB != closeB) {
                outputLogs.add("'{' - unbalanced parentheses   Expert.mq5   line ${code.lines().size}   (1 error, 0 warnings)")
            } else {
                val kb = (code.toByteArray().size / 3 + 1024)
                outputLogs.add("0 errors, 0 warnings, $kb bytes code generated")
                outputLogs.add("Expert.ex5 written to C:\\MQL5\\Experts\\")
            }
            compiling = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // Toolbar ala MetaEditor
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("MetaEditor — Expert.mq5", color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { compile() }, modifier = Modifier.height(26.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (compiling) "Compiling..." else "Compile (F7)", fontSize = 10.sp)
            }
        }
        // Editor
        TextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedTextColor = Color(0xFFD4D4D4),
                unfocusedTextColor = Color(0xFFD4D4D4),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        )
        // Output panel (Errors / Warnings) ala MetaEditor
        Column(modifier = Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF252526)).padding(6.dp)) {
            Text("Errors  |  Warnings  |  Find", color = Color.Gray, fontSize = 9.sp)
            LazyColumn {
                if (outputLogs.isEmpty()) {
                    item { Text("Tekan Compile (F7) untuk membangun EA...", color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                } else {
                    items(outputLogs) { line ->
                        Text(
                            line,
                            color = if (line.contains("error", ignoreCase = true) && !line.startsWith("0 errors")) Color(0xFFEF5350) else Color(0xFF4CAF50),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        // Status bar
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF007ACC)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(syncStatus, color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("Ln ${code.lines().size}", color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("UTF-8", color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MQL5", color = Color.White, fontSize = 10.sp)
        }
    }
}
@Composable
fun BrowserWindow() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var urlInput by remember { mutableStateOf(url) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(DarkSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigasi ala Chrome: kembali / maju / beranda
            IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Maju", tint = Color.White)
            }
            IconButton(
                onClick = {
                    url = "https://www.google.com"
                    urlInput = url
                    webView?.loadUrl(url)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Beranda", tint = Color.White)
            }
            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant),
                textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    var loadUrl = urlInput.trim()
                    if (!loadUrl.startsWith("http://") && !loadUrl.startsWith("https://")) {
                        loadUrl = if (loadUrl.contains(".")) "https://$loadUrl" else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(loadUrl, "UTF-8")
                    }
                    url = loadUrl
                    urlInput = loadUrl
                    webView?.loadUrl(url)
                }),
                singleLine = true
            )
            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang", tint = Color.White)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        loadUrl(url)
                        webView = this
                    }
                },
                update = {
                    // Navigasi via ongmn action/keyboard/Go button
                }
            )
        }
    }
}

@Composable
fun GitBashWindow() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(12.dp)) {
        Text("ivansslo@CPH1823 MINGW64 /", color = Color(0xFFADFF2F), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("$ git fetch origin", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("From github.com:ivansslo/rofwin-agents", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(" * [new branch]      main       -> origin/main", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("$ git branch", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("* main", color = Color(0xFFADFF2F), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("$ _", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun AiRouteWindow() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI ROC-AgentsRoute v1.0", style = MaterialTheme.typography.titleMedium.copy(color = SecondaryTeal))
        Text("Powered by Gemini Agentic Engine", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Current Route Status:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                Text("Route A-102 (Optimized for MediaTek P60)", color = PrimarySky, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Latency Reduction: 24ms", color = Color.Green, fontSize = 11.sp)
                Text("Packet Steering: Active", color = Color.Green, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Recalculate */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recalculate AI Path")
        }
    }
}
