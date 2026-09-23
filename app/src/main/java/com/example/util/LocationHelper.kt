package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener {
            if (continuation.isActive) continuation.resume(null)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        return@withContext try {
            val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).awaitTask()
            location ?: fusedClient.lastLocation.awaitTask()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resultAddress = ""
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        resultAddress = formatAddress(addresses[0])
                    }
                }
                for (i in 0..10) {
                    if (resultAddress.isNotEmpty()) return@withContext resultAddress
                    kotlinx.coroutines.delay(50)
                }
                if (resultAddress.isNotEmpty()) return@withContext resultAddress
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(latitude, longitude, 1)
                if (!list.isNullOrEmpty()) {
                    return@withContext formatAddress(list[0])
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return@withContext "Field Site (${String.format(Locale.US, "%.3f, %.3f", latitude, longitude)})"
    }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        address.subLocality?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.subAdminArea?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        return if (parts.isNotEmpty()) {
            parts.distinct().take(3).joinToString(", ")
        } else {
            address.getAddressLine(0) ?: "Field Site Location"
        }
    }
}
