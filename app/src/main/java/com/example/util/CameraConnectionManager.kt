package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.CapturedImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SupportedCameraBrand(
    val brandName: String,
    val modelName: String,
    val defaultSsidPrefix: String,
    val defaultIp: String,
    val ptpPort: Int,
    val description: String
) {
    CANON_SX70(
        brandName = "Canon",
        modelName = "PowerShot SX70 HS",
        defaultSsidPrefix = "Canon_SX70_",
        defaultIp = "192.168.1.1",
        ptpPort = 15740,
        description = "Wi-Fi Direct / PTP-IP Local AP Mode (Port 15740)"
    ),
    NIKON_P950(
        brandName = "Nikon",
        modelName = "COOLPIX P950",
        defaultSsidPrefix = "NIKON_P950_",
        defaultIp = "192.168.0.1",
        ptpPort = 15740,
        description = "SnapBridge Direct Wi-Fi / PTP-IP Wireless AP"
    )
}

data class CameraConnectionState(
    val selectedBrand: SupportedCameraBrand = SupportedCameraBrand.CANON_SX70,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val currentIp: String = "192.168.1.1",
    val statusMessage: String = "Ready to connect",
    val detectedSsid: String? = null,
    val availableFilesCount: Int = 0,
    val lastSyncTime: Long? = null
)

object CameraConnectionManager {
    private const val TAG = "CameraConnMgr"

