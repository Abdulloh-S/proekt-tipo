package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AttractionEntity
import com.example.data.model.AttractionCategory
import com.example.ui.components.RatingBadge
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldCrown

@Composable
fun AttractionsScreen(
    attractions: List<AttractionEntity>,
    onSelectAttraction: (AttractionEntity) -> Unit,
    onPlayAudioGuide: (AttractionEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onShowOnMap: (AttractionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AttractionCategory.ALL) }
    var detailAttraction by remember { mutableStateOf<AttractionEntity?>(null) }

    val filteredAttractions = remember(attractions, searchQuery, selectedCategory) {
        attractions.filter { attr ->
            val matchQuery = searchQuery.isBlank() ||
                    attr.nameRu.contains(searchQuery, ignoreCase = true) ||
                    attr.nameTj.contains(searchQuery, ignoreCase = true) ||
                    attr.descriptionRu.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == AttractionCategory.ALL || attr.category == selectedCategory.code
            matchQuery && matchCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("attractions_screen")
    ) {
        // Search TextField
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск достопримечательностей Душанбе...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AttractionCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.labelRu, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Attractions List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredAttractions) { attr ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailAttraction = attr }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        // Image banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dushanbe_hero),
                                contentDescription = attr.nameRu,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            RatingBadge(
                                rating = attr.rating,
                                reviewCount = attr.reviewCount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            )

                            IconButton(
                                onClick = { onToggleFavorite(attr.id, !attr.isFavorite) },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = if (attr.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Избранное",
                                    tint = if (attr.isFavorite) Color.Red else Color.White
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = attr.nameRu,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = attr.nameTj,
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald600
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = attr.descriptionRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = attr.workingHours,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onPlayAudioGuide(attr) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Аудиогид (${attr.audioDurationSec / 60} мин)", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onShowOnMap(attr) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("На карте", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    detailAttraction?.let { attr ->
        AlertDialog(
            onDismissRequest = { detailAttraction = null },
            title = {
                Text(
                    text = attr.nameRu,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Тоҷикӣ: ${attr.nameTj}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Emerald600
                    )
                    Text(
                        text = "English: ${attr.nameEn}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Text(
                        text = attr.descriptionRu,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Адрес: ${attr.address}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Координаты: ${attr.latitude}, ${attr.longitude}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPlayAudioGuide(attr)
                        detailAttraction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Слушать аудиогид")
                }
            },
            dismissButton = {
                TextButton(onClick = { detailAttraction = null }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
