package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CameraSyncScreen
import com.example.ui.screens.ExportHistoryScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.LiveGpsCameraScreen
import com.example.ui.screens.WatermarkEditorScreen
import com.example.ui.theme.GpsCamTheme
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SafetyOrangeBright
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.TechCyan
import com.example.viewmodel.GpsCamViewModel
import kotlinx.coroutines.flow.collectLatest

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    SYNC("sync", "Cam Sync", Icons.Default.Wifi),
    GALLERY("gallery", "Gallery", Icons.Default.PhotoLibrary),
    CAMERA("camera", "GPS Cam", Icons.Default.CameraAlt),
    EDITOR("editor", "Watermark", Icons.Default.Edit),
    EXPORTS("exports", "Exports", Icons.Default.FolderShared)
}

class MainActivity : ComponentActivity() {

    private val viewModel: GpsCamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GpsCamTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var currentDestination by remember { mutableStateOf(AppDestination.GALLERY) }

                val allImages by viewModel.allImages.collectAsState()
                val markedCount = allImages.count { it.isMarked }

                // Listen for Toast / Snackbar UI events
                LaunchedEffect(Unit) {
                    viewModel.uiEvents.collectLatest { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Slate900,
                            contentColor = Slate200,
                            tonalElevation = 8.dp
                        ) {
                            AppDestination.values().forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        if (dest == AppDestination.GALLERY && markedCount > 0) {
                                            BadgedBox(badge = { Badge(containerColor = SafetyOrange) { Text("$markedCount") } }) {
                                                Icon(dest.icon, contentDescription = dest.label)
                                            }
                                        } else {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    },
                                    label = { Text(dest.label, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_${dest.route}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Slate950,
                                        selectedTextColor = SafetyOrangeBright,
                                        indicatorColor = SafetyOrange,
                                        unselectedIconColor = Slate400,
                                        unselectedTextColor = Slate400
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentDestination) {
                            AppDestination.SYNC -> {
                                CameraSyncScreen(
                                    viewModel = viewModel,
                                    onNavigateToGallery = { currentDestination = AppDestination.GALLERY }
                                )
                            }
                            AppDestination.GALLERY -> {
                                GalleryScreen(
                                    viewModel = viewModel,
                                    onNavigateToEditor = { currentDestination = AppDestination.EDITOR },
                                    onNavigateToCameraSync = { currentDestination = AppDestination.SYNC }
                                )
                            }
                            AppDestination.CAMERA -> {
                                LiveGpsCameraScreen(
                                    viewModel = viewModel,
                                    onNavigateToEditor = { currentDestination = AppDestination.EDITOR }
                                )
                            }
                            AppDestination.EDITOR -> {
                                WatermarkEditorScreen(
                                    viewModel = viewModel,
                                    onBack = { currentDestination = AppDestination.GALLERY }
                                )
                            }
                            AppDestination.EXPORTS -> {
                                ExportHistoryScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
