package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,               // "Object", "Text", "Barcode", "Plant", "Animal", "Landmark", "Vehicle", "Product"
    val title: String,              // Identified label or short summary
    val details: String,            // Large field: OCR text, barcode value, or detailed AI description
    val confidence: Float = 0.0f,   // Detection confidence
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,   // Saved absolute image path on device, if any
    val extraJson: String? = null,  // Any extra metadata, formatted as JSON
    val voiceText: String? = null   // The generated TTS-friendly narration summary
)
