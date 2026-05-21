package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.api.AIAnalysisResult
import com.example.ui.viewmodel.AIAnalysisState

@Composable
fun ScanResultDialog(
    aiState: AIAnalysisState,
    capturedBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean = false
) {
    val context = LocalContext.current

    if (aiState is AIAnalysisState.Idle) return

    Dialog(
        onDismissRequest = { if (aiState !is AIAnalysisState.Loading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                when (aiState) {
                    is AIAnalysisState.Idle -> {}
                    is AIAnalysisState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 5.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Querying Visual AI...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Running identification algorithms, transcribing features and preparing detailed explanations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    is AIAnalysisState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "⚠️",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analysis Failed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Close")
                            }
                        }
                    }

                    is AIAnalysisState.Success -> {
                        val result = aiState.result
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Header with captured image snapshot
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.4f)
                                    .background(Color.Black)
                            ) {
                                capturedBitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Captured snapshot",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Quick close button
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close overlay",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Details block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.6f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Visual Identifier tag Badge
                                    SuggestionChip(
                                        enabled = false,
                                        onClick = {},
                                        label = { Text(result.type.uppercase()) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )

                                    // Confidence score indication
                                    Text(
                                        text = "${result.confidence.toInt()}% Match",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.confidence > 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .background(
                                                color = (if (result.confidence > 75) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Main Classified Title
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons: Narrate, Share, Favorite
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { onSpeak(result.narration) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Voice speak narration",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = {
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_SUBJECT, "Vision AI Identification: ${result.title}")
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Identified ${result.type}: ${result.title}\n\nConfidence: ${result.confidence}%\n\nDescription: ${result.explanation}"
                                                )
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share Identification results")
                                            context.startActivity(shareIntent)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share analysis",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = onToggleFavorite,
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Toggle favorite state",
                                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Narration Quote Bubble
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Acoustic Narration Snapshot:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${result.narration}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Detailed analysis summary markdown body
                                Text(
                                    text = "Full AI Analysis:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = result.explanation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Bottom action shutter dismissed buttons
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Got It, Save & Done")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