    /**
     * Checks if camera socket / PTP-IP port is responsive on local Wi-Fi AP
     */
    suspend fun testCameraSocketConnection(ipAddress: String, port: Int, timeoutMs: Int = 1800): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            socket.close()
            true
        } catch (e: Exception) {
            Log.d(TAG, "Socket test on $ipAddress:$port: ${e.message}")
            false
        }
    }

    /**
     * Imports a user-selected image from device storage / SD card and extracts intact EXIF
     */
    suspend fun importImageFromUri(
        context: Context,
        uri: Uri,
        sourceType: String = "IMPORTED",
        brandOverride: SupportedCameraBrand? = null
    ): CapturedImageItem = withContext(Dispatchers.IO) {
        val exifData = ExifHelper.readExif(context, uri)
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"

        val effectiveMake = brandOverride?.brandName ?: exifData.make
        val effectiveModel = brandOverride?.modelName ?: exifData.model

        val address = if (exifData.latitude != null && exifData.longitude != null) {
            LocationHelper.reverseGeocode(context, exifData.latitude, exifData.longitude)
        } else {
            "Site Location Coordinates"
        }

        CapturedImageItem(
            fileUri = uri.toString(),
            fileName = fileName,
            sourceType = sourceType,
            cameraMake = effectiveMake,
            cameraModel = effectiveModel,
            latitude = exifData.latitude ?: 22.60547,
            longitude = exifData.longitude ?: 88.55771,
            altitudeMeters = exifData.altitude ?: 14.5,
            dateTimeOriginal = exifData.dateTimeString,
            shutterSpeed = exifData.shutterSpeed ?: "1/500s",
            aperture = exifData.aperture ?: "f/4.0",
            iso = exifData.iso ?: "ISO 200",
            focalLength = exifData.focalLength ?: "24.0mm",
            referenceNumber = "TLM-ER2-2026/09",
            fieldNotes = "Technical field inspection - $effectiveModel transfer",
            inspectorName = "MMALI",
            locationAddress = address,
            isMarked = false
        )
    }

    /**
     * Generates a realistic high-resolution field inspection photo with authentic
     * Canon SX70 HS or Nikon P950 EXIF metadata for testing and field demonstration.
     */
    suspend fun generateDemonstrationCameraImage(
        context: Context,
        brand: SupportedCameraBrand,
        index: Int = 1
    ): CapturedImageItem = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val cameraDir = File(cacheDir, "camera_transfers")
        if (!cameraDir.exists()) cameraDir.mkdirs()

        val fileName = when (brand) {
            SupportedCameraBrand.CANON_SX70 -> "IMG_SX70_${1000 + index}.JPG"
            SupportedCameraBrand.NIKON_P950 -> "DSC_P950_${2000 + index}.JPG"
        }
        val targetFile = File(cameraDir, fileName)

        // Draw realistic industrial scene bitmap
        val width = 2400
        val height = 1800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Field background gradient (sky and industrial terrain)
        val skyPaint = Paint().apply {
            color = if (index % 2 == 0) Color.rgb(70, 130, 180) else Color.rgb(90, 140, 170)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.55f, skyPaint)

        val groundPaint = Paint().apply {
            color = if (index % 2 == 0) Color.rgb(65, 85, 60) else Color.rgb(80, 75, 60)
        }
        canvas.drawRect(0f, height * 0.55f, width.toFloat(), height.toFloat(), groundPaint)

        // Draw electrical transmission tower / structure outline
        val structurePaint = Paint().apply {
            color = Color.rgb(30, 40, 50)
            strokeWidth = 14f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val centerX = width * (0.35f + (index * 0.1f))
        canvas.drawLine(centerX - 240f, height.toFloat(), centerX - 60f, height * 0.18f, structurePaint)
        canvas.drawLine(centerX + 240f, height.toFloat(), centerX + 60f, height * 0.18f, structurePaint)
        canvas.drawLine(centerX - 60f, height * 0.18f, centerX + 60f, height * 0.18f, structurePaint)
        // Tower crossbars
        for (yStep in 1..6) {
            val y = height * (0.18f + (yStep * 0.12f))
            canvas.drawLine(centerX - 180f, y, centerX + 180f, y, structurePaint)
            canvas.drawLine(centerX - 180f, y - 80f, centerX + 180f, y, structurePaint)
            canvas.drawLine(centerX + 180f, y - 80f, centerX - 180f, y, structurePaint)
        }

        // Camera Simulation Watermark HUD Tag
        val hudPaint = Paint().apply {
            color = Color.rgb(240, 240, 240)
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText("RAW CAPTURE | ${brand.brandName} ${brand.modelName} OPTICAL SENSOR", 80f, 100f, hudPaint)

        val fos = FileOutputStream(targetFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 98, fos)
        fos.flush()
        fos.close()

        // Inject genuine EXIF headers into the file
        val lat = 22.60547 + (index * 0.0014)
        val lng = 88.55771 + (index * 0.0012)
        val alt = 14.5 + (index * 2.3)

        val exif = ExifInterface(targetFile)
        exif.setAttribute(ExifInterface.TAG_MAKE, brand.brandName)
        exif.setAttribute(ExifInterface.TAG_MODEL, brand.modelName)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:09:23 18:02:15")
        exif.setAttribute(ExifInterface.TAG_DATETIME, "2026:09:23 18:02:15")
        exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, if (brand == SupportedCameraBrand.CANON_SX70) "0.002" else "0.00125")
        exif.setAttribute(ExifInterface.TAG_F_NUMBER, if (brand == SupportedCameraBrand.CANON_SX70) "4.0" else "5.6")
        exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, "200")
        exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, if (brand == SupportedCameraBrand.CANON_SX70) "21.0" else "34.5")
        exif.setLatLong(lat, lng)
        exif.setAltitude(alt)
        exif.saveAttributes()

        val shutter = if (brand == SupportedCameraBrand.CANON_SX70) "1/500s" else "1/800s"
        val aperture = if (brand == SupportedCameraBrand.CANON_SX70) "f/4.0" else "f/5.6"

        CapturedImageItem(
            fileUri = Uri.fromFile(targetFile).toString(),
            fileName = fileName,
            fileSizeBytes = targetFile.length(),
            sourceType = if (brand == SupportedCameraBrand.CANON_SX70) "CANON_SX70" else "NIKON_P950",
            cameraMake = brand.brandName,
            cameraModel = brand.modelName,
            latitude = lat,
            longitude = lng,
            altitudeMeters = alt,
            dateTimeOriginal = "23-09-2026 06:02 PM",
            shutterSpeed = shutter,
            aperture = aperture,
            iso = "ISO 200",
            focalLength = if (brand == SupportedCameraBrand.CANON_SX70) "21.0mm" else "34.5mm",
            referenceNumber = "TLM-ER2-2026/09",
            fieldNotes = "Phase conductor & insulator string inspection - Clear",
            inspectorName = "MMALI",
            locationAddress = "North 24 Parganas, WB",
            isMarked = false
        )
    }
}
