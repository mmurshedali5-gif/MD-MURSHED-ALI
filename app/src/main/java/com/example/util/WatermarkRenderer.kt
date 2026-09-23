package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.CapturedImageItem
import com.example.data.model.WatermarkConfig
import com.example.data.model.WatermarkFontSize
import com.example.data.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object WatermarkRenderer {

    suspend fun decodeSampledBitmap(
        context: Context,
        uriString: String,
        reqWidth: Int = 2048,
        reqHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return@withContext null)
                if (!file.exists()) return@withContext null
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                return@withContext BitmapFactory.decodeFile(file.absolutePath, options)
            } else {
                var stream: InputStream? = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                stream?.close()

                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                stream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(stream, null, options)
                stream?.close()
                return@withContext bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Burns the high-legibility technical watermark overlay onto the bitmap
     */
    fun renderWatermark(
        sourceBitmap: Bitmap,
        imageItem: CapturedImageItem,
        config: WatermarkConfig
    ): Bitmap {
        // Create a mutable copy to draw upon
        val outputBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val width = outputBitmap.width.toFloat()
        val height = outputBitmap.height.toFloat()

        // Base scale factor calculated dynamically from image dimensions
        val baseDimension = max(width, height)
        val scaleMultiplier = when (config.fontSize) {
            WatermarkFontSize.COMPACT -> 0.8f
            WatermarkFontSize.STANDARD -> 1.0f
            WatermarkFontSize.LARGE -> 1.25f
            WatermarkFontSize.EXTRA_LARGE -> 1.55f
        }

        val padding = (baseDimension * 0.02f).coerceAtLeast(16f)
        val titleTextSize = (baseDimension * 0.022f * scaleMultiplier).coerceAtLeast(24f)
        val bodyTextSize = (baseDimension * 0.016f * scaleMultiplier).coerceAtLeast(18f)
        val smallTextSize = (baseDimension * 0.013f * scaleMultiplier).coerceAtLeast(14f)

        // Coordinates formatting
        val latDms = imageItem.latitude?.let { ExifHelper.toDms(it, isLatitude = true) } ?: "N/A"
        val latDec = imageItem.latitude?.let { ExifHelper.toDecimal(it, isLatitude = true) } ?: ""
        val lngDms = imageItem.longitude?.let { ExifHelper.toDms(it, isLatitude = false) } ?: "N/A"
        val lngDec = imageItem.longitude?.let { ExifHelper.toDecimal(it, isLatitude = false) } ?: ""

        val latLine = if (config.showDmsCoordinates && config.showDecimalCoordinates && latDec.isNotEmpty()) {
            "LAT  : $latDms ($latDec)"
        } else if (config.showDmsCoordinates) {
            "LAT  : $latDms"
        } else {
            "LAT  : $latDec"
        }

        val lngLine = if (config.showDmsCoordinates && config.showDecimalCoordinates && lngDec.isNotEmpty()) {
            "LONG : $lngDms ($lngDec)"
        } else if (config.showDmsCoordinates) {
            "LONG : $lngDms"
        } else {
            "LONG : $lngDec"
        }

        val altitudeText = imageItem.altitudeMeters?.let { " | ALT: ${String.format("%.1f", it)}m" } ?: ""
        val dateTimeLine = "DATE : ${imageItem.dateTimeOriginal}$altitudeText"
        val refLine = "REF  : ${imageItem.referenceNumber}"
        val notesLine = "NOTE : ${imageItem.fieldNotes}"
        val inspectorLine = if (config.showInspector && imageItem.inspectorName.isNotBlank()) {
            "INSPECTED BY: ${imageItem.inspectorName}"
        } else null
        val addressLine = if (config.showAddress && imageItem.locationAddress.isNotBlank()) {
            "LOC  : ${imageItem.locationAddress}"
        } else null
        val cameraLine = if (config.showCameraSpecs) {
            "CAM  : ${imageItem.cameraMake} ${imageItem.cameraModel} [${imageItem.shutterSpeed ?: ""} ${imageItem.aperture ?: ""} ${imageItem.iso ?: ""}]"
        } else null

        // Collect lines to render
        val lines = mutableListOf<Pair<String, Boolean>>() // Text to isHeadline
        lines.add(refLine to true)
        lines.add(latLine to false)
        lines.add(lngLine to false)
        lines.add(dateTimeLine to false)
        if (addressLine != null) lines.add(addressLine to false)
        if (notesLine.isNotBlank()) lines.add(notesLine to false)
        if (inspectorLine != null) lines.add(inspectorLine to false)
        if (cameraLine != null) lines.add(cameraLine to false)

        val lineSpacing = bodyTextSize * 0.45f
        val bannerHeight = (lines.size * (bodyTextSize + lineSpacing)) + (padding * 2.2f)

        val bannerRect: RectF = when (config.position) {
            WatermarkPosition.BOTTOM_FULL_BANNER -> {
                RectF(0f, height - bannerHeight, width, height)
            }
            WatermarkPosition.TOP_FULL_BANNER -> {
                RectF(0f, 0f, width, bannerHeight)
            }
            WatermarkPosition.BOTTOM_LEFT -> {
                val boxWidth = (width * 0.72f).coerceAtLeast(width * 0.5f)
                RectF(padding, height - bannerHeight - padding, padding + boxWidth, height - padding)
            }
            WatermarkPosition.BOTTOM_RIGHT -> {
                val boxWidth = (width * 0.72f).coerceAtLeast(width * 0.5f)
                RectF(width - boxWidth - padding, height - bannerHeight - padding, width - padding, height - padding)
            }
        }

        // Draw Dark Contrast Banner
        val bannerPaint = Paint().apply {
            val alphaInt = (config.bannerOpacity * 255).toInt().coerceIn(40, 255)
            color = Color.argb(alphaInt, 15, 23, 42) // Slate 900
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cornerRadius = if (config.position == WatermarkPosition.BOTTOM_FULL_BANNER || config.position == WatermarkPosition.TOP_FULL_BANNER) {
            0f
        } else {
            (baseDimension * 0.012f).coerceAtLeast(12f)
        }
        canvas.drawRoundRect(bannerRect, cornerRadius, cornerRadius, bannerPaint)

        // Accent Brand Border / Top Accent Bar
        val accentPaint = Paint().apply {
            color = Color.parseColor("#FF6D00") // Safety Orange
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val barThickness = (baseDimension * 0.0035f).coerceAtLeast(4f)
        if (config.position == WatermarkPosition.BOTTOM_FULL_BANNER) {
            canvas.drawRect(0f, bannerRect.top, width, bannerRect.top + barThickness, accentPaint)
        } else if (config.position == WatermarkPosition.TOP_FULL_BANNER) {
            canvas.drawRect(0f, bannerRect.bottom - barThickness, width, bannerRect.bottom, accentPaint)
        } else {
            val borderStroke = Paint().apply {
                color = Color.parseColor("#38BDF8") // Cyan
                strokeWidth = barThickness
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRoundRect(bannerRect, cornerRadius, cornerRadius, borderStroke)
        }

        // Header Tag: "GPS CAM BY MMALI" Badge
        val badgePaint = Paint().apply {
            color = Color.parseColor("#06B6D4") // Cyan
            textSize = smallTextSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        val badgeY = bannerRect.top + padding + smallTextSize
        canvas.drawText("⚡ GPS CAM BY MMALI  |  FIELD INSPECTION VERIFIED", bannerRect.left + padding, badgeY, badgePaint)

        // Draw Lines of Telemetry Text
        val textPaint = Paint().apply {
            color = config.textColor.argb
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            isSubpixelText = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val refPaint = Paint().apply {
            color = Color.parseColor("#FFD600") // High-vis yellow for Ref ID
            textSize = titleTextSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }

        var currentY = badgeY + lineSpacing + (titleTextSize * 0.9f)
        for ((lineText, isHeadline) in lines) {
            if (isHeadline) {
                canvas.drawText(lineText, bannerRect.left + padding, currentY, refPaint)
                currentY += titleTextSize + lineSpacing
            } else {
                canvas.drawText(lineText, bannerRect.left + padding, currentY, textPaint)
                currentY += bodyTextSize + lineSpacing
            }
        }

        return outputBitmap
    }

    /**
     * Module D: Saves watermarked image to DCIM/Watermarked_Exports/
     * and triggers lossless EXIF re-injection.
     */
    suspend fun saveWatermarkedImageLossless(
        context: Context,
        watermarkedBitmap: Bitmap,
        imageItem: CapturedImageItem,
        originalExifMap: Map<String, String>
    ): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Prepare target file in dedicated folder: /DCIM/Watermarked_Exports/
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val exportDir = File(dcimDir, "Watermarked_Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val cleanRef = imageItem.referenceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val exportFileName = "GPS_CAM_${cleanRef}_${timestamp}.jpg"
            val targetFile = File(exportDir, exportFileName)

            // 2. Compress at maximum quality (100) to prevent compression artifacts
            val fos = FileOutputStream(targetFile)
            watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()

            // 3. Lossless EXIF Re-injection (CRITICAL)
            ExifHelper.reinjectExif(
                targetJpgFile = targetFile,
                originalAttributes = originalExifMap,
                latitude = imageItem.latitude,
                longitude = imageItem.longitude,
                altitudeMeters = imageItem.altitudeMeters,
                referenceNumber = imageItem.referenceNumber,
                fieldNotes = imageItem.fieldNotes,
                inspectorName = imageItem.inspectorName
            )

            // 4. Index in MediaStore so it appears in device photo gallery immediately
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, exportFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Watermarked_Exports")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                // Ignore MediaStore insert exception if already saved directly to DCIM
            }

            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
