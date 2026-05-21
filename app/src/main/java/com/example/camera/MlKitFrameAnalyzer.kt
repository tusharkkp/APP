package com.example.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class DetectedObjectInfo(
    val boundingBox: Rect,
    val label: String,
    val confidence: Float,
    val id: Int? = null
)

data class DetectedTextLine(
    val boundingBox: Rect,
    val text: String
)

data class DetectedBarcode(
    val boundingBox: Rect?,
    val displayValue: String,
    val rawValue: String,
    val format: String
)

data class LivingScanDetections(
    val objects: List<DetectedObjectInfo> = emptyList(),
    val textLines: List<DetectedTextLine> = emptyList(),
    val barcodes: List<DetectedBarcode> = emptyList(),
    // Image scale dimensions to map coordinate systems back onto the UI layout sizes
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

class MlKitFrameAnalyzer(
    private val scope: CoroutineScope,
    private val activeMode: String, // "All", "Object", "Text", "Barcode"
    private val onDetectionsUpdated: (LivingScanDetections) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)

    // On-device ML Kit clients
    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val barcodeScanner by lazy {
        BarcodeScanning.getClient()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        // Keep in mind landscape vs portrait dimensions
        val isPortrait = rotationDegrees == 90 || rotationDegrees == 270
        val imageWidth = if (isPortrait) imageProxy.height else imageProxy.width
        val imageHeight = if (isPortrait) imageProxy.width else imageProxy.height

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        when (activeMode) {
            "Object" -> {
                objectDetector.process(inputImage)
                    .addOnSuccessListener { detectedObjects ->
                        val list = detectedObjects.map { obj ->
                            val label = obj.labels.firstOrNull()?.text ?: "Unknown Object"
                            val confidence = obj.labels.firstOrNull()?.confidence ?: 0.0f
                            DetectedObjectInfo(obj.boundingBox, label, confidence, obj.trackingId)
                        }
                        onDetectionsUpdated(LivingScanDetections(objects = list, imageWidth = imageWidth, imageHeight = imageHeight))
                    }
                    .addOnFailureListener { e ->
                        Log.e("MlKitFrameAnalyzer", "Object detector failed: ${e.message}")
                    }
                    .addOnCompleteListener {
                        isProcessing.set(false)
                        imageProxy.close()
                    }
            }
            "Text" -> {
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val lines = mutableListOf<DetectedTextLine>()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                line.boundingBox?.let { rect ->
                                    lines.add(DetectedTextLine(rect, line.text))
                                }
                            }
                        }
                        onDetectionsUpdated(LivingScanDetections(textLines = lines, imageWidth = imageWidth, imageHeight = imageHeight))
                    }
                    .addOnFailureListener { e ->
                        Log.e("MlKitFrameAnalyzer", "Text OCR recognizer failed: ${e.message}")
                    }
                    .addOnCompleteListener {
                        isProcessing.set(false)
                        imageProxy.close()
                    }
            }
            "Barcode" -> {
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val list = barcodes.map { b ->
                            val formatStr = when (b.format) {
                                Barcode.FORMAT_QR_CODE -> "QR_CODE"
                                Barcode.FORMAT_EAN_13 -> "EAN_13"
                                Barcode.FORMAT_EAN_8 -> "EAN_8"
                                Barcode.FORMAT_UPC_A -> "UPC_A"
                                Barcode.FORMAT_CODE_128 -> "CODE_128"
                                else -> "BARCODE"
                            }
                            DetectedBarcode(
                                boundingBox = b.boundingBox,
                                displayValue = b.displayValue ?: b.rawValue ?: "Unknown",
                                rawValue = b.rawValue ?: "",
                                format = formatStr
                            )
                        }
                        onDetectionsUpdated(LivingScanDetections(barcodes = list, imageWidth = imageWidth, imageHeight = imageHeight))
                    }
                    .addOnFailureListener { e ->
                        Log.e("MlKitFrameAnalyzer", "Barcode scanner failed: ${e.message}")
                    }
                    .addOnCompleteListener {
                        isProcessing.set(false)
                        imageProxy.close()
                    }
            }
            else -> {
                // "All" mode: runs barcode & object scanner logically sequentially
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val barcodeList = barcodes.map { b ->
                            DetectedBarcode(
                                b.boundingBox,
                                b.displayValue ?: b.rawValue ?: "Unknown",
                                b.rawValue ?: "",
                                "QR/BARCODE"
                            )
                        }
                        
                        // Proceed to object classification in the same pipeline to compile both
                        objectDetector.process(inputImage)
                            .addOnSuccessListener { detectedObjects ->
                                val objectList = detectedObjects.map { obj ->
                                    val label = obj.labels.firstOrNull()?.text ?: "Object"
                                    val confidence = obj.labels.firstOrNull()?.confidence ?: 0.0f
                                    DetectedObjectInfo(obj.boundingBox, label, confidence, obj.trackingId)
                                }
                                onDetectionsUpdated(
                                    LivingScanDetections(
                                        objects = objectList,
                                        barcodes = barcodeList,
                                        imageWidth = imageWidth,
                                        imageHeight = imageHeight
                                    )
                                )
                            }
                            .addOnCompleteListener {
                                isProcessing.set(false)
                                imageProxy.close()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MlKitFrameAnalyzer", "Fallback pipeline errors: ${e.message}")
                        isProcessing.set(false)
                        imageProxy.close()
                    }
            }
        }
    }
}
