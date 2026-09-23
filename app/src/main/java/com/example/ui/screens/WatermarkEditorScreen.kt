package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WatermarkConfig
import com.example.data.model.WatermarkFontSize
import com.example.data.model.WatermarkPosition
import com.example.data.model.WatermarkTextColor
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
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
import com.example.ui.theme.WarningAmber
import com.example.util.ExifHelper
import com.example.viewmodel.GpsCamViewModel

@Composable
fun WatermarkEditorScreen(
    viewModel: GpsCamViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeImage by viewModel.activeImage.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val isGeneratingPreview by viewModel.isGeneratingPreview.collectAsState()
    val watermarkConfig by viewModel.watermarkConfig.collectAsState()
    val assetPresets by viewModel.assetPresets.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val markedCount = allImages.count { it.isMarked }

    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportStatusText by viewModel.exportStatusText.collectAsState()

    var showPresetMenu by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Text Fields, 1: Style & Layout

    if (activeImage == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Slate950),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No photo selected for watermark editing", color = Slate400)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange, contentColor = Slate950)) {
                    Text("Return to Media Gallery")
                }
            }
        }
        return
    }

    val item = activeImage!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Slate900,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_gallery_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate200)
                    }
                    Column {
                        Text(
                            text = "WATERMARK PREVIEW & EDITOR",
                            style = MaterialTheme.typography.titleSmall,
                            color = TechCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${item.cameraMake} ${item.cameraModel} • ${item.fileName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                // Phone GPS Fallback Stamp Button
                IconButton(
                    onClick = { viewModel.stampPhoneGpsLocation() },
                    modifier = Modifier.testTag("stamp_phone_gps_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Stamp Phone GPS",
                        tint = HighVisGreen
                    )
                }
            }
        }

        // Export Progress Banner if exporting
        if (isExporting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate800)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exportStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = SafetyOrangeBright,
                    fontFamily = FontFamily.Monospace
                )
                LinearProgressIndicator(
                    progress = { exportProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = SafetyOrange,
                    trackColor = Slate700
                )
            }
        }

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Interactive Preview Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.33f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "Live Watermarked Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else if (isGeneratingPreview) {
                        CircularProgressIndicator(color = TechCyan)
                    } else {
                        Text("Loading high-res preview...", color = Slate400)
                    }

                    // Live indicator watermark status pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate900.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HighVisGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE PREVIEW",
                                color = Slate200,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Sub-tabs: [Metadata & Text Fields] vs [Style & Watermark Layout]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeTab == 0) SafetyOrange else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1. METADATA & NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeTab == 0) Slate950 else Slate200,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeTab == 1) SafetyOrange else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "2. STYLE & BANNER",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeTab == 1) Slate950 else Slate200,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (activeTab == 0) {
                // Tab 0: Metadata & User Defined Fields
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "USER-DEFINED INSPECTION FIELDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TechCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        // Reference Number with Dropdown Presets
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reference Number", style = MaterialTheme.typography.bodySmall, color = Slate400)
                                Box {
                                    Text(
                                        text = "Select Asset Preset ▾",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber,
                                        modifier = Modifier
                                            .clickable { showPresetMenu = true }
                                            .testTag("open_presets_btn")
                                            .padding(4.dp)
                                    )
                                    DropdownMenu(
                                        expanded = showPresetMenu,
                                        onDismissRequest = { showPresetMenu = false },
                                        modifier = Modifier.background(Slate800)
                                    ) {
                                        assetPresets.forEach { preset ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(preset.code, color = WarningAmber, fontWeight = FontWeight.Bold)
                                                        if (preset.description.isNotBlank()) {
                                                            Text(preset.description, color = Slate400, fontSize = 11.sp)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.updateActiveImageDetails(referenceNumber = preset.code)
                                                    showPresetMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = item.referenceNumber,
                                onValueChange = { viewModel.updateActiveImageDetails(referenceNumber = it) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("ref_number_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate200,
                                    unfocusedTextColor = Slate200,
                                    focusedBorderColor = WarningAmber,
                                    unfocusedBorderColor = Slate700
                                )
                            )
                        }

                        // Field Notes & Remarks
                        Column {
                            Text("Field Notes & Remarks", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            OutlinedTextField(
                                value = item.fieldNotes,
                                onValueChange = { viewModel.updateActiveImageDetails(fieldNotes = it) },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("field_notes_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate200,
                                    unfocusedTextColor = Slate200,
                                    focusedBorderColor = TechCyan,
                                    unfocusedBorderColor = Slate700
                                )
                            )
                        }

                        // Inspector Name
                        Column {
                            Text("Inspector Identifier / Tag", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            OutlinedTextField(
                                value = item.inspectorName,
                                onValueChange = { viewModel.updateActiveImageDetails(inspectorName = it) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("inspector_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate200,
                                    unfocusedTextColor = Slate200,
                                    focusedBorderColor = TechCyan,
                                    unfocusedBorderColor = Slate700
                                )
                            )
                        }

                        // Reverse Geocoded Location Address
                        Column {
                            Text("Location / District Region", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            OutlinedTextField(
                                value = item.locationAddress,
                                onValueChange = { viewModel.updateActiveImageDetails(locationAddress = it) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate200,
                                    unfocusedTextColor = Slate200,
                                    focusedBorderColor = TechCyan,
                                    unfocusedBorderColor = Slate700
                                )
                            )
                        }

                        // Coordinates Summary Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate800)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "EXIF GPS TELEMETRY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HighVisGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Lock: HIGH ACCURACY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "LAT : ${item.latitude?.let { ExifHelper.toDms(it, true) }} (${item.latitude?.let { ExifHelper.toDecimal(it, true) }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate200,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "LONG: ${item.longitude?.let { ExifHelper.toDms(it, false) }} (${item.longitude?.let { ExifHelper.toDecimal(it, false) }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate200,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "DATE: ${item.dateTimeOriginal} | ALT: ${item.altitudeMeters ?: 0.0}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Watermark Styling, Position, Contrast Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "WATERMARK OVERLAY STYLING",
                            style = MaterialTheme.typography.labelSmall,
                            color = TechCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        // Placement Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Overlay Placement", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    WatermarkPosition.BOTTOM_FULL_BANNER to "Bottom Banner",
                                    WatermarkPosition.BOTTOM_LEFT to "Bottom Left",
                                    WatermarkPosition.BOTTOM_RIGHT to "Bottom Right",
                                    WatermarkPosition.TOP_FULL_BANNER to "Top Banner"
                                ).forEach { (pos, label) ->
                                    FilterChip(
                                        selected = watermarkConfig.position == pos,
                                        onClick = { viewModel.updateWatermarkConfig(watermarkConfig.copy(position = pos)) },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SafetyOrange.copy(alpha = 0.25f),
                                            selectedLabelColor = SafetyOrangeBright,
                                            containerColor = Slate800,
                                            labelColor = Slate400
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = watermarkConfig.position == pos,
                                            borderColor = Slate700,
                                            selectedBorderColor = SafetyOrange
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = Slate800)

                        // Font Size Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Font Size", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    WatermarkFontSize.COMPACT to "Compact",
                                    WatermarkFontSize.STANDARD to "Standard",
                                    WatermarkFontSize.LARGE to "Large",
                                    WatermarkFontSize.EXTRA_LARGE to "XL"
                                ).forEach { (size, label) ->
                                    FilterChip(
                                        selected = watermarkConfig.fontSize == size,
                                        onClick = { viewModel.updateWatermarkConfig(watermarkConfig.copy(fontSize = size)) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TechCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = TechCyan,
                                            containerColor = Slate800,
                                            labelColor = Slate400
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = watermarkConfig.fontSize == size,
                                            borderColor = Slate700,
                                            selectedBorderColor = TechCyan
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = Slate800)

                        // Text Color Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Text Highlight Color", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WatermarkTextColor.values().forEach { col ->
                                    val isSelected = watermarkConfig.textColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(col.argb))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) SafetyOrange else Slate700,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.updateWatermarkConfig(watermarkConfig.copy(textColor = col)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Slate800)

                        // Contrast Banner Opacity Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dark Contrast Banner Opacity", style = MaterialTheme.typography.bodySmall, color = Slate400)
                                Text("${(watermarkConfig.bannerOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Slate200)
                            }
                            Slider(
                                value = watermarkConfig.bannerOpacity,
                                onValueChange = { viewModel.updateWatermarkConfig(watermarkConfig.copy(bannerOpacity = it)) },
                                valueRange = 0.3f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SafetyOrange,
                                    activeTrackColor = SafetyOrange,
                                    inactiveTrackColor = Slate700
                                )
                            )
                        }

                        // Feature Toggles (DMS + Decimal, Camera specs, Address)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Include Address / District", color = Slate200, style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = watermarkConfig.showAddress,
                                    onCheckedChange = { viewModel.updateWatermarkConfig(watermarkConfig.copy(showAddress = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SafetyOrange, checkedTrackColor = Slate800)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Camera Hardware Specs", color = Slate200, style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = watermarkConfig.showCameraSpecs,
                                    onCheckedChange = { viewModel.updateWatermarkConfig(watermarkConfig.copy(showCameraSpecs = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SafetyOrange, checkedTrackColor = Slate800)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Spacing
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Bottom Fixed Save Actions: [Save Single Photo] or [Batch Save All Marked (X)]
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Slate900,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Single Save Button
                OutlinedButton(
                    onClick = { viewModel.exportActiveImage { } },
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f).testTag("save_single_btn"),
                    border = BorderStroke(1.dp, TechCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TechCyan)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Photo")
                }

                // Batch Save Button (if marked items exist)
                Button(
                    onClick = {
                        if (markedCount > 0) {
                            viewModel.exportAllMarkedImages { }
                        } else {
                            viewModel.exportActiveImage { }
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f).testTag("batch_save_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafetyOrange,
                        contentColor = Slate950
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (markedCount > 1) "Batch Save ($markedCount)" else "Burn & Export",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
