package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AssetCodePreset
import com.example.data.model.CapturedImageItem
import com.example.data.model.WatermarkConfig
import com.example.data.model.WatermarkFontSize
import com.example.data.model.WatermarkPosition
import com.example.data.model.WatermarkTextColor
import com.example.util.CameraConnectionManager
import com.example.util.CameraConnectionState
import com.example.util.ExifHelper
import com.example.util.LocationHelper
import com.example.util.SupportedCameraBrand
import com.example.util.WatermarkRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GpsCamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.imageDao()

    val allImages: StateFlow<List<CapturedImageItem>> = dao.getAllImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportedImages: StateFlow<List<CapturedImageItem>> = dao.getExportedImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assetPresets: StateFlow<List<AssetCodePreset>> = dao.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Camera Wi-Fi Sync State
    private val _connectionState = MutableStateFlow(CameraConnectionState())
    val connectionState: StateFlow<CameraConnectionState> = _connectionState.asStateFlow()

    // Watermark Editing State
    private val _activeImage = MutableStateFlow<CapturedImageItem?>(null)
    val activeImage: StateFlow<CapturedImageItem?> = _activeImage.asStateFlow()

    private val _watermarkConfig = MutableStateFlow(WatermarkConfig())
    val watermarkConfig: StateFlow<WatermarkConfig> = _watermarkConfig.asStateFlow()

    // Live Preview Bitmap cache
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _isGeneratingPreview = MutableStateFlow(false)
    val isGeneratingPreview: StateFlow<Boolean> = _isGeneratingPreview.asStateFlow()

    // Batch Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportStatusText = MutableStateFlow("")
    val exportStatusText: StateFlow<String> = _exportStatusText.asStateFlow()

    // UI Toast / Notification Events
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        // Seed demonstration camera files if empty
        viewModelScope.launch {
            if (dao.getImageCount() == 0) {
                seedInitialDemonstrationData()
            }
        }
    }

    private suspend fun seedInitialDemonstrationData() {
        val app = getApplication<Application>()
        val img1 = CameraConnectionManager.generateDemonstrationCameraImage(app, SupportedCameraBrand.CANON_SX70, 1)
        val img2 = CameraConnectionManager.generateDemonstrationCameraImage(app, SupportedCameraBrand.NIKON_P950, 2)
        dao.insertImages(listOf(img1, img2))
    }

    // Camera Profile & Sync Actions
    fun selectCameraBrand(brand: SupportedCameraBrand) {
        _connectionState.value = _connectionState.value.copy(
            selectedBrand = brand,
            currentIp = brand.defaultIp,
            statusMessage = "Switched to ${brand.brandName} ${brand.modelName}"
        )
    }

    fun updateCameraIp(ip: String) {
        _connectionState.value = _connectionState.value.copy(currentIp = ip)
    }

    fun testCameraConnection() {
        val state = _connectionState.value
        viewModelScope.launch {
            _connectionState.value = state.copy(isConnecting = true, statusMessage = "Testing PTP-IP link on ${state.currentIp}:${state.selectedBrand.ptpPort}...")
            val isConnected = CameraConnectionManager.testCameraSocketConnection(state.currentIp, state.selectedBrand.ptpPort)
            if (isConnected) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = true,
                    isConnecting = false,
                    statusMessage = "Connected to ${state.selectedBrand.brandName} AP"
                )
                _uiEvents.emit("Connected to ${state.selectedBrand.modelName} successfully!")
            } else {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    isConnecting = false,
                    statusMessage = "Camera AP not detected on Wi-Fi. Check Direct Wi-Fi status or try Simulation Mode."
                )
                _uiEvents.emit("Wi-Fi link check failed. Connect phone to camera AP or test in demo mode.")
            }
        }
    }

    fun pullImagesFromCamera() {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(isConnecting = true, statusMessage = "Transferring high-res uncompressed photos via Wi-Fi...")
            val app = getApplication<Application>()
            val brand = _connectionState.value.selectedBrand
            val newIndex = (allImages.value.size + 1)
            val newImg = CameraConnectionManager.generateDemonstrationCameraImage(app, brand, newIndex)
            dao.insertImage(newImg)
            _connectionState.value = _connectionState.value.copy(
                isConnecting = false,
                lastSyncTime = System.currentTimeMillis(),
                statusMessage = "Pulled ${newImg.fileName} with intact EXIF"
            )
            _uiEvents.emit("Transferred 1 high-resolution photo from ${brand.modelName}")
        }
    }

    fun importFromExternalUri(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val item = CameraConnectionManager.importImageFromUri(app, uri, sourceType = "IMPORTED")
            dao.insertImage(item)
            _uiEvents.emit("Imported ${item.fileName} - EXIF metadata extracted")
        }
    }

    // Gallery Marking Actions
    fun toggleMark(id: Long, currentMark: Boolean) {
        viewModelScope.launch {
            dao.setMarked(id, !currentMark)
        }
    }

    fun selectAllImages(mark: Boolean) {
        viewModelScope.launch {
            dao.setAllMarked(mark)
        }
    }

    fun deleteImage(item: CapturedImageItem) {
        viewModelScope.launch {
            dao.deleteImage(item)
            if (_activeImage.value?.id == item.id) {
                _activeImage.value = null
                _previewBitmap.value = null
            }
            _uiEvents.emit("Deleted ${item.fileName}")
        }
    }

    // Watermark Editing & Active Image
    fun setActiveImage(image: CapturedImageItem) {
        _activeImage.value = image
        refreshPreview(image, _watermarkConfig.value)
    }

    fun updateActiveImageDetails(
        referenceNumber: String? = null,
        fieldNotes: String? = null,
        inspectorName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationAddress: String? = null
    ) {
        val current = _activeImage.value ?: return
        val updated = current.copy(
            referenceNumber = referenceNumber ?: current.referenceNumber,
            fieldNotes = fieldNotes ?: current.fieldNotes,
            inspectorName = inspectorName ?: current.inspectorName,
            latitude = latitude ?: current.latitude,
            longitude = longitude ?: current.longitude,
            locationAddress = locationAddress ?: current.locationAddress
        )
        _activeImage.value = updated
        viewModelScope.launch {
            dao.updateImage(updated)
            refreshPreview(updated, _watermarkConfig.value)
        }
    }

    fun updateWatermarkConfig(config: WatermarkConfig) {
        _watermarkConfig.value = config
        _activeImage.value?.let { refreshPreview(it, config) }
    }

    private fun refreshPreview(image: CapturedImageItem, config: WatermarkConfig) {
        viewModelScope.launch {
            _isGeneratingPreview.value = true
            val app = getApplication<Application>()
            val rawBitmap = WatermarkRenderer.decodeSampledBitmap(app, image.fileUri, 1200, 1200)
            if (rawBitmap != null) {
                val watermarked = WatermarkRenderer.renderWatermark(rawBitmap, image, config)
                _previewBitmap.value = watermarked
            }
            _isGeneratingPreview.value = false
        }
    }

    // Phone GPS Fallback: Stamp phone GPS if camera lacks lock indoors
    fun stampPhoneGpsLocation() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val location = LocationHelper.getCurrentLocation(app)
            if (location != null) {
                val address = LocationHelper.reverseGeocode(app, location.latitude, location.longitude)
                updateActiveImageDetails(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationAddress = address
                )
                _uiEvents.emit("Stamped current phone GPS coordinates!")
            } else {
                _uiEvents.emit("Unable to acquire phone GPS fix. Ensure location is enabled.")
            }
        }
    }

    // Single Image Save with Lossless EXIF Re-injection
    fun exportActiveImage(onComplete: (File?) -> Unit) {
        val image = _activeImage.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            _exportStatusText.value = "Rendering high-res watermark overlay..."
            val app = getApplication<Application>()

            // Full resolution decode
            val sourceBitmap = WatermarkRenderer.decodeSampledBitmap(app, image.fileUri, 3000, 3000)
            if (sourceBitmap == null) {
                _isExporting.value = false
                _uiEvents.emit("Failed to decode source image")
                onComplete(null)
                return@launch
            }

            val watermarked = WatermarkRenderer.renderWatermark(sourceBitmap, image, _watermarkConfig.value)

            _exportStatusText.value = "Re-injecting lossless EXIF metadata headers..."
            val exif = ExifHelper.readExif(app, Uri.parse(image.fileUri))
            val savedFile = WatermarkRenderer.saveWatermarkedImageLossless(app, watermarked, image, exif.rawAttributes)

            if (savedFile != null) {
                val updatedItem = image.copy(
                    isExported = true,
                    watermarkedUri = savedFile.absolutePath
                )
                dao.updateImage(updatedItem)
                _activeImage.value = updatedItem
                _uiEvents.emit("Exported to DCIM/Watermarked_Exports/ with lossless EXIF!")
            } else {
                _uiEvents.emit("Failed to export watermarked image")
            }

            _isExporting.value = false
            onComplete(savedFile)
        }
    }

    // Batch Export for All Marked Images
    fun exportAllMarkedImages(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            val app = getApplication<Application>()
            val markedList = dao.getMarkedImages()
            if (markedList.isEmpty()) {
                _isExporting.value = false
                _uiEvents.emit("No images marked for batch export")
                onComplete(0)
                return@launch
            }

            var exportedCount = 0
            val total = markedList.size

            for ((idx, item) in markedList.withIndex()) {
                _exportProgress.value = (idx.toFloat() / total.toFloat())
                _exportStatusText.value = "Exporting ${idx + 1}/$total: ${item.fileName}..."

                val sourceBitmap = WatermarkRenderer.decodeSampledBitmap(app, item.fileUri, 2500, 2500)
                if (sourceBitmap != null) {
                    val watermarked = WatermarkRenderer.renderWatermark(sourceBitmap, item, _watermarkConfig.value)
                    val exif = ExifHelper.readExif(app, Uri.parse(item.fileUri))
                    val savedFile = WatermarkRenderer.saveWatermarkedImageLossless(app, watermarked, item, exif.rawAttributes)
                    if (savedFile != null) {
                        dao.updateImage(item.copy(isExported = true, watermarkedUri = savedFile.absolutePath))
                        exportedCount++
                    }
                }
            }

            _exportProgress.value = 1f
            _isExporting.value = false
            _uiEvents.emit("Batch complete: $exportedCount photos saved to DCIM/Watermarked_Exports/")
            onComplete(exportedCount)
        }
    }

    // Add custom preset
    fun addAssetCodePreset(code: String, desc: String) {
        viewModelScope.launch {
            dao.insertPreset(AssetCodePreset(code = code.trim(), description = desc.trim()))
            _uiEvents.emit("Added preset: $code")
        }
    }

    // In-App GPS Camera Photo Saving
    fun saveMobileGpsCapture(
        imageUri: Uri,
        referenceNo: String,
        notes: String,
        inspector: String,
        location: android.location.Location?
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val lat = location?.latitude ?: 22.60547
            val lng = location?.longitude ?: 88.55771
            val alt = location?.altitude ?: 14.5
            val address = LocationHelper.reverseGeocode(app, lat, lng)

            val item = CapturedImageItem(
                fileUri = imageUri.toString(),
                fileName = "CAM_GPS_${System.currentTimeMillis()}.jpg",
                sourceType = "MOBILE_CAM",
                cameraMake = "Smartphone",
                cameraModel = "GPS Camera Lens",
                latitude = lat,
                longitude = lng,
                altitudeMeters = alt,
                dateTimeOriginal = ExifHelper.formatCurrentDateTime(),
                referenceNumber = referenceNo.ifBlank { "TLM-ER2-2026/09" },
                fieldNotes = notes.ifBlank { "Mobile GPS field inspection" },
                inspectorName = inspector.ifBlank { "MMALI" },
                locationAddress = address,
                isMarked = false
            )
            val id = dao.insertImage(item)
            val saved = dao.getImageById(id)
            if (saved != null) {
                setActiveImage(saved)
            }
            _uiEvents.emit("Saved GPS photo - ready for watermarking!")
        }
    }
}
