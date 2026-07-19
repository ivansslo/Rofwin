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
import kotlinx.coroutines.delay
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
            Image(
                painter = painterResource(id = R.drawable.rofwin_background_1784258475774),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

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
                    onClick = { openWindow = DesktopWindow.MY_COMPUTER }
                )
                DesktopIconButton(
                    name = "Web Browser",
                    icon = Icons.Default.Language,
                    onClick = { openWindow = DesktopWindow.BROWSER }
                )
                DesktopIconButton(
                    name = "Git Bash",
                    icon = Icons.Default.Terminal,
                    onClick = { openWindow = DesktopWindow.GIT_BASH }
                )
                DesktopIconButton(
                    name = "AI ROC Route",
                    icon = Icons.Default.Route,
                    onClick = { openWindow = DesktopWindow.AI_ROUTE }
                )
                DesktopIconButton(
                    name = "WinRAR",
                    icon = Icons.Default.FolderZip,
                    onClick = { openWindow = DesktopWindow.WINRAR }
                )
                DesktopIconButton(
                    name = "Python 3",
                    icon = Icons.Default.Code,
                    onClick = { openWindow = DesktopWindow.PYTHON_SHELL }
                )
                DesktopIconButton(
                    name = "SSH Connect",
                    icon = Icons.Default.CloudSync,
                    onClick = { openWindow = DesktopWindow.SSH_MANAGER }
                )
                DesktopIconButton(
                    name = "Task Manager",
                    icon = Icons.Default.AlignVerticalBottom,
                    onClick = { openWindow = DesktopWindow.TASK_MANAGER }
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
                                    .clickable { openWindow = win }
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
                                    text = getWindowTitle(openWindow),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        minimizedWindows.add(openWindow)
                                        openWindow = DesktopWindow.NONE
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Minimize, contentDescription = "Minimize", tint = Color.Black)
                                }
                                IconButton(
                                    onClick = { openWindow = DesktopWindow.NONE },
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
                                    // Interactive Terminal Prompt
                                    TerminalWindow(container, commandLogs, terminalInput, onInputChange = { terminalInput = it })
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
                                    Mt5Window()
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
                                    } else {
                                        minimizedWindows.remove(win)
                                        openWindow = win
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
        DesktopWindow.COMMAND_PROMPT -> "Command Prompt"
        DesktopWindow.DX_DIAG -> "DirectX Diagnostic Tool (dxdiag)"
        DesktopWindow.BROWSER -> "Chromium Web Peramban"
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

@Composable
fun TerminalWindow(
    container: WineContainer,
    commandLogs: MutableList<String>,
    terminalInput: String,
    onInputChange: (String) -> Unit
) {
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
                onValueChange = onInputChange,
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
                                commandLogs.add("  ps1 script.ps1 - Run PowerShell script")
                            }
                            cmd == "dir" -> {
                                commandLogs.add(" Directory of C:\\Windows\\System32:")
                                commandLogs.add("07/16/2026  10:24 AM    <DIR>          .")
                                commandLogs.add("07/16/2026  10:24 AM    <DIR>          ..")
                                commandLogs.add("07/16/2026  10:24 AM           820,112 kernel32.dll")
                                commandLogs.add("07/16/2026  10:24 AM           640,992 user32.dll")
                                commandLogs.add("07/16/2026  10:24 AM           310,240 gdi32.dll")
                            }
                            cmd.startsWith("ps1") -> {
                                commandLogs.add("Execution Policy: Bypass...")
                                commandLogs.add("Loading script module... SUCCESS.")
                                commandLogs.add("Output: Rofwin PowerShell simulation active.")
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
                        onInputChange("")
                    }
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Execute Command", tint = Color.Green)
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
fun Mt5Window() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MetaTrader 5 - Demo Account (Connected)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Row(modifier = Modifier.weight(1f)) {
            // Market Watch
            Column(modifier = Modifier.width(150.dp).fillMaxHeight().border(1.dp, Color(0xFF333333)).padding(8.dp)) {
                Text("Market Watch", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("EURUSD   1.0924   1.0925", color = Color.Green, fontSize = 10.sp)
                Text("GBPUSD   1.2750   1.2752", color = Color.Green, fontSize = 10.sp)
                Text("USDJPY   145.30   145.31", color = Color.Red, fontSize = 10.sp)
            }
            // Chart Area
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("EURUSD, H1", color = Color.DarkGray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Draw a simple mock chart
                    drawLine(Color.Green, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.4f), strokeWidth = 2f)
                    drawLine(Color.Red, androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.4f), androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.7f), strokeWidth = 2f)
                    drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.7f), androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f), strokeWidth = 2f)
                }
            }
        }
        // Terminal / Toolbox
        Column(modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, Color(0xFF333333)).padding(8.dp)) {
            Text("Toolbox - Trade | Exposure | History | News", color = Color.LightGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Balance: 10000.00 USD  Equity: 10050.00 USD  Margin: 50.00", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Mql5EditorWindow() {
    var code by remember { mutableStateOf("//+------------------------------------------------------------------+\n//|                                                      Expert.mq5 |\n//|                                      Copyright 2026, MetaQuotes |\n//|                                             https://www.mql5.com |\n//+------------------------------------------------------------------+\n#property copyright \"Copyright 2026\"\n#property link      \"https://www.mql5.com\"\n#property version   \"1.00\"\n\n//+------------------------------------------------------------------+\n//| Expert initialization function                                   |\n//+------------------------------------------------------------------+\nint OnInit()\n  {\n   Print(\"Algo Editor Sync Activated!\");\n   return(INIT_SUCCEEDED);\n  }\n") }
    var syncStatus by remember { mutableStateOf("Syncing with Algo Cloud...") }
    LaunchedEffect(Unit) {
        delay(2000)
        syncStatus = "Algo Editor MQL5 Sync: Active & Connected"
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = Color.LightGray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MetaEditor - Expert.mq5", color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { /* compile mock */ }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                Text("Compile", fontSize = 10.sp)
            }
        }
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
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF007ACC)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(syncStatus, color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.weight(1f))
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
            Icon(Icons.Default.Language, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant),
                textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    var loadUrl = urlInput
                    if (!loadUrl.startsWith("http://") && !loadUrl.startsWith("https://")) {
                        loadUrl = "https://$loadUrl"
                    }
                    url = loadUrl
                    webView?.loadUrl(url)
                }),
                singleLine = true
            )
            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
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
                        loadUrl(url)
                        webView = this
                    }
                },
                update = {
                    // Update happens through the go button/keyboard action
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
