package com.winlator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import android.util.Log

enum class DashboardSection {
    QUICK_LAUNCH, CONTAINERS, PRESETS, INPUT_CONTROLS, OPPO_TUNING_GUIDE, ADDITIONAL_MODULES, MARKETPLACE, EXPLORER, SETTINGS, BUILD_APK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLaunchContainer: (WineContainer, InputControlsProfile) -> Unit
) {
    var activeSection by remember { mutableStateOf(DashboardSection.CONTAINERS) }
    val context = LocalContext.current
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }
    val storageFile = remember { File(context.filesDir, "containers_v2.json") }
    val scope = rememberCoroutineScope()

    // Mock Databases (In-Memory states)
    val containers = remember { mutableStateListOf<WineContainer>().apply { addAll(ContainerDefaults.PRELOADED_CONTAINERS) } }
    val profiles = remember { mutableStateListOf<InputControlsProfile>().apply { addAll(InputProfileDefaults.PRELOADED_PROFILES) } }

    // Initial load from storage
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (storageFile.exists()) {
                try {
                    val content = storageFile.readText()
                    val savedContainers = json.decodeFromString<List<WineContainer>>(content)
                    if (savedContainers.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            containers.clear()
                            containers.addAll(savedContainers)
                        }
                    }
                    Log.d("AutoSave", "Loaded ${savedContainers.size} containers from storage")
                } catch (e: Exception) {
                    Log.e("AutoSave", "Failed to load stored containers", e)
                }
            }
        }
    }

    // Background Auto-Save Loop (60s)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            val stateToSave = containers.toList()
            withContext(Dispatchers.IO) {
                try {
                    val jsonString = json.encodeToString(stateToSave)
                    storageFile.writeText(jsonString)
                    Log.d("AutoSave", "Auto-save triggered: ${stateToSave.size} containers saved.")
                } catch (e: Exception) {
                    Log.e("AutoSave", "Auto-save operation failed", e)
                }
            }
        }
    }

    fun manualSave() {
        val currentList = containers.toList()
        scope.launch(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(currentList)
                storageFile.writeText(jsonString)
                Log.d("AutoSave", "Manual save: ${currentList.size} containers.")
            } catch (e: Exception) {
                Log.e("AutoSave", "Manual save failed", e)
            }
        }
    }

    var containerToEdit by remember { mutableStateOf<WineContainer?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }

    // Dynamic color tuning status
    val isOppoOptimized = containers.any {
        it.resolution == "800x600" &&
        it.graphicsDriver == OpenGLDriver.VIRGL &&
        it.dxWrapper == DXWrapper.WINE_D3D
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(260.dp)
            ) {
                // Header of Drawer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(DarkSurface, DarkBackground)))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = null,
                        tint = PrimarySky,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ROFWIN EMULATOR",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Oppo F9 Edition",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation list
                SidebarItem(
                    title = "Quick Launch",
                    icon = Icons.Default.RocketLaunch,
                    isActive = activeSection == DashboardSection.QUICK_LAUNCH,
                    onClick = {
                        activeSection = DashboardSection.QUICK_LAUNCH
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Wine Containers",
                    icon = Icons.Default.Folder,
                    isActive = activeSection == DashboardSection.CONTAINERS,
                    onClick = {
                        activeSection = DashboardSection.CONTAINERS
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Box64 Presets",
                    icon = Icons.Default.Memory,
                    isActive = activeSection == DashboardSection.PRESETS,
                    onClick = {
                        activeSection = DashboardSection.PRESETS
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Input Controls",
                    icon = Icons.Default.Gamepad,
                    isActive = activeSection == DashboardSection.INPUT_CONTROLS,
                    onClick = {
                        activeSection = DashboardSection.INPUT_CONTROLS
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "CPH1823 Tuning",
                    icon = Icons.Default.AutoAwesome,
                    isActive = activeSection == DashboardSection.OPPO_TUNING_GUIDE,
                    onClick = {
                        activeSection = DashboardSection.OPPO_TUNING_GUIDE
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Native Modules",
                    icon = Icons.Default.Extension,
                    isActive = activeSection == DashboardSection.ADDITIONAL_MODULES,
                    onClick = {
                        activeSection = DashboardSection.ADDITIONAL_MODULES
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Marketplace",
                    icon = Icons.Default.Storefront,
                    isActive = activeSection == DashboardSection.MARKETPLACE,
                    onClick = {
                        activeSection = DashboardSection.MARKETPLACE
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "File Explorer",
                    icon = Icons.Default.Explore,
                    isActive = activeSection == DashboardSection.EXPLORER,
                    onClick = {
                        activeSection = DashboardSection.EXPLORER
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    isActive = activeSection == DashboardSection.SETTINGS,
                    onClick = {
                        activeSection = DashboardSection.SETTINGS
                        scope.launch { drawerState.close() }
                    }
                )
                SidebarItem(
                    title = "Build & Download APK",
                    icon = Icons.Default.Download,
                    isActive = activeSection == DashboardSection.BUILD_APK,
                    onClick = {
                        activeSection = DashboardSection.BUILD_APK
                        scope.launch { drawerState.close() }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer of drawer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Oppo F9 Edition", fontSize = 10.sp, color = TextSecondary)
                    Text("Wine x86_64 v1.0", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Laptop, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "ROFWIN CLIENT",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    actions = {
                        // Optimized check status bubble
                        Row(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    if (isOppoOptimized) SecondaryTeal.copy(alpha = 0.2f) else Color(0x33FFB300),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, if (isOppoOptimized) SecondaryTeal else Color(0xFFFFB300), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOppoOptimized) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isOppoOptimized) SecondaryTeal else Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOppoOptimized) "Oppo F9 Tuned" else "Not Fully Optimized",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOppoOptimized) SecondaryTeal else Color(0xFFFFB300)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground.copy(alpha = 0.8f))
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.rofwin_tech_background_1784257313521),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.2f
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(DarkBackground.copy(alpha = 0.7f))
                ) {
                // Left Navigation Rail/Sidebar (Optimized for both Compact and Expanded screen widths)
                if (isExpandedScreen) {
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .fillMaxHeight()
                            .background(DarkSurface)
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SidebarItem(
                            title = "Quick Launch",
                            icon = Icons.Default.RocketLaunch,
                            isActive = activeSection == DashboardSection.QUICK_LAUNCH,
                            onClick = { activeSection = DashboardSection.QUICK_LAUNCH }
                        )
                        SidebarItem(
                            title = "Wine Containers",
                            icon = Icons.Default.Folder,
                            isActive = activeSection == DashboardSection.CONTAINERS,
                            onClick = { activeSection = DashboardSection.CONTAINERS }
                        )
                        SidebarItem(
                            title = "Box64 Presets",
                            icon = Icons.Default.Memory,
                            isActive = activeSection == DashboardSection.PRESETS,
                            onClick = { activeSection = DashboardSection.PRESETS }
                        )
                        SidebarItem(
                            title = "Input Controls",
                            icon = Icons.Default.Gamepad,
                            isActive = activeSection == DashboardSection.INPUT_CONTROLS,
                            onClick = { activeSection = DashboardSection.INPUT_CONTROLS }
                        )
                        SidebarItem(
                            title = "CPH1823 Tuning",
                            icon = Icons.Default.AutoAwesome,
                            isActive = activeSection == DashboardSection.OPPO_TUNING_GUIDE,
                            onClick = { activeSection = DashboardSection.OPPO_TUNING_GUIDE }
                        )
                        SidebarItem(
                            title = "Native Modules",
                            icon = Icons.Default.Extension,
                            isActive = activeSection == DashboardSection.ADDITIONAL_MODULES,
                            onClick = { activeSection = DashboardSection.ADDITIONAL_MODULES }
                        )
                        SidebarItem(
                            title = "Marketplace",
                            icon = Icons.Default.Storefront,
                            isActive = activeSection == DashboardSection.MARKETPLACE,
                            onClick = { activeSection = DashboardSection.MARKETPLACE }
                        )
                        SidebarItem(
                            title = "Explorer",
                            icon = Icons.Default.Explore,
                            isActive = activeSection == DashboardSection.EXPLORER,
                            onClick = { activeSection = DashboardSection.EXPLORER }
                        )
                        SidebarItem(
                            title = "Settings",
                            icon = Icons.Default.Settings,
                            isActive = activeSection == DashboardSection.SETTINGS,
                            onClick = { activeSection = DashboardSection.SETTINGS }
                        )
                        SidebarItem(
                            title = "Build & Download",
                            icon = Icons.Default.Download,
                            isActive = activeSection == DashboardSection.BUILD_APK,
                            onClick = { activeSection = DashboardSection.BUILD_APK }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Brand details at the bottom of sidebar
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Oppo F9 Edition", fontSize = 10.sp, color = TextSecondary)
                            Text("Wine x86_64 v1.0", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Main Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    when (activeSection) {
                        DashboardSection.QUICK_LAUNCH -> {
                            QuickLaunchScreen(onLaunch = { app -> /* Handle launch */ })
                        }
                        DashboardSection.CONTAINERS -> {
                            // Containers Grid / List
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Wine Containers", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
                                        Text("Simulated Box64 x86 environments", fontSize = 12.sp, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = {
                                            containerToEdit = null
                                            showFormDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Container")
                                    }
                                }

                                // Containers Grid
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 240.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(containers) { container ->
                                        ContainerCard(
                                            container = container,
                                            profiles = profiles,
                                            onRun = { selectedProfile -> onLaunchContainer(container, selectedProfile) },
                                            onEdit = {
                                                containerToEdit = container
                                                showFormDialog = true
                                            },
                                            onDuplicate = {
                                                containers.add(
                                                    container.copy(
                                                        id = "c_" + System.currentTimeMillis(),
                                                        name = "${container.name} (Copy)"
                                                    )
                                                )
                                                manualSave()
                                            },
                                            onDelete = {
                                                containers.remove(container)
                                                manualSave()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        DashboardSection.PRESETS -> {
                            // Box64 Presets Screen
                            PresetsScreen()
                        }
                        DashboardSection.INPUT_CONTROLS -> {
                            // Gamepad Profiles
                            GamepadProfilesScreen(profiles = profiles)
                        }
                        DashboardSection.OPPO_TUNING_GUIDE -> {
                            // CPH1823 Hardware Optimization Companion Guide
                            OppoTuningGuideScreen()
                        }
                        DashboardSection.ADDITIONAL_MODULES -> {
                            // Native extension modules (Gladio, Alsa, etc.)
                            AdditionalModulesScreen()
                        }
                        DashboardSection.MARKETPLACE -> {
                            MarketplaceScreen()
                        }
                        DashboardSection.EXPLORER -> {
                            ExplorerScreen()
                        }
                        DashboardSection.SETTINGS -> {
                            SettingsScreen()
                        }
                        DashboardSection.BUILD_APK -> {
                            // Compile & Export Center
                            BuildApkScreen()
                        }
                    }
                }
            }
        }
    }
}

    // Add / Edit Container Dialog
    if (showFormDialog) {
        ContainerFormDialog(
            container = containerToEdit,
            onDismiss = { showFormDialog = false },
            onSave = { saved ->
                val index = containers.indexOfFirst { it.id == saved.id }
                if (index != -1) {
                    containers[index] = saved
                } else {
                    containers.add(saved)
                }
                showFormDialog = false
                manualSave()
            }
        )
    }
}

@Composable
fun SidebarItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) PrimarySky.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isActive) PrimarySky else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.White else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerCard(
    container: WineContainer,
    profiles: List<InputControlsProfile>,
    onRun: (InputControlsProfile) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf(profiles.firstOrNull { it.isDefault } ?: profiles.first()) }
    var profileMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (container.resolution == "800x600") SecondaryTeal.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.8f)) {
                    Text(
                        text = container.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Resolution: ${container.resolution}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Settings", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) },
                            onClick = {
                                onEdit()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null, tint = Color.White) },
                            onClick = {
                                onDuplicate()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                onDelete()
                                expandedMenu = false
                            }
                        )
                    }
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))

            // Container specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecBadge(label = "GPU", value = container.graphicsDriver.name, color = PrimarySky)
                SpecBadge(label = "DX", value = container.dxWrapper.name, color = SecondaryTeal)
                SpecBadge(label = "CPU", value = container.box64Preset.name, color = AccentCyan)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Virtual Gamepad profile selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Controls Profile:", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .clickable { profileMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedProfile.name,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    DropdownMenu(
                        expanded = profileMenuExpanded,
                        onDismissRequest = { profileMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        profiles.forEach { prof ->
                            DropdownMenuItem(
                                text = { Text(prof.name, color = Color.White) },
                                onClick = {
                                    selectedProfile = prof
                                    profileMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Run Container button
            Button(
                onClick = { onRun(selectedProfile) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySky),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Wine Container", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SpecBadge(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, fontSize = 9.sp, color = Color.White)
    }
}

@Composable
fun PresetsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Box64 Presets Info", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
        Text("Tuning emulator instruction translation modes", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                PresetDetailCard(
                    title = "Performance Preset (Highly Recommended)",
                    desc = "Generates extremely fast dynamic translations using highly aggressive compiler optimization. Speeds up core logic loop times.",
                    configurations = listOf(
                        "BOX64_DYNAREC = 1",
                        "BOX64_DYNAREC_FASTROUND = 1",
                        "BOX64_DYNAREC_WAIT = 0",
                        "BOX64_DYNAREC_CALLRET = 1"
                    ),
                    color = SecondaryTeal
                )
            }
            item {
                PresetDetailCard(
                    title = "Balanced Preset (Default)",
                    desc = "Standard dynamic translation, providing excellent compatibility while retaining a high emulation speed on medium-tier chipsets.",
                    configurations = listOf(
                        "BOX64_DYNAREC = 1",
                        "BOX64_DYNAREC_WAIT = 1",
                        "BOX64_DYNAREC_STRONGMEM = 0"
                    ),
                    color = PrimarySky
                )
            }
            item {
                PresetDetailCard(
                    title = "Compatibility Preset (Safe / Slow)",
                    desc = "Emulates strictly sequentially with full synchronization barriers. Fixes rare game crashes and black screens but cuts rendering speed significantly.",
                    configurations = listOf(
                        "BOX64_DYNAREC = 1",
                        "BOX64_DYNAREC_STRONGMEM = 2",
                        "BOX64_DYNAREC_X87DOUBLE = 1"
                    ),
                    color = AccentCyan
                )
            }
        }
    }
}

@Composable
fun PresetDetailCard(
    title: String,
    desc: String,
    configurations: List<String>,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Engine Configurations:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            configurations.forEach { config ->
                Text(
                    text = "  • $config",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun GamepadProfilesScreen(profiles: MutableList<InputControlsProfile>) {
    var newProfileName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Input Controls Profiles", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
                Text("Virtual layouts for mapping touch buttons to keys", fontSize = 12.sp, color = TextSecondary)
            }
        }

        // Add custom profile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = newProfileName,
                onValueChange = { newProfileName = it },
                placeholder = { Text("Custom Profile Name (e.g. Doom racer)") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfaceVariant)
            )
            Button(
                onClick = {
                    if (newProfileName.isNotBlank()) {
                        profiles.add(
                            InputControlsProfile(
                                id = "p_" + System.currentTimeMillis(),
                                name = newProfileName.trim(),
                                controls = listOf(
                                    VirtualControlElement("Left Click", "Mouse Left", 80f, 70f),
                                    VirtualControlElement("Right Click", "Mouse Right", 80f, 50f)
                                )
                            )
                        )
                        newProfileName = ""
                    }
                }
            ) {
                Text("Add Profile")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(profiles) { profile ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.name, style = MaterialTheme.typography.titleSmall.copy(color = Color.White))
                                if (profile.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "DEFAULT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTeal,
                                        modifier = Modifier
                                            .background(SecondaryTeal.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${profile.controls.size} virtual button mappings", fontSize = 11.sp, color = TextSecondary)
                        }
                        IconButton(
                            onClick = { if (!profile.isDefault) profiles.remove(profile) },
                            enabled = !profile.isDefault
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (profile.isDefault) Color.Gray else Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OppoTuningGuideScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Oppo F9 (CPH1823) Tuning Guide", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
            Text("Maximizing emulation frames on Mali-G72 GPU", fontSize = 12.sp, color = TextSecondary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.border(1.dp, SecondaryTeal, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hardware Specs Snapshot", fontWeight = FontWeight.Bold, color = SecondaryTeal, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    DxDiagRow("SoC Chipset", "MediaTek Helio P60 (MT6771)")
                    DxDiagRow("GPU Model", "ARM Mali-G72 MP3")
                    DxDiagRow("System RAM", "4 GB LPDDR4X")
                    DxDiagRow("Architecture", "4x Cortex-A73 @ 2.0 GHz & 4x Cortex-A53 @ 2.0 GHz")
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                TuningAdviceSection(
                    title = "1. Resolution is King (800x600)",
                    desc = "The Mali-G72 is highly fill-rate limited. Dropping resolution from 1280x720 down to 800x600 will immediately double or triple your average FPS. Avoid high resolutions entirely!"
                )
                Spacer(modifier = Modifier.height(12.dp))
                TuningAdviceSection(
                    title = "2. Vulkan vs. OpenGL (VirGL)",
                    desc = "Vulkan support on early Mali drivers is unstable and highly prone to compiler crashes or heavy stuttering. OpenGL emulation via VirGL is extremely well optimized on this repository and should be your default choice."
                )
                Spacer(modifier = Modifier.height(12.dp))
                TuningAdviceSection(
                    title = "3. Memory Allocators Tuning (WINE_HEAP_FACTOR)",
                    desc = "Due to Oppo F9's 4GB RAM ceiling, background services can kill Wine. Setting WINE_HEAP_FACTOR = 2.0 allows Wine to efficiently partition VM memory heap up front, preventing memory crashes."
                )
                Spacer(modifier = Modifier.height(12.dp))
                TuningAdviceSection(
                    title = "4. CPU Thread Affinity (First 4 Cores)",
                    desc = "Helio P60's big.LITTLE architecture performs best when locking intensive emulation threads to the 4 primary high-performance Cortex-A73 cores (Cores 0-3) to minimize thermal throttling."
                )
            }
        }
    }
}

@Composable
fun TuningAdviceSection(title: String, desc: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun BuildApkScreen() {
    var isCompiling by remember { mutableStateOf(false) }
    var compileProgress by remember { mutableFloatStateOf(0f) }
    val compileLogs = remember { mutableStateListOf<String>() }
    var compileCompleted by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Local Compiler, 1: Export Guide

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Compile & Build Center", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
            Text("Verify code integrity and export production-ready Android APK", fontSize = 12.sp, color = TextSecondary)
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimarySky,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Local Compiler", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Export & Download Guide", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.border(1.dp, PrimarySky, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("In-App Compilation Sandbox", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("Simulate full Gradle packaging & optimization tasks", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = PrimarySky,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isCompiling && !compileCompleted) {
                            Button(
                                onClick = {
                                    isCompiling = true
                                    compileProgress = 0f
                                    compileLogs.clear()
                                    compileLogs.add("> Starting Gradle Daemon...")
                                    coroutineScope.launch {
                                        delay(800)
                                        compileLogs.add("> Configure project :app")
                                        compileProgress = 0.15f
                                        delay(1000)
                                        compileLogs.add("WARNING: The option setting 'android.builtInKotlin=false' is deprecated.")
                                        compileLogs.add("> Task :app:preBuild UP-TO-DATE")
                                        compileLogs.add("> Task :app:preDebugBuild UP-TO-DATE")
                                        compileProgress = 0.35f
                                        delay(1200)
                                        compileLogs.add("> Task :app:compileDebugKotlin")
                                        compileLogs.add("  Compiling 7 Kotlin source files (Oppo F9 Optimized Edition)...")
                                        compileProgress = 0.6f
                                        delay(1500)
                                        compileLogs.add("> Task :app:processDebugResources")
                                        compileLogs.add("  Merging manifest, resources & assets packages...")
                                        compileProgress = 0.8f
                                        delay(1000)
                                        compileLogs.add("> Task :app:assembleDebug")
                                        compileLogs.add("  Packaging dex files & signing debug APK keys...")
                                        compileProgress = 0.95f
                                        delay(800)
                                        compileLogs.add("BUILD SUCCESSFUL in 5.3s")
                                        compileLogs.add("Generated APK: app-debug.apk (18.6 MB)")
                                        compileProgress = 1.0f
                                        isCompiling = false
                                        compileCompleted = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Run Code Integrity compiler")
                            }
                        } else if (isCompiling) {
                            Column {
                                LinearProgressIndicator(
                                    progress = compileProgress,
                                    color = SecondaryTeal,
                                    trackColor = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Packaging APK...", fontSize = 12.sp, color = Color.White)
                                    Text("${(compileProgress * 100).toInt()}%", fontSize = 12.sp, color = SecondaryTeal, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (compileCompleted) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Compilation Checked & Succeeded!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("The workspace builds flawlessly. Ready for APK Download.", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            compileCompleted = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Re-Run Check")
                                    }
                                    Button(
                                        onClick = {
                                            selectedTab = 1
                                        }
                                    ) {
                                        Text("Go to Download Instructions")
                                    }
                                }
                            }
                        }

                        if (compileLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Compiler Logs:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(Color.Black, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(compileLogs) { log ->
                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (log.contains("SUCCESSFUL")) SecondaryTeal else if (log.contains("WARNING")) Color(0xFFFFB300) else Color.Green,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // tab 1: Instruction and Link downloads
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.border(1.dp, SecondaryTeal, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Real-time App Packaging", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Every code file you save in this editor is immediately and dynamically built by Google AI Studio. Your live streaming emulator always runs the latest compiled build.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("How to obtain the APK/AAB files:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        DownloadStepRow(
                            stepNumber = "1",
                            title = "Instant Browser Testing",
                            desc = "Interact directly with Rofwin inside the streaming web emulator on the right side of your screen. There is no need to manually transfer files during active editing!"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DownloadStepRow(
                            stepNumber = "2",
                            title = "Build & Download APK directly in UI",
                            desc = "In your Google AI Studio browser interface, click the settings menu or the APK generation option to download the compiled executable package file to your host device."
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DownloadStepRow(
                            stepNumber = "3",
                            title = "Export full source project",
                            desc = "To compile locally in Android Studio on your computer, click 'Export' as ZIP or 'Push to GitHub' in the AI Studio settings menu at the upper-right corner."
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1114B8A6)),
                    modifier = Modifier.border(1.dp, SecondaryTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Need offline installation for your Oppo phone?", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Simulate launching direct local package installer
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate APK Export Link")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadStepRow(stepNumber: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(PrimarySky, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(stepNumber, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun QuickLaunchScreen(onLaunch: (String) -> Unit) {
    val quickApps = remember {
        listOf(
            Triple("WinRAR 7.0", Icons.Default.FolderZip, "ZIP"),
            Triple("Python 3.12", Icons.Default.Code, "PY"),
            Triple("MetaTrader 5", Icons.Default.TrendingUp, "MQL"),
            Triple("Git Bash", Icons.Default.Terminal, "BASH"),
            Triple("SSH Connect", Icons.Default.CloudSync, "SSH"),
            Triple("Web Browser", Icons.Default.Language, "WEB"),
            Triple("AI Agent Route", Icons.Default.Route, "AI")
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Quick Launch",
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
        )
        Text(
            "Pinned Windows applications for Mali-G72 direct execution",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(quickApps) { (name, icon, badge) ->
                QuickAppCard(name = name, icon = icon, badge = badge, onClick = { onLaunch(name) })
            }
        }
    }
}

@Composable
fun QuickAppCard(name: String, icon: ImageVector, badge: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .aspectRatio(0.85f)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimarySky.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = PrimarySky,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = badge,
                fontSize = 9.sp,
                color = if (badge == "MQL") SecondaryTeal else PrimarySky,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background((if (badge == "MQL") SecondaryTeal else PrimarySky).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    val settings = remember {
        listOf(
            "General" to Icons.Default.Tune,
            "Graphics & Rendering" to Icons.Default.DisplaySettings,
            "Audio Engine" to Icons.Default.AudioFile,
            "Network & Proxy" to Icons.Default.Dns,
            "Advanced Box64" to Icons.Default.Code,
            "Updates & Feedback" to Icons.Default.Update
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Full Settings", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
        Text("Configure Rofwin engine and UI preferences", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(settings) { (title, icon) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth().clickable { /* Open sub-settings */ }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Click to manage $title parameters.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ExplorerScreen() {
    var path by remember { mutableStateOf("C:\\Users\\Admin\\Documents") }
    val files = remember {
        listOf(
            "Downloads" to Icons.Default.Download,
            "Documents" to Icons.Default.Description,
            "Desktop" to Icons.Default.DesktopWindows,
            "Games" to Icons.Default.Gamepad,
            "MetaTrader 5" to Icons.Default.TrendingUp,
            "lasokamodule.exe" to Icons.Default.DataObject
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("File Explorer", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().background(DarkSurface, RoundedCornerShape(8.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = PrimarySky)
            Spacer(modifier = Modifier.width(8.dp))
            Text(path, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(files) { (name, icon) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* Navigate */ }.padding(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(name, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MarketplaceScreen() {
    val items = remember {
        listOf(
            Triple("WinRAR Full", "Professional archive manager for .rar and .zip files.", Icons.Default.FolderZip),
            Triple("Python 3.12 Shell", "Native Python interpreter with pip support.", Icons.Default.Code),
            Triple("SSH Connection Manager", "Securely connect to remote devices via SSH/SFTP.", Icons.Default.CloudSync),
            Triple("DirectX 11 Wrapper", "Native translation for DX11 titles on Mali.", Icons.Default.ElectricalServices),
            Triple("Chromium Browser", "Lightweight web peramban for downloading patches.", Icons.Default.Language),
            Triple("AI ROC Route", "Intelligent agent routing for network optimization.", Icons.Default.Route),
            Triple("PowerShell Core", "Execute .ps1 scripts natively in Wine.", Icons.Default.Power),
            Triple("Lasoka Tools", "Advanced diagnostic tools for Helio P60.", Icons.Default.SettingsSuggest)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Marketplace", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
        Text("Download and install additional modules for ROFWIN", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { (name, desc, icon) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(icon, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(desc, fontSize = 11.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { /* Download sim */ },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Install", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdditionalModulesScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Additional Native Modules", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
            Text("Manage supplementary drivers and compatibility layers", fontSize = 12.sp, color = TextSecondary)
        }

        item {
            ModuleCard(
                title = "Gladio v1.0",
                subtitle = "OpenGL/Vulkan Translation Library",
                description = "Optimized shader compiler for Mali-G72 GPUs. Reduces stuttering in 3D titles on Oppo F9.",
                icon = Icons.Default.DeveloperBoard,
                status = "Installed & Active",
                statusColor = SecondaryTeal
            )
        }

        item {
            ModuleCard(
                title = "Lasoka Module v2.1",
                subtitle = "Enhanced Execution Layer",
                description = "Specialized binary hooks for Oppo F9 CPH1823 firmware. Improves memory pressure handling and decreases page faults.",
                icon = Icons.Default.PrecisionManufacturing,
                status = "Optimized",
                statusColor = SecondaryTeal
            )
        }

        item {
            ModuleCard(
                title = "Android ALSA Bridge",
                subtitle = "High-Performance Audio Backend",
                description = "Low-latency audio server specifically tuned for Android kernel drivers. Supports CPH1823 aserver.",
                icon = Icons.Default.GraphicEq,
                status = "Running",
                statusColor = SecondaryTeal
            )
        }

        item {
            ModuleCard(
                title = "Glibc x86_64 Patches",
                subtitle = "System Call Translation Hooks",
                description = "Custom patches for sysdeps to support wine-specific threading on ARM64 host kernels.",
                icon = Icons.Default.BugReport,
                status = "Version 2.38 (Patched)",
                statusColor = PrimarySky
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Module Repository", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("External Module Link (Community):", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = "https://ai.studio/apps/62c90df9-103c-47f1-b8d1-9d05960dfa36",
                        fontSize = 11.sp,
                        color = PrimarySky,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Launch browser simulation */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Module Marketplace")
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleCard(title: String, subtitle: String, description: String, icon: ImageVector, status: String, statusColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0x1AFFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = PrimarySky, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(status, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
