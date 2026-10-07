package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SurveyWithDetails
import com.example.data.places.PlaceResultSource
import com.example.data.places.PlaceSearchResult
import com.example.data.places.PlacesSearchService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GooglePlacesSearchBar(
    surveys: List<SurveyWithDetails>,
    onPlaceSelected: (PlaceSearchResult, Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    userLocation: Pair<Double, Double>? = null,
    onFilterClick: () -> Unit = {},
    isFilterActive: Boolean = false,
    onLayerClick: () -> Unit = {},
    isSdkActive: Boolean = true,
    isClusteringEnabled: Boolean = true,
    onToggleClustering: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val placesService = remember { PlacesSearchService(context) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    // Debounced Search Effect
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length < 2) {
            searchResults = emptyList()
            isDropdownOpen = false
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(320) // Smooth debouncing
        try {
            val results = placesService.searchPlaces(
                query = trimmed,
                surveys = surveys,
                proximityLat = userLocation?.first,
                proximityLng = userLocation?.second
            )
            searchResults = results
            isDropdownOpen = results.isNotEmpty()
        } catch (e: Exception) {
            searchResults = emptyList()
            isDropdownOpen = false
        } finally {
            isSearching = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search & Top Control Bar Card
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Places",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isBlank()) {
                                isDropdownOpen = false
                            }
                        },
                        placeholder = {
                            Text(
                                text = if (placesService.isGooglePlacesConfigured) {
                                    "Google Places, addresses, villages..."
                                } else {
                                    "Search places, addresses, field points..."
                                },
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("places_search_input")
                    )

                    if (isSearching) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                isDropdownOpen = false
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_clear_places_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Places API Engine Indicator Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (placesService.isGooglePlacesConfigured) {
                            Color(0xFF0284C7).copy(alpha = 0.12f)
                        } else {
                            Color(0xFF10B981).copy(alpha = 0.12f)
                        },
                        border = BorderStroke(
                            0.5.dp,
                            if (placesService.isGooglePlacesConfigured) Color(0xFF0284C7) else Color(0xFF10B981)
                        ),
                        modifier = Modifier
                            .clickable { onLayerClick() }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (placesService.isGooglePlacesConfigured) Icons.Default.Place else Icons.Default.Navigation,
                                contentDescription = null,
                                tint = if (placesService.isGooglePlacesConfigured) Color(0xFF0284C7) else Color(0xFF10B981),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (placesService.isGooglePlacesConfigured) "Google Places" else if (isSdkActive) "SDK" else "WEB",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (placesService.isGooglePlacesConfigured) Color(0xFF0284C7) else Color(0xFF10B981)
                            )
                        }
                    }

                    // Clustering Toggle Button
                    IconButton(
                        onClick = onToggleClustering,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_places_clustering_toggle")
                    ) {
                        Icon(
                            imageVector = if (isClusteringEnabled) Icons.Default.Hub else Icons.Default.Grain,
                            contentDescription = "Toggle Clustering",
                            tint = if (isClusteringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Filter Button
                    IconButton(
                        onClick = onFilterClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_places_filter")
                    ) {
                        BadgedBox(
                            badge = {
                                if (isFilterActive) {
                                    Badge { Text("1") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filters",
                                tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Inline small indicator strip showing live search status
                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // Suggestions Dropdown Card
        AnimatedVisibility(
            visible = isDropdownOpen && searchResults.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .heightIn(max = 280.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(searchResults, key = { it.id }) { result ->
                        PlaceSuggestionItem(
                            result = result,
                            onClick = {
                                focusManager.clearFocus()
                                isDropdownOpen = false
                                searchQuery = result.primaryText

                                coroutineScope.launch {
                                    val coords = placesService.resolvePlaceCoordinates(result)
                                    if (coords != null) {
                                        onPlaceSelected(result, coords.first, coords.second)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceSuggestionItem(
    result: PlaceSearchResult,
    onClick: () -> Unit
) {
    val (icon, iconColor, sourceLabel) = when (result.source) {
        PlaceResultSource.GOOGLE_PLACES -> Triple(Icons.Default.Place, Color(0xFF0284C7), "Google Places")
        PlaceResultSource.GEOCODER -> Triple(Icons.Default.LocationOn, Color(0xFF10B981), "Address")
        PlaceResultSource.LOCAL_FIELD_INTELLIGENCE -> Triple(Icons.Default.Agriculture, Color(0xFFD97706), "Field Record")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.primaryText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = iconColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = sourceLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            if (result.secondaryText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.secondaryText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
