package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.VisionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val historyList = viewModel.scanHistory.collectAsStateWithLifecycle().value
    val categoryMetrics = viewModel.categoryMetrics.collectAsStateWithLifecycle().value

    val totalScans = historyList.size
    val favoritesCount = historyList.count { it.isFavorite }

    // Aggregate category calculations
    val categoryStats = remember(historyList) {
        val groups = historyList.groupBy { it.type.lowercase().replaceFirstChar { char -> char.uppercase() } }
        val finalMap = mutableMapOf<String, Int>()
        
        // Ensure standard categories exist
        listOf("Plant", "Animal", "Landmark", "Object", "Text", "Barcode").forEach { type ->
            finalMap[type] = groups[type]?.size ?: 0
        }
        
        // Add other categories if any
        groups.forEach { (type, list) ->
            if (type !in finalMap) {
                finalMap[type] = list.size
            }
        }
        
        finalMap.entries.sortedByDescending { it.value }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Numerical summaries (Two columns layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Key total scans Card
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Total Scans",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalScans",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Total favorites Card
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                tint = MaterialTheme.colorScheme.secondary,
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$favoritesCount",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Visual breakdown layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Scan Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (totalScans == 0) {
                        Text(
                            text = "No scanning metrics available yet. Take photo snaps to populate database metrics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    } else {
                        // Category progress meters
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            categoryStats.forEach { (categoryName, count) ->
                                val fraction = if (totalScans > 0) count.toFloat() / totalScans else 0f
                                
                                val categoryIcon = when (categoryName.lowercase()) {
                                    "plant" -> Icons.Default.Eco
                                    "animal" -> Icons.Default.Pets
                                    "landmark" -> Icons.Default.Place
                                    "barcode" -> Icons.Default.QrCodeScanner
                                    "text" -> Icons.Default.TextFields
                                    else -> Icons.Default.Category
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = categoryIcon,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                                contentDescription = null
                                            )
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "$count scans (${(fraction * 100).toInt()}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = fraction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Achievements Gamification panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null
                        )
                        Text(
                            text = "Explorer Badges",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Achievement item 1
                    AchievementItem(
                        title = "Apprentice Scanner",
                        description = "Take your first Visual Snapshot with Core AI",
                        isUnlocked = totalScans >= 1
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Achievement item 2
                    val uniqueCategoriesScanned = categoryStats.count { it.value > 0 }
                    AchievementItem(
                        title = "Polymath Scholar",
                        description = "Identify items across 3 distinct visual modes",
                        isUnlocked = uniqueCategoriesScanned >= 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Achievement item 3
                    AchievementItem(
                        title = "Visual Connoisseur",
                        description = "Uncover 5 favorite objects and log them in your database",
                        isUnlocked = favoritesCount >= 5
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementItem(
    title: String,
    description: String,
    isUnlocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                contentDescription = null
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
            )
        }
    }
}
