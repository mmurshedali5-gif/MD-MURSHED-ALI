package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.HighVisGreen
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SafetyOrangeBright
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.TechCyan
import com.example.util.SupportedCameraBrand
import com.example.viewmodel.GpsCamViewModel

@Composable
fun CameraSyncScreen(
    viewModel: GpsCamViewModel,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val allImages by viewModel.allImages.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { viewModel.importFromExternalUri(it) }
            onNavigateToGallery()
        }
    }

    var showGuideDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card with generated visual asset
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_camera_sync_hero),
                    contentDescription = "Camera Wi-Fi Sync Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Slate950.copy(alpha = 0.85f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CAMERA WI-FI SYNC HUB",
                        style = MaterialTheme.typography.titleMedium,
                        color = TechCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Lossless PTP-IP Transfer • Canon & Nikon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate200
                    )
                }
            }
        }

        // Camera Brand Selector (Canon SX70 HS vs Nikon P950)
        Text(
            text = "SELECT CAMERA PROFILE",
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CameraBrandCard(
                brand = SupportedCameraBrand.CANON_SX70,
                isSelected = connectionState.selectedBrand == SupportedCameraBrand.CANON_SX70,
                onClick = { viewModel.selectCameraBrand(SupportedCameraBrand.CANON_SX70) },
                modifier = Modifier.weight(1f)
            )
            CameraBrandCard(
                brand = SupportedCameraBrand.NIKON_P950,
                isSelected = connectionState.selectedBrand == SupportedCameraBrand.NIKON_P950,
                onClick = { viewModel.selectCameraBrand(SupportedCameraBrand.NIKON_P950) },
                modifier = Modifier.weight(1f)
            )
        }

        // Wi-Fi Connection Manager Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (connectionState.isConnected) HighVisGreen.copy(alpha = 0.2f) else Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (connectionState.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (connectionState.isConnected) HighVisGreen else Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = connectionState.selectedBrand.modelName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate200,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PTP-IP Protocol • Port ${connectionState.selectedBrand.ptpPort}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Status pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (connectionState.isConnected) HighVisGreen.copy(alpha = 0.2f) else Slate800)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (connectionState.isConnected) "LINKED" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectionState.isConnected) HighVisGreen else Slate400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Divider(color = Slate800)

                // IP Address input
                OutlinedTextField(
                    value = connectionState.currentIp,
                    onValueChange = { viewModel.updateCameraIp(it) },
                    label = { Text("Camera AP IP Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("camera_ip_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate200,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = Slate700,
                        focusedLabelColor = TechCyan,
                        unfocusedLabelColor = Slate400
                    )
                )

                // Status message
                Text(
                    text = connectionState.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TechCyan,
                    fontFamily = FontFamily.Monospace
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testCameraConnection() },
                        enabled = !connectionState.isConnecting,
                        modifier = Modifier.weight(1f).testTag("test_conn_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TechCyan),
                        border = BorderStroke(1.dp, TechCyan)
                    ) {
                        if (connectionState.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TechCyan)
                        } else {
                            Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ping Link")
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.pullImagesFromCamera()
                            onNavigateToGallery()
                        },
                        enabled = !connectionState.isConnecting,
                        modifier = Modifier.weight(1f).testTag("pull_photos_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafetyOrange,
                            contentColor = Slate950
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Photos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Secondary Import Options (Local Storage / SD Card via Photo Picker)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "STORAGE & SD CARD IMPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Import raw or full-resolution photos directly from Camera SD card or internal files. EXIF GPS coordinates, shutter, aperture, and ISO will be automatically extracted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("import_storage_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TechCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photos from Phone / SD Card")
                }
            }
        }

        // Field Guide Expandable Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGuideDialog = !showGuideDialog },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TechCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Field Connection Setup Guide",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate200,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = if (showGuideDialog) "Hide" else "Show",
                        style = MaterialTheme.typography.labelSmall,
                        color = TechCyan
                    )
                }

                AnimatedVisibility(visible = showGuideDialog) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "• Canon PowerShot SX70 HS:\n  1. Press Mobile Device Connect button on camera.\n  2. Camera broadcasts direct Wi-Fi SSID (e.g., Canon_SX70_...).\n  3. Connect phone Wi-Fi to camera SSID, then tap 'Sync Photos'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                        Text(
                            text = "• Nikon COOLPIX P950:\n  1. Go to Camera Menu > Network > Connect to smart device > Wi-Fi connection.\n  2. Note camera SSID and default IP (192.168.0.1).\n  3. Join Wi-Fi network and tap 'Sync Photos' to pull uncompressed files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                        Text(
                            text = "• 100% EXIF Retention:\n  Images maintain original shutter speed, aperture, ISO, and GPS coordinates without downsampling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighVisGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraBrandCard(
    brand: SupportedCameraBrand,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) SafetyOrange else CardBorder
    val bgColor = if (isSelected) Slate800 else CardDark

    Card(
        modifier = modifier.clickable { onClick() }.testTag("camera_brand_${brand.name}"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = brand.brandName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) SafetyOrange else Slate400,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = SafetyOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = brand.modelName,
                style = MaterialTheme.typography.titleSmall,
                color = Slate200,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "PTP-IP Port ${brand.ptpPort}",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}
