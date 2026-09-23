package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CapturedImageItem
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
fun GalleryScreen(
    viewModel: GpsCamViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToCameraSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allImages by viewModel.allImages.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredImages = when (selectedFilter) {
        "CANON" -> allImages.filter { it.cameraMake.contains("Canon", ignoreCase = true) }
        "NIKON" -> allImages.filter { it.cameraMake.contains("Nikon", ignoreCase = true) }
        "MOBILE" -> allImages.filter { it.sourceType == "MOBILE_CAM" }
        else -> allImages
    }

    val markedCount = allImages.count { it.isMarked }
    val allMarked = allImages.isNotEmpty() && allImages.all { it.isMarked }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Batch Control Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Slate900,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MEDIA GALLERY",
                                style = MaterialTheme.typography.titleMedium,
                                color = TechCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${allImages.size} imported photos • $markedCount marked",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }

                        // Select All / Deselect All Button
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.selectAllImages(!allMarked) },
                                modifier = Modifier.testTag("toggle_select_all_btn"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, if (allMarked) SafetyOrange else Slate700)
                            ) {
                                Icon(
                                    imageVector = if (allMarked) Icons.Default.RemoveDone else Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = if (allMarked) SafetyOrange else TechCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allMarked) "Deselect All" else "Select All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate200
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Source Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All (${allImages.size})",
                            "CANON" to "Canon SX70",
                            "NIKON" to "Nikon P950",
                            "MOBILE" to "GPS Cam"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SafetyOrange.copy(alpha = 0.25f),
                                    selectedLabelColor = SafetyOrangeBright,
                                    containerColor = Slate800,
                                    labelColor = Slate400
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == key,
                                    borderColor = Slate700,
                                    selectedBorderColor = SafetyOrange
                                )
                            )
                        }
                    }
                }
            }

            // Media Grid or Empty State
            if (filteredImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = TechCyan,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = "No Imported Photos Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate200,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sync photos over Wi-Fi from your Canon SX70 HS or Nikon P950, or import from phone storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToCameraSync,
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange, contentColor = Slate950)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync or Import Photos", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredImages, key = { it.id }) { item ->
                        GalleryItemCard(
                            item = item,
                            onToggleMark = { viewModel.toggleMark(item.id, item.isMarked) },
                            onClick = {
                                viewModel.setActiveImage(item)
                                onNavigateToEditor()
                            },
                            onDelete = { viewModel.deleteImage(item) }
                        )
                    }
                    // Bottom spacing so FAB / action bar doesn't overlap last row
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Bottom Action Bar: [Create Watermark / Proceed]
        if (allImages.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Slate900.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, Slate800)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (markedCount > 0) "$markedCount Photos Selected" else "Tap a photo to edit",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate200,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lossless EXIF Re-injection active",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighVisGreen,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            val target = allImages.firstOrNull { it.isMarked } ?: allImages.first()
                            viewModel.setActiveImage(target)
                            onNavigateToEditor()
                        },
                        modifier = Modifier.testTag("proceed_watermark_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafetyOrange,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (markedCount > 1) "Watermark ($markedCount)" else "Create Watermark",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryItemCard(
    item: CapturedImageItem,
    onToggleMark: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("gallery_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(if (item.isMarked) 2.dp else 1.dp, if (item.isMarked) SafetyOrange else CardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Thumbnail with overlay badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
            ) {
                AsyncImage(
                    model = item.fileUri,
                    contentDescription = item.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient shadow for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Top Left: Mark Checkbox
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (item.isMarked) SafetyOrange else Slate900.copy(alpha = 0.75f))
                            .clickable { onToggleMark() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isMarked) Icons.Default.Check else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Mark",
                            tint = if (item.isMarked) Slate950 else Slate200,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Top Right: Camera Model Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Slate900.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.cameraModel.take(14),
                        style = MaterialTheme.typography.labelSmall,
                        color = TechCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Bottom Left: GPS Badge & Export status
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.latitude != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HighVisGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = HighVisGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("GPS", color = HighVisGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (item.isExported) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("EXPORTED", color = WarningAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Technical Details Box
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.referenceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.latitude != null && item.longitude != null) {
                    Text(
                        text = "${ExifHelper.toDecimal(item.latitude, true)}, ${ExifHelper.toDecimal(item.longitude, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = item.dateTimeOriginal,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.shutterSpeed ?: ""} ${item.aperture ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TechCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
