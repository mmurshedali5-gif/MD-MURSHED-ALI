package com.example

import com.example.util.ExifHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifHelperTest {

    @Test
    fun testCoordinatesConversionDms() {
        val lat = 22.60547
        val lng = 88.55771

        val dmsLat = ExifHelper.toDms(lat, isLatitude = true)
        val dmsLng = ExifHelper.toDms(lng, isLatitude = false)

        assertTrue(dmsLat.contains("22°"))
        assertTrue(dmsLat.contains("36'"))
        assertTrue(dmsLat.contains("N"))

        assertTrue(dmsLng.contains("88°"))
        assertTrue(dmsLng.contains("33'"))
        assertTrue(dmsLng.contains("E"))
    }

    @Test
    fun testDecimalCoordinatesFormatting() {
        val lat = 22.60547
        val formatted = ExifHelper.toDecimal(lat, isLatitude = true)
        assertEquals("22.60547° N", formatted)
    }
}
