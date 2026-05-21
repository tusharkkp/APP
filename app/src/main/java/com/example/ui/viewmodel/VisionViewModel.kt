package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.AIAnalysisResult
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.CategoryCount
import com.example.data.ScanItem
import com.example.data.ScanRepository
import com.example.utils.TtsNarrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed interface AIAnalysisState {
    object Idle : AIAnalysisState
    object Loading : AIAnalysisState
    data class Success(val result: AIAnalysisResult) : AIAnalysisState
    data class Error(val message: String) : AIAnalysisState
}

class VisionViewModel(
    private val application: Application,
    private val repository: ScanRepository
) : AndroidViewModel(application) {

    private val TAG = "VisionViewModel"

    // Local Repository Streams
    val scanHistory: StateFlow<List<ScanItem>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteScans: StateFlow<List<ScanItem>> = repository.favoriteScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryMetrics: StateFlow<List<CategoryCount>> = repository.categoryCounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Camera State
    private val _cameraLiveDetections = MutableStateFlow(com.example.camera.LivingScanDetections())
    val cameraLiveDetections: StateFlow<com.example.camera.LivingScanDetections> = _cameraLiveDetections.asStateFlow()

    private val _selectedMode = MutableStateFlow("All") // "All", "Object", "Text", "Barcode"
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    // Narrator state
    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private var ttsNarrator: TtsNarrator? = null

    // Gemini AI Analysis visual state
    private val _aiState = MutableStateFlow<AIAnalysisState>(AIAnalysisState.Idle)
    val aiState: StateFlow<AIAnalysisState> = _aiState.asStateFlow()

    // Last captured screenshot preview inside details dialog
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    init {
        ttsNarrator = TtsNarrator(application)
    }

    fun updateLiveDetections(detections: com.example.camera.LivingScanDetections) {
        _cameraLiveDetections.value = detections
    }

    fun setScanMode(mode: String) {
        _selectedMode.value = mode
        _cameraLiveDetections.value = com.example.camera.LivingScanDetections() // Clear older detections on mode changes
    }

    fun toggleFlash() {
        _flashEnabled.value = !_flashEnabled.value
    }

    fun toggleVoice() {
        _voiceEnabled.value = !_voiceEnabled.value
        if (!_voiceEnabled.value) {
            ttsNarrator?.stop()
        }
    }

    fun speakNarration(text: String) {
        if (_voiceEnabled.value) {
            ttsNarrator?.speak(text)
        }
    }

    fun clearAIAnalysis() {
        _aiState.value = AIAnalysisState.Idle
        _capturedBitmap.value = null
        ttsNarrator?.stop()
    }

    /**
     * Executes the cloud multimodal API operation on the captured frame.
     * High-fidelity plant, animal, landmark classification occurs on-demand.
     */
    fun performHeavyAIAnalysis(bitmap: Bitmap, modeIntent: String) {
        _capturedBitmap.value = bitmap
        _aiState.value = AIAnalysisState.Loading
        ttsNarrator?.stop()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Execute Gemini visual analysis. Returns structured content
                val aiResult = GeminiClient.analyzeImage(bitmap, modeIntent)

                // Save image locally for high-fidelity offline records page
                val imagePath = saveBitmapToInternalStorage(bitmap)

                // Create DB entity
                val scanRecord = ScanItem(
                    type = aiResult.type,
                    title = aiResult.title,
                    details = aiResult.explanation,
                    confidence = aiResult.confidence,
                    isFavorite = false,
                    imageUrl = imagePath,
                    voiceText = aiResult.narration
                )

                // Save to local database
                val insertId = repository.insertScan(scanRecord)
                Log.d(TAG, "Successfully analyzed and persisted scan item into DB with id: $insertId")

                _aiState.value = AIAnalysisState.Success(aiResult)

                // Acoustic Speech Narration Output
                if (_voiceEnabled.value) {
                    ttsNarrator?.speak(aiResult.narration)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Multimodal query crashed: ${e.message}", e)
                _aiState.value = AIAnalysisState.Error(e.localizedMessage ?: "Unknown visual intelligence error")
            }
        }
    }

    fun toggleFavorite(item: ScanItem) {
        viewModelScope.launch {
            repository.updateScan(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteScanItem(item: ScanItem) {
        viewModelScope.launch {
            repository.deleteScan(item)
            // Delete accompanying local file if exists on disk
            item.imageUrl?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed deleting media snapshot from disk: ${e.message}")
                }
            }
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val directory = File(application.filesDir, "scan_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val filename = "snapshot_${System.currentTimeMillis()}.jpg"
            val file = File(directory, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Internal state io failure saving capture frame: ${e.message}")
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsNarrator?.shutdown()
        ttsNarrator = null
    }
}

class VisionViewModelFactory(
    private val application: Application,
    private val repository: ScanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VisionViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
