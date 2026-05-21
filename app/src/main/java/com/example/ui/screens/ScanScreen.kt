package com.example.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.LivingScanDetections
import com.example.ui.components.CameraOverlay
import com.example.ui.components.CameraPreview
import com.example.ui.viewmodel.AIAnalysisState
import com.example.ui.viewmodel.VisionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    viewModel: VisionViewModel,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val liveDetections by viewModel.cameraLiveDetections.collectAsStateWithLifecycle()
    val activeMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val voiceEnabled by viewModel.voiceEnabled.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val capturedBitmap by viewModel.capturedBitmap.collectAsStateWithLifecycle()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Scan modes available for real-time local classification
    val modes = listOf("All", "Object", "Text", "Barcode")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Sleek Material 3 "Vision AI" Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterCenterFocus,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                        contentDescription = null
                    )
                }
                Text(
                    text = "Vision AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { /* Settings context option if needed */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                    contentDescription = "Tune parameters"
                )
            }
        }

        // 2. Main Viewfinder container - meticulously framed by nested rounded card structure (as in Professional theme HTML)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF202124))
        ) {
            if (cameraPermissionState.status.isGranted) {
                // Camera preview stream
                CameraPreview(
                    activeMode = activeMode,
                    flashEnabled = flashEnabled,
                    scope = scope,
                    onDetectionsUpdated = { detections ->
                        viewModel.updateLiveDetections(detections)
                    },
                    onPreviewViewCreated = { view ->
                        previewViewRef = view
                    }
                )

                // Real-time local ML bounding boxes overlay
                CameraOverlay(
                    detections = liveDetections,
                    modifier = Modifier.fillMaxSize()
                )

                // Top tracking banner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Text(
                            text = "REAL-TIME DETECTION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Centered sophisticated Viewfinder Reticle box
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(32.dp)
                        )
                )

                // Modes carousel bar matching absolute HTML spacing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            modes.forEach { mode ->
                                val selected = activeMode == mode
                                InputChip(
                                    selected = selected,
                                    onClick = { viewModel.setScanMode(mode) },
                                    label = { Text(mode) },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = Color.Transparent,
                                        labelColor = Color.LightGray
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                // Shutter trigger and glassmorphic hardware controllers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight toggle button
                    IconButton(
                        onClick = { viewModel.toggleFlash() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("flash_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            tint = if (flashEnabled) Color.Yellow else Color.White,
                            contentDescription = "Toggle camera flash"
                        )
                    }

                    // MAIN CAMERA CAPTURING SHUTTER
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .background(Color.Transparent, CircleShape)
                            .testTag("ai_capture_button")
                            .clickable {
                                val bitmap = previewViewRef?.bitmap
                                if (bitmap != null) {
                                    val modeIntent = if (activeMode == "All" || activeMode == "Object") {
                                        "General"
                                    } else {
                                        activeMode
                                    }
                                    viewModel.performHeavyAIAnalysis(bitmap, modeIntent)
                                } else {
                                    Log.e("ScanScreen", "Failed to retrieve snapshot frame from camera view.")
                                    val width = 600
                                    val height = 800
                                    val mockBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    mockBitmap.eraseColor(android.graphics.Color.DKGRAY)
                                    viewModel.performHeavyAIAnalysis(mockBitmap, "General")
                                }
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, CircleShape)
                        )
                    }

                    // Acoustic Voice Synthesizer toggle button
                    IconButton(
                        onClick = { viewModel.toggleVoice() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("voice_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            tint = if (voiceEnabled) MaterialTheme.colorScheme.primaryContainer else Color.White,
                            contentDescription = "Toggle acoustic narrator"
                        )
                    }
                }
            } else {
                // Camera Permission requirement block
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            tint = Color.LightGray,
                            modifier = Modifier.size(40.dp),
                            contentDescription = null
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Vision AI needs camera access to scan text, recognize objects, plants, animals, landmarks and products in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null)
                            Text(text = "Grant Camera Access")
                        }
                    }
                }
            }
        }

        // Pop up overlay dialog
        val currentAiState = aiState
        ScanResultDialog(
            aiState = currentAiState,
            capturedBitmap = capturedBitmap,
            onDismiss = { viewModel.clearAIAnalysis() },
            onSpeak = { text -> viewModel.speakNarration(text) },
            onToggleFavorite = {
                if (currentAiState is AIAnalysisState.Success) {
                    val currentHistoryList = viewModel.scanHistory.value
                    val savedItem = currentHistoryList.firstOrNull { it.voiceText == currentAiState.result.narration }
                    if (savedItem != null) {
                        viewModel.toggleFavorite(savedItem)
                    }
                }
            },
            isFavorite = if (currentAiState is AIAnalysisState.Success) {
                val currentHistoryList = viewModel.scanHistory.value
                val savedItem = currentHistoryList.firstOrNull { it.voiceText == currentAiState.result.narration }
                savedItem?.isFavorite ?: false
            } else false
        )
    }
}
