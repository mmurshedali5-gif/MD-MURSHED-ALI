package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_images")
data class CapturedImageItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileUri: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val sourceType: String = "CANON_SX70", // "CANON_SX70", "NIKON_P950", "MOBILE_CAM", "IMPORTED"
    val cameraMake: String = "Canon",
    val cameraModel: String = "PowerShot SX70 HS",
    val latitude: Double? = 22.60547,
    val longitude: Double? = 88.55771,
    val altitudeMeters: Double? = 14.5,
    val dateTimeOriginal: String = "2026-09-23 06:02 PM",
    val shutterSpeed: String? = "1/500s",
    val aperture: String? = "f/4.0",
    val iso: String? = "ISO 200",
    val focalLength: String? = "24.0mm",
    val referenceNumber: String = "TLM-ER2-2026/09",
    val fieldNotes: String = "Technical field inspection - Line clear",
    val inspectorName: String = "MMALI",
    val locationAddress: String = "North 24 Parganas, WB",
    val isMarked: Boolean = false,
    val isExported: Boolean = false,
    val watermarkedUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "asset_code_presets")
data class AssetCodePreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val description: String = ""
)

enum class WatermarkPosition {
    BOTTOM_FULL_BANNER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_FULL_BANNER
}

enum class WatermarkFontSize {
    COMPACT,
    STANDARD,
    LARGE,
    EXTRA_LARGE
}

enum class WatermarkTextColor(val argb: Int, val label: String) {
    WHITE(0xFFFFFFFF.toInt(), "White"),
    SAFETY_AMBER(0xFFFFD600.toInt(), "Amber"),
    CYAN(0xFF00E5FF.toInt(), "Cyan"),
    HIGH_VIS_GREEN(0xFF00E676.toInt(), "Lime")
}

data class WatermarkConfig(
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_FULL_BANNER,
    val fontSize: WatermarkFontSize = WatermarkFontSize.STANDARD,
    val textColor: WatermarkTextColor = WatermarkTextColor.WHITE,
    val bannerOpacity: Float = 0.75f,
    val showDmsCoordinates: Boolean = true,
    val showDecimalCoordinates: Boolean = true,
    val showAddress: Boolean = true,
    val showCameraSpecs: Boolean = true,
    val showInspector: Boolean = true,
    val showCompass: Boolean = true
)
