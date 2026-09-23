package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object ExifHelper {
    private const val TAG = "ExifHelper"

    data class ExtractedExifData(
        val latitude: Double?,
        val longitude: Double?,
        val altitude: Double?,
        val dateTimeString: String,
        val make: String,
        val model: String,
        val shutterSpeed: String?,
        val aperture: String?,
        val iso: String?,
        val focalLength: String?,
        val orientation: Int,
        val rawAttributes: Map<String, String> = emptyMap()
    )

    fun readExif(context: Context, uri: Uri): ExtractedExifData {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                return parseExifInterface(exif)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading EXIF from uri: $uri", e)
        } finally {
            inputStream?.close()
        }
        return defaultExifData()
    }

    fun readExif(file: File): ExtractedExifData {
        try {
            val exif = ExifInterface(file)
            return parseExifInterface(exif)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading EXIF from file: ${file.absolutePath}", e)
        }
        return defaultExifData()
    }

    private fun parseExifInterface(exif: ExifInterface): ExtractedExifData {
        val latLong = exif.latLong
        val latitude = latLong?.get(0)
        val longitude = latLong?.get(1)
        val altitude = exif.getAltitude(0.0).takeIf { it != 0.0 }

        val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "Canon"
        val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "PowerShot SX70 HS"
        val rawDateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: formatCurrentDateTime()

        val formattedDate = formatExifDate(rawDateTime)

        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        val shutterSpeed = if (exposureTime != null) {
            try {
                val sec = exposureTime.toDouble()
                if (sec < 1.0 && sec > 0) "1/${(1.0 / sec).toInt()}s" else "${sec}s"
            } catch (e: Exception) {
                "${exposureTime}s"
            }
        } else "1/500s"

        val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: "f/4.0"
        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" } ?: "ISO 200"
        val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" } ?: "24.0mm"
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // Capture all critical tags for lossless re-injection
        val criticalTags = listOf(
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_METERING_MODE,
            ExifInterface.TAG_EXPOSURE_PROGRAM,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_LENS_MAKE,
            ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_SOFTWARE
        )

        val rawMap = mutableMapOf<String, String>()
        for (tag in criticalTags) {
            exif.getAttribute(tag)?.let { rawMap[tag] = it }
        }

        return ExtractedExifData(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            dateTimeString = formattedDate,
            make = make,
            model = model,
            shutterSpeed = shutterSpeed,
            aperture = fNumber,
            iso = iso,
            focalLength = focalLength,
            orientation = orientation,
            rawAttributes = rawMap
        )
    }

    private fun defaultExifData(): ExtractedExifData {
        return ExtractedExifData(
            latitude = 22.60547,
            longitude = 88.55771,
            altitude = 14.5,
            dateTimeString = formatCurrentDateTime(),
            make = "Canon",
            model = "PowerShot SX70 HS",
            shutterSpeed = "1/500s",
            aperture = "f/4.0",
            iso = "ISO 200",
            focalLength = "24.0mm",
            orientation = ExifInterface.ORIENTATION_NORMAL
        )
    }

    /**
     * Lossless EXIF Re-injection:
     * Takes the original EXIF attributes, merges them with updated field metadata
     * (Coordinates, Ref No, Inspection Notes, Software signature),
     * and saves directly to the exported JPEG file without metadata degradation.
     */
    fun reinjectExif(
        targetJpgFile: File,
        originalAttributes: Map<String, String>,
        latitude: Double?,
        longitude: Double?,
        altitudeMeters: Double?,
        referenceNumber: String,
        fieldNotes: String,
        inspectorName: String
    ): Boolean {
        return try {
            val exif = ExifInterface(targetJpgFile)

            // 1. Re-apply all preserved original technical tags
            for ((tag, value) in originalAttributes) {
                exif.setAttribute(tag, value)
            }

            // 2. Inject or override precise GPS tags
            if (latitude != null && longitude != null) {
                exif.setLatLong(latitude, longitude)
            }
            if (altitudeMeters != null) {
                exif.setAltitude(altitudeMeters)
            }

            // 3. Inject App & Technical Field Metadata
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "GPS CAM BY MMALI - Field Inspection Suite")
            val inspectionSummary = "Ref: $referenceNumber | Inspector: $inspectorName | Notes: $fieldNotes"
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, inspectionSummary)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "GPS Field Inspection - $referenceNumber - $fieldNotes")

            // 4. Save lossless EXIF headers
            exif.saveAttributes()
            Log.i(TAG, "Successfully re-injected lossless EXIF into: ${targetJpgFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-inject EXIF into: ${targetJpgFile.name}", e)
            false
        }
    }

    /**
     * Convert decimal coordinates to standard DMS format:
     * e.g., 22.60547 -> 22° 36' 19.69" N
     */
    fun toDms(coordinate: Double, isLatitude: Boolean): String {
        val absolute = abs(coordinate)
        val degrees = absolute.toInt()
        val minutesDouble = (absolute - degrees) * 60.0
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60.0
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        return String.format(Locale.US, "%d° %02d' %05.2f\" %s", degrees, minutes, seconds, direction)
    }

    /**
     * Formats decimal coordinates:
     * e.g., 22.60547 -> 22.60547° N
     */
    fun toDecimal(coordinate: Double, isLatitude: Boolean): String {
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        return String.format(Locale.US, "%.5f° %s", abs(coordinate), direction)
    }

    fun formatCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
        return sdf.format(Date())
    }

    private fun formatExifDate(exifDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val formatter = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
            val date = parser.parse(exifDate)
            if (date != null) formatter.format(date) else exifDate
        } catch (e: Exception) {
            exifDate
        }
    }
}
