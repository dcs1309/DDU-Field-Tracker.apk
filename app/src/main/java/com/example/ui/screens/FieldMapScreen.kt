package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SurveyWithDetails
import com.example.ui.components.GoogleSatelliteMapView
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberMapController
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel
import java.util.Locale

data class VillageJump(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val zoom: Int,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldMapScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateToRecordDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allSurveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedRecord by remember { mutableStateOf<SurveyWithDetails?>(allSurveys.firstOrNull()) }

    // Map format state: HYBRID (default), SATELLITE (pure), ESRI, STREET
    var currentMapLayer by remember { mutableStateOf("HYBRID") }

    // Camera target for quick jumps / building zoom
    var targetLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var targetZoom by remember { mutableStateOf<Int?>(null) }

    // Search query within map
    var mapSearchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Direct Map Controller for instant, glitch-free button responses
    val mapController = rememberMapController()

    val filterOptions = listOf(
        "ALL" to "All Entities",
        "INSTITUTION" to "Institutions",
        "LOCAL_SHOP" to "Local Shops",
        "SUPPLIER" to "Suppliers",
        "FIELD_OBSERVATION" to "Observations"
    )

    val villageJumps = listOf(
        VillageJump("ALL", "All Block", 27.4320, 82.1850, 15, "📍"),
        VillageJump("RAMPUR", "Rampur Tola", 27.4312, 82.1892, 18, "🏫"),
        VillageJump("JURI", "Juri Hospital", 27.4280, 82.1950, 18, "🏥"),
        VillageJump("BAZAR", "Balrampur Bazar", 27.4350, 82.1820, 18, "🏪"),
        VillageJump("GRAZI", "Grazi Village", 27.4410, 82.1760, 18, "🌾"),
        VillageJump("PIPRA", "Pipra Tola", 27.4265, 82.1870, 18, "🧵")
    )

    val displayedSurveys = remember(allSurveys, selectedFilter, mapSearchQuery) {
        allSurveys.filter { item ->
            val matchesFilter = selectedFilter == "ALL" || item.survey.surveyType == selectedFilter
            val matchesSearch = mapSearchQuery.isBlank() ||
                item.survey.entityName.contains(mapSearchQuery, ignoreCase = true) ||
                item.survey.village.contains(mapSearchQuery, ignoreCase = true) ||
                item.survey.tola.contains(mapSearchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    // Function to launch external Google Maps App directly in Satellite Format (t=k)
    fun launchGoogleMapsAppSatellite(survey: SurveyWithDetails) {
        val lat = survey.survey.gpsLatitude
        val lng = survey.survey.gpsLongitude
        val label = Uri.encode("${survey.survey.entityName} (${survey.survey.village})")
        // Note: t=k forces Google Maps to launch in Satellite / Aerial format
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)&z=19&t=k")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Web browser fallback with satellite basemap query
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng&zoom=19&basemap=satellite")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        TextField(
                            value = mapSearchQuery,
                            onValueChange = { query ->
                                mapSearchQuery = query
                                if (query.isNotBlank()) {
                                    val match = allSurveys.find { item ->
                                        item.survey.entityName.contains(query, ignoreCase = true) ||
                                        item.survey.village.contains(query, ignoreCase = true) ||
                                        item.survey.tola.contains(query, ignoreCase = true)
                                    }
                                    if (match != null) {
                                        selectedRecord = match
                                        mapController.flyTo(match.survey.gpsLatitude, match.survey.gpsLongitude, 18)
                                        mapController.selectMarker(match.survey.dduId)
                                    }
                                }
                            },
                            placeholder = { Text("Search village, building, or shop...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("map_search_input")
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Field Map (Satellite View)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SATELLITE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${displayedSurveys.size} Village Buildings & GPS Points Captured",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) mapSearchQuery = ""
                        },
                        modifier = Modifier.testTag("map_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Control Bar: Satellite Layer Switcher & Village Jump Chips
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {

                        // Row 1: Map Format Selector (Satellite Hybrid, Pure Satellite, Esri, Street)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LAYER FORMAT:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MapLayerChip(
                                    selected = currentMapLayer == "HYBRID",
                                    label = "🛰️ Google Hybrid",
                                    onClick = {
                                        currentMapLayer = "HYBRID"
                                        mapController.setLayer("HYBRID")
                                    }
                                )
                                MapLayerChip(
                                    selected = currentMapLayer == "SATELLITE",
                                    label = "🏡 Pure Satellite",
                                    onClick = {
                                        currentMapLayer = "SATELLITE"
                                        mapController.setLayer("SATELLITE")
                                    }
                                )
                                MapLayerChip(
                                    selected = currentMapLayer == "ESRI",
                                    label = "🌍 Esri Earth",
                                    onClick = {
                                        currentMapLayer = "ESRI"
                                        mapController.setLayer("ESRI")
                                    }
                                )
                                MapLayerChip(
                                    selected = currentMapLayer == "STREET",
                                    label = "🗺️ Street",
                                    onClick = {
                                        currentMapLayer = "STREET"
                                        mapController.setLayer("STREET")
                                    }
                                )
                            }
                        }

                        // Row 2: Village Jump Chips (Direct Building-Level Zoom 18)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VILLAGE:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            villageJumps.forEach { jump ->
                                SuggestionChip(
                                    onClick = {
                                        targetLocation = Pair(jump.lat, jump.lng)
                                        targetZoom = jump.zoom
                                        mapController.flyTo(jump.lat, jump.lng, jump.zoom)
                                        // Pick first survey in this village if any
                                        val match = allSurveys.find {
                                            it.survey.village.contains(jump.name, ignoreCase = true) ||
                                            jump.id == "ALL"
                                        }
                                        if (match != null) {
                                            selectedRecord = match
                                            mapController.selectMarker(match.survey.dduId)
                                        }
                                    },
                                    label = {
                                        Text(
                                            "${jump.icon} ${jump.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Row 3: Entity Type Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filterOptions.forEach { (key, label) ->
                                val isSelected = selectedFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = key },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DduPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }

                // Interactive Satellite Map View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    GoogleSatelliteMapView(
                        surveys = displayedSurveys,
                        selectedSurvey = selectedRecord,
                        mapLayerType = currentMapLayer,
                        targetLocation = targetLocation,
                        targetZoom = targetZoom,
                        onSurveySelected = { selectedRecord = it },
                        mapController = mapController,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-Left Floating Info Pill (Confirming Satellite & Building Footprint mode)
                    Surface(
                        color = Color(0xDD0F172A),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 3.dp,
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Village Buildings & Settlements Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Floating Map Controls (Zoom In, Zoom Out, Building Zoom, Recenter)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Building View Shortcut (Zoom 19 for roof-level detail)
                        FloatingActionButton(
                            onClick = {
                                selectedRecord?.let {
                                    mapController.flyTo(it.survey.gpsLatitude, it.survey.gpsLongitude, 19)
                                    mapController.selectMarker(it.survey.dduId)
                                    Toast.makeText(context, "Zoomed to Building Level (Roofs & Compounds)", Toast.LENGTH_SHORT).show()
                                } ?: run {
                                    mapController.flyTo(27.4312, 82.1892, 19)
                                }
                            },
                            modifier = Modifier.size(40.dp).testTag("building_level_zoom_button"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = "Building Zoom", modifier = Modifier.size(20.dp))
                        }

                        // Zoom In (+)
                        FloatingActionButton(
                            onClick = {
                                mapController.zoomIn()
                            },
                            modifier = Modifier.size(40.dp).testTag("map_zoom_in_button"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                        }

                        // Zoom Out (-)
                        FloatingActionButton(
                            onClick = {
                                mapController.zoomOut()
                            },
                            modifier = Modifier.size(40.dp).testTag("map_zoom_out_button"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                        }

                        // Recenter Block View
                        FloatingActionButton(
                            onClick = {
                                mapController.recenter()
                            },
                            modifier = Modifier.size(40.dp).testTag("recenter_map_button"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit All", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Bottom Inspector Card for the Selected Village Building Marker
            AnimatedVisibility(
                visible = selectedRecord != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                selectedRecord?.let { record ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_marker_inspector_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {

                            // Header row: Enterprise icon, Name, Village, Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when (record.survey.surveyType) {
                                                    "INSTITUTION" -> Color(0xFFE0F2F1)
                                                    "LOCAL_SHOP" -> Color(0xFFFFE0B2)
                                                    "SUPPLIER" -> Color(0xFFE3F2FD)
                                                    else -> Color(0xFFF3E5F5)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (record.survey.surveyType) {
                                                "INSTITUTION" -> Icons.Default.AccountBalance
                                                "LOCAL_SHOP" -> Icons.Default.Storefront
                                                "SUPPLIER" -> Icons.Default.LocalShipping
                                                else -> Icons.Default.Visibility
                                            },
                                            contentDescription = null,
                                            tint = when (record.survey.surveyType) {
                                                "INSTITUTION" -> Color(0xFF00796B)
                                                "LOCAL_SHOP" -> Color(0xFFE65100)
                                                "SUPPLIER" -> Color(0xFF1565C0)
                                                else -> Color(0xFF7B1FA2)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = record.survey.entityName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "📍 ${record.survey.village} (${record.survey.tola}) • ${record.survey.entityType}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                StatusBadge(status = record.survey.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Building GPS Accuracy & Surveyor Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.4f° N, %.4f° E (±%.1fm Verified)",
                                            record.survey.gpsLatitude,
                                            record.survey.gpsLongitude,
                                            record.survey.gpsAccuracyMeters
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }

                                Text(
                                    text = "Surveyed: ${record.survey.dateString}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Product Demand Preview if present
                            if (record.products.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = Color(0xFF00796B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Demand: " + record.products.joinToString(", ") {
                                                "${it.productName} (${it.minQuantity.toInt()} ${it.unit} @ ₹${it.buyingPrice.toInt()})"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dual Action Buttons: Open in Google Maps App (Satellite) & View Full Record
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Google Maps App Satellite Launcher Button
                                OutlinedButton(
                                    onClick = { launchGoogleMapsAppSatellite(record) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("open_in_google_maps_satellite_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF0284C7)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Google Maps App 🛰️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                // Inspect Full Record Button
                                Button(
                                    onClick = { onNavigateToRecordDetail(record.survey.dduId) },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(42.dp)
                                        .testTag("inspect_record_details_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DduPrimary,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "Inspect Record",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapLayerChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .clickable { onClick() }
            .height(26.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
