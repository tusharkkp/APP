package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ScanItem
import com.example.ui.viewmodel.VisionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList by viewModel.scanHistory.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Favorites", "Plant", "Animal", "Landmark", "Object", "Text", "Barcode"

    var selectedItemForDetail by remember { mutableStateOf<ScanItem?>(null) }

    val filterOptions = listOf("All", "Favorites", "Plant", "Animal", "Landmark", "Object", "Text", "Barcode")

    // Filtered result set computation
    val filteredHistory = remember(historyList, searchQuery, selectedFilter) {
        historyList.filter { item ->
            // Check selected filter chip
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Favorites" -> item.isFavorite
                else -> item.type.equals(selectedFilter, ignoreCase = true)
            }

            // Check details or title search match
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.details.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History Scans",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Search text box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_history_input"),
            placeholder = { Text("Search title, text, or explanations...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear query")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filters horizontal row
        ScrollableTabRow(
            selectedTabIndex = filterOptions.indexOf(selectedFilter).coerceAtLeast(0),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            edgePadding = 16.dp,
            divider = {},
            indicator = {}
        ) {
            filterOptions.forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (filteredHistory.isEmpty()) {
            // Friendly Empty layout illustration
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.ManageSearch else Icons.Default.History,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "No search results match" else "Your scan history is empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "Try adjusting your query words or resetting active category filters." else "Identified items from snapshots and visual search will appear logged here for offline review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            // Visual listing row layout
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredHistory,
                    key = { item -> item.id }
                ) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = { selectedItemForDetail = item },
                        onVoiceNarrate = { speech -> viewModel.speakNarration(speech) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onDelete = { viewModel.deleteScanItem(item) }
                    )
                }
            }
        }
    }

    // Modal popup to display historical items fully scrollable in detail
    selectedItemForDetail?.let { activeItem ->
        HistoryDetailDialog(
            item = activeItem,
            onDismiss = { selectedItemForDetail = null },
            onSpeak = { speech -> viewModel.speakNarration(speech) },
            onToggleFavorite = {
                viewModel.toggleFavorite(activeItem)
                // Keep the modal references up to date
                selectedItemForDetail = activeItem.copy(isFavorite = !activeItem.isFavorite)
            },
            onDelete = {
                viewModel.deleteScanItem(activeItem)
                selectedItemForDetail = null
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    item: ScanItem,
    onClick: () -> Unit,
    onVoiceNarrate: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    // Select category icon vectors
    val categoryIcon = when (item.type.lowercase()) {
        "plant" -> Icons.Default.Eco
        "animal" -> Icons.Default.Pets
        "landmark" -> Icons.Default.Place
        "barcode" -> Icons.Default.QrCodeScanner
        "text", "ocr" -> Icons.Default.TextFields
        else -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Snapshot thumbnail loading with local storage mapping
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null && File(item.imageUrl).exists()) {
                    AsyncImage(
                        model = File(item.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = categoryIcon,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                        contentDescription = null
                    )
                    Text(
                        text = item.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Quick micro actions list
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { item.voiceText?.let { onVoiceNarrate(it) } ?: onVoiceNarrate(item.title) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                        contentDescription = "Speak narration summary"
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                        contentDescription = "Toggle favorite status"
                    )
                }
            }
        }
    }
}

/**
 * Historical offline visual description inspector dialog. Displays absolute details from cached objects.
 */
@Composable
fun HistoryDetailDialog(
    item: ScanItem,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val categoryIcon = when (item.type.lowercase()) {
        "plant" -> Icons.Default.Eco
        "animal" -> Icons.Default.Pets
        "landmark" -> Icons.Default.Place
        "barcode" -> Icons.Default.QrCodeScanner
        "text", "ocr" -> Icons.Default.TextFields
        else -> Icons.Default.Category
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Visual capture snap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                            .background(Color.Black)
                    ) {
                        if (item.imageUrl != null && File(item.imageUrl).exists()) {
                            AsyncImage(
                                model = File(item.imageUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(64.dp),
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "On-Device Snapshot",
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                tint = Color.White,
                                contentDescription = "Close overlay"
                            )
                        }
                    }

                    // Content text block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f)
                            .padding(20.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                enabled = false,
                                onClick = {},
                                label = { Text(item.type.uppercase()) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )

                            // Confidence badge
                            Text(
                                text = if (item.confidence > 0) "${item.confidence.toInt()}% Match" else "Offline Scan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast Actions Toolbar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Voice synthesis
                            FilledTonalIconButton(
                                onClick = { item.voiceText?.let { onSpeak(it) } ?: onSpeak(item.title) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            // Share Results
                            FilledTonalIconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_SUBJECT, "Vision AI Scan Details: ${item.title}")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Explorer found ${item.type}: ${item.title}\n\nExplanations: ${item.details}"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share findings"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            // Favorites hearts
                            FilledTonalIconButton(
                                onClick = onToggleFavorite
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onTertiaryContainer,
                                    contentDescription = "Favorite toggle"
                                )
                            }

                            // Delete permanently
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = onDelete,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = "Delete item"
                                )
                            }
                        }

                        if (item.voiceText != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Acoustic Narration:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${item.voiceText}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Explanations & Facts:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Full scrollable custom descriptions text
                        Text(
                            text = item.details,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
