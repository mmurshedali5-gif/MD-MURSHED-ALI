package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.CapturedImageItem
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.HighVisGreen
import com.example.ui.theme.SafetyOrange
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
import java.io.File

@Composable
fun ExportHistoryScreen(
    viewModel: GpsCamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportedImages by viewModel.exportedImages.collectAsState()

    var selectedForDetails by remember { mutableStateOf<CapturedImageItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Slate900,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WATERMARKED EXPORTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = TechCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "/DCIM/Watermarked_Exports/ • ${exportedImages.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighVisGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = HighVisGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXIF INTACT", color = HighVisGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (exportedImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Slate800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = TechCyan, modifier = Modifier.size(36.dp))
                    }
                    Text("No Exported Photos Yet", style = MaterialTheme.typography.titleMedium, color = Slate200)
                    Text(
                        text = "Mark images in the media gallery or shoot with GPS CAM, then tap 'Burn & Export' to save here with lossless EXIF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exportedImages, key = { it.id }) { item ->
                    ExportedItemCard(
                        item = item,
                        onClick = { selectedForDetails = item },
                        onShare = {
                            val path = item.watermarkedUri ?: return@ExportedItemCard
                            val file = File(path)
                            if (file.exists()) {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Watermarked Inspection Photo"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportedItemCard(
    item: CapturedImageItem,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("exported_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800)
            ) {
                AsyncImage(
                    model = item.watermarkedUri ?: item.fileUri,
                    contentDescription = item.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.referenceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${item.cameraMake} ${item.cameraModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TechCyan,
                    fontSize = 11.sp
                )
                Text(
                    text = "LAT : ${item.latitude?.let { ExifHelper.toDecimal(it, true) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate200,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "LONG: ${item.longitude?.let { ExifHelper.toDecimal(it, false) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate200,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Slate400)
            }
        }
    }
}
