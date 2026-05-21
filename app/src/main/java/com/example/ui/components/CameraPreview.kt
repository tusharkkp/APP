package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.LivingScanDetections
import com.example.camera.MlKitFrameAnalyzer
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    activeMode: String,
    flashEnabled: Boolean,
    scope: CoroutineScope,
    onDetectionsUpdated: (LivingScanDetections) -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Notify parent about preview view creation (so shutter actions can extract bitmap)
    LaunchedEffect(previewView) {
        onPreviewViewCreated(previewView)
    }

    // Toggle flash on camera instance
    LaunchedEffect(flashEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    // Bind camera inputs and pipelines dynamically upon activeMode changes
    LaunchedEffect(activeMode) {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor,
                    MlKitFrameAnalyzer(scope, activeMode, onDetectionsUpdated)
                )
            }

        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Camera preview binding failed: ${e.message}", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Draws dynamic on-device detected targets overlays directly aligned with our Camera preview viewport.
 */
@Composable
fun CameraOverlay(
    modifier: Modifier = Modifier,
    detections: LivingScanDetections,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 3.dp.toPx() }
    val textPaint = remember {
        Paint().asFrameworkPaint().apply {
            color = Color.White.toArgb()
            textSize = with(density) { 14.sp.toPx() }
            isFakeBoldText = true
        }
    }
    val backgroundPaint = remember {
        Paint().asFrameworkPaint().apply {
            color = primaryColor.copy(alpha = 0.85f).toArgb()
            style = android.graphics.Paint.Style.FILL
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Only layout boxes if image aspect proportions are valid
        if (detections.imageWidth > 0 && detections.imageHeight > 0) {
            val scaleX = size.width / detections.imageWidth
            val scaleY = size.height / detections.imageHeight

            // 1. Draw Barcodes
            for (barcode in detections.barcodes) {
                val box = barcode.boundingBox ?: Rect(
                    (detections.imageWidth * 0.2).toInt(),
                    (detections.imageHeight * 0.4).toInt(),
                    (detections.imageWidth * 0.8).toInt(),
                    (detections.imageHeight * 0.6).toInt()
                )
                val left = box.left * scaleX
                val top = box.top * scaleY
                val right = box.right * scaleX
                val bottom = box.bottom * scaleY

                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = strokeWidthPx),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                drawContext.canvas.nativeCanvas.drawRoundRect(
                    left, top - 60f, left + textPaint.measureText(barcode.displayValue) + 30f, top,
                    12f, 12f, Paint().asFrameworkPaint().apply { color = secondaryColor.toArgb() }
                )
                drawContext.canvas.nativeCanvas.drawText(
                    barcode.displayValue,
                    left + 15f,
                    top - 18f,
                    textPaint
                )
            }

            // 2. Draw General classified Objects
            for (obj in detections.objects) {
                val left = obj.boundingBox.left * scaleX
                val top = obj.boundingBox.top * scaleY
                val right = obj.boundingBox.right * scaleX
                val bottom = obj.boundingBox.bottom * scaleY

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = strokeWidthPx),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                val labelText = if (obj.confidence > 0f) "${obj.label} (${(obj.confidence * 100).toInt()}%)" else obj.label
                drawContext.canvas.nativeCanvas.drawRoundRect(
                    left, top - 60f, left + textPaint.measureText(labelText) + 30f, top,
                    12f, 12f, backgroundPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    left + 15f,
                    top - 18f,
                    textPaint
                )
            }

            // 3. Draw OCR OCR detected lines
            for (line in detections.textLines) {
                val left = line.boundingBox.left * scaleX
                val top = line.boundingBox.top * scaleY
                val right = line.boundingBox.right * scaleX
                val bottom = line.boundingBox.bottom * scaleY

                drawRoundRect(
                    color = Color.Yellow,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = strokeWidthPx / 1.5f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
    }
}
