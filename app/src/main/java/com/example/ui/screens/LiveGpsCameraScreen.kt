package com.example.ui.screens

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import com.example.util.LocationHelper
import com.example.viewmodel.GpsCamViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun LiveGpsCameraScreen(
    viewModel: GpsCamViewModel,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[android.Manifest.permission.CAMERA] ?: false
        hasLocationPermission = (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                (permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var referenceNumber by remember { mutableStateOf("TLM-ER2-2026/09") }
    var fieldNotes by remember { mutableStateOf("Base structure inspection") }
    var inspectorName by remember { mutableStateOf("MMALI") }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isCapturing by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Refresh live location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val loc = LocationHelper.getCurrentLocation(context)
            currentLocation = loc
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("LiveGpsCamera", "Camera bind failure", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission fallback
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera & GPS Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate200
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant permissions to shoot direct GPS-watermarked photos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange, contentColor = Slate950)
                ) {
                    Text("Grant Permissions")
                }
            }
        }

        // Live Telemetry HUD Overlay (Top)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ GPS CAM BY MMALI",
                        style = MaterialTheme.typography.labelSmall,
                        color = TechCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = HighVisGreen, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (currentLocation != null) "GPS LOCKED" else "ACQUIRING...",
                            color = if (currentLocation != null) HighVisGreen else WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                val lat = currentLocation?.latitude ?: 22.60547
                val lng = currentLocation?.longitude ?: 88.55771
                Text(
                    text = "LAT : ${ExifHelper.toDms(lat, true)} (${ExifHelper.toDecimal(lat, true)})",
                    color = Slate200,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "LONG: ${ExifHelper.toDms(lng, false)} (${ExifHelper.toDecimal(lng, false)})",
                    color = Slate200,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "REF : $referenceNumber",
                    color = WarningAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Controls Bar & Shutter Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Camera Lens
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Slate900.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Slate200)
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(SafetyOrange)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            val photoFile = File(
                                context.cacheDir,
                                "CAM_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        val uri = Uri.fromFile(photoFile)
                                        viewModel.saveMobileGpsCapture(
                                            imageUri = uri,
                                            referenceNo = referenceNumber,
                                            notes = fieldNotes,
                                            inspector = inspectorName,
                                            location = currentLocation
                                        )
                                        onNavigateToEditor()
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        isCapturing = false
                                        Log.e("LiveGpsCamera", "Capture failed", exc)
                                    }
                                }
                            )
                        }
                        .testTag("camera_shutter_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = Slate950, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Slate950, modifier = Modifier.size(36.dp))
                    }
                }

                // Refresh Location Fix
                IconButton(
                    onClick = {
                        val loc = runCatching {
                            kotlinx.coroutines.runBlocking { LocationHelper.getCurrentLocation(context) }
                        }.getOrNull()
                        if (loc != null) currentLocation = loc
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Slate900.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Refresh GPS", tint = HighVisGreen)
                }
            }
        }
    }
}
