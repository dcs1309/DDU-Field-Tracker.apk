package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.SurveyWithDetails
import com.example.data.places.PlaceSearchResult
import com.example.ui.components.GoogleMapsSdkController
import com.example.ui.components.GoogleMapsSdkView
import com.example.ui.components.GooglePlacesSearchBar
import com.example.ui.components.GoogleSatelliteMapView
import com.example.ui.components.SearchedPlacePin
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberGoogleMapsSdkController
import com.example.ui.components.rememberMapController
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.launch
import java.util.Locale

data class VillageJump(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val zoom: Float,
    val icon: String
)

data class LayerOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val isSdk: Boolean,
    val sdkMapType: MapType = MapType.HYBRID
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldMapScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateToRecordDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val allSurveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()

    // Check whether a real Google Maps API key has been configured in Secrets
    val hasRealMapsApiKey = remember {
        val key = BuildConfig.MAPS_API_KEY
        key.isNotBlank() &&
        !key.equals("MY_MAPS_API_KEY", ignoreCase = true) &&
        !key.startsWith("MY_") &&
        key.length >= 20
    }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedRecord by remember { mutableStateOf<SurveyWithDetails?>(null) }

    // Map Engine mode: If user has a real key, default to Native SDK. Otherwise, default to the zero-config Web Satellite engine to avoid a blank gray screen!
    var useNativeSdk by remember { mutableStateOf(hasRealMapsApiKey) }
    var sdkMapType by remember { mutableStateOf(MapType.HYBRID) }
    var webMapLayer by remember { mutableStateOf("HYBRID") }
    var showLayerPicker by remember { mutableStateOf(false) }
    var showApiKeyInfoBanner by remember { mutableStateOf(!hasRealMapsApiKey) }
    var isClusteringEnabled by remember { mutableStateOf(true) }

    // List view bottom sheet
    var showListSheet by remember { mutableStateOf(false) }

    // Filter sheet
    var showFilterSheet by remember { mutableStateOf(false) }

    // Camera target for Web map fallback
    var targetLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var targetZoom by remember { mutableStateOf<Int?>(null) }

    // Search query within map
    var mapSearchQuery by remember { mutableStateOf("") }

    // Controllers for both engines
    val sdkController = rememberGoogleMapsSdkController()
    val webMapController = rememberMapController()

    // Location provider & state
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isLocating by remember { mutableStateOf(false) }
    var userCurrentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var searchedPlaceResult by remember { mutableStateOf<PlaceSearchResult?>(null) }
    var searchedPlaceCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Function to acquire location and center map
    fun acquireAndCenterLocation() {
        isLocating = true
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(listener: OnTokenCanceledListener) = CancellationTokenSource().token
                    override fun isCancellationRequested() = false
                }
            ).addOnSuccessListener { location ->
                isLocating = false
                if (location != null) {
                    userCurrentLocation = Pair(location.latitude, location.longitude)
                    Toast.makeText(context, "Centered on current location (±${location.accuracy.toInt()}m)", Toast.LENGTH_SHORT).show()
                    if (useNativeSdk) {
                        coroutineScope.launch {
                            sdkController.animateTo(location.latitude, location.longitude, 18f)
                        }
                    } else {
                        webMapController.flyTo(location.latitude, location.longitude, 18)
                    }
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            userCurrentLocation = Pair(lastLoc.latitude, lastLoc.longitude)
                            Toast.makeText(context, "Centered on last known position", Toast.LENGTH_SHORT).show()
                            if (useNativeSdk) {
                                coroutineScope.launch {
                                    sdkController.animateTo(lastLoc.latitude, lastLoc.longitude, 18f)
                                }
                            } else {
                                webMapController.flyTo(lastLoc.latitude, lastLoc.longitude, 18)
                            }
                        } else {
                            // Friendly fallback to field operations hub if no GPS fix yet (e.g. fresh emulator)
                            Toast.makeText(context, "GPS fix pending: centered on Balrampur field area", Toast.LENGTH_SHORT).show()
                            if (useNativeSdk) {
                                coroutineScope.launch {
                                    sdkController.animateTo(27.4320, 82.1850, 16f)
                                }
                            } else {
                                webMapController.flyTo(27.4320, 82.1850, 16)
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                isLocating = false
                Toast.makeText(context, "Location error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            isLocating = false
            Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for runtime location request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            Toast.makeText(context, "Location permission granted. Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
            acquireAndCenterLocation()
        } else {
            hasLocationPermission = false
            Toast.makeText(
                context,
                "Location permission is required to center the map on your position",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Handles the "My Location" button click
    fun onMyLocationClicked() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            acquireAndCenterLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val filterOptions = listOf(
        "ALL" to "All Types",
        "INSTITUTION" to "Institutions",
        "LOCAL_SHOP" to "Local Shops",
        "SUPPLIER" to "Suppliers",
        "FIELD_OBSERVATION" to "Observations"
    )

    val villageJumps = listOf(
        VillageJump("ALL", "All Villages", 27.4320, 82.1850, 15f, "📍"),
        VillageJump("RAMPUR", "Rampur Tola", 27.4312, 82.1892, 18f, "🏫"),
        VillageJump("JURI", "Juri Hospital", 27.4280, 82.1950, 18f, "🏥"),
        VillageJump("BAZAR", "Balrampur Bazar", 27.4350, 82.1820, 18f, "🏪"),
        VillageJump("GRAZI", "Grazi Village", 27.4410, 82.1760, 18f, "🌾"),
        VillageJump("PIPRA", "Pipra Tola", 27.4265, 82.1870, 18f, "🧵")
    )

    val layerOptions = listOf(
        LayerOption("SDK_HYBRID", "Google Maps: Satellite Hybrid", "Native 3D buildings, vector roads & high-res satellite", "🛰️", isSdk = true, sdkMapType = MapType.HYBRID),
        LayerOption("SDK_SATELLITE", "Google Maps: Pure Satellite", "High-altitude aerial photography without street labels", "🏡", isSdk = true, sdkMapType = MapType.SATELLITE),
        LayerOption("SDK_NORMAL", "Google Maps: Standard Map", "Clean Google vector map with building blocks & roads", "🗺️", isSdk = true, sdkMapType = MapType.NORMAL),
        LayerOption("SDK_TERRAIN", "Google Maps: Terrain View", "Topographic elevation contours & vegetation", "⛰️", isSdk = true, sdkMapType = MapType.TERRAIN),
        LayerOption("WEB_VOYAGER", "CartoDB Voyager (Web)", "High-contrast clean navigation map (No key required)", "🏙️", isSdk = false),
        LayerOption("WEB_OSM", "OpenStreetMap (Web)", "Detailed community-driven rural map layer", "🌐", isSdk = false),
        LayerOption("WEB_ESRI", "Esri World Imagery (Web)", "Esri Earth satellite basemap", "🌍", isSdk = false)
    )

    val displayedSurveys = remember(allSurveys, selectedFilter, mapSearchQuery) {
        allSurveys.filter { item ->
            val matchesFilter = selectedFilter == "ALL" || item.survey.surveyType == selectedFilter
            val matchesSearch = mapSearchQuery.isBlank() ||
                item.survey.entityName.contains(mapSearchQuery, ignoreCase = true) ||
                item.survey.village.contains(mapSearchQuery, ignoreCase = true) ||
                item.survey.tola.contains(mapSearchQuery, ignoreCase = true) ||
                item.products.any { it.productName.contains(mapSearchQuery, ignoreCase = true) }
            matchesFilter && matchesSearch
        }
    }

    // Launch external Google Maps Navigation / Satellite view safely
    fun launchGoogleMapsDirections(survey: SurveyWithDetails) {
        val lat = survey.survey.gpsLatitude
        val lng = survey.survey.gpsLongitude
        val label = Uri.encode("${survey.survey.entityName} (${survey.survey.village})")
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)&z=19&t=k")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng&zoom=19")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (ex: Exception) {
                Toast.makeText(context, "GPS: $lat, $lng copied to clipboard", Toast.LENGTH_SHORT).show()
                clipboardManager.setText(AnnotatedString("$lat, $lng"))
            }
        }
    }

    // Call contact directly
    fun callContact(phoneNumber: String) {
        if (phoneNumber.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot dial phone number", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No contact number recorded", Toast.LENGTH_SHORT).show()
        }
    }

    // Back handler: dismiss sheet, card, or search before exiting
    BackHandler(enabled = selectedRecord != null || showListSheet || showFilterSheet || mapSearchQuery.isNotBlank() || searchedPlaceResult != null) {
        if (showListSheet) showListSheet = false
        else if (showFilterSheet) showFilterSheet = false
        else if (selectedRecord != null) selectedRecord = null
        else if (searchedPlaceResult != null) {
            searchedPlaceResult = null
            searchedPlaceCoords = null
        }
        else if (mapSearchQuery.isNotBlank()) mapSearchQuery = ""
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Map View (Native Google Maps SDK or Web Satellite Engine)
        if (useNativeSdk) {
            GoogleMapsSdkView(
                surveys = displayedSurveys,
                selectedSurvey = selectedRecord,
                mapType = sdkMapType,
                onSurveySelected = { survey ->
                    selectedRecord = survey
                    searchedPlaceResult = null
                },
                controller = sdkController,
                isMyLocationEnabled = hasLocationPermission,
                isClusteringEnabled = isClusteringEnabled,
                searchedPlacePin = searchedPlaceCoords?.let {
                    SearchedPlacePin(
                        title = searchedPlaceResult?.primaryText ?: "Searched Place",
                        snippet = searchedPlaceResult?.secondaryText?.ifBlank { searchedPlaceResult?.fullAddress ?: "" } ?: "",
                        position = LatLng(it.first, it.second)
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            GoogleSatelliteMapView(
                surveys = displayedSurveys,
                selectedSurvey = selectedRecord,
                mapLayerType = webMapLayer,
                targetLocation = searchedPlaceCoords ?: targetLocation,
                targetZoom = targetZoom ?: if (searchedPlaceCoords != null) 18 else null,
                onSurveySelected = { survey ->
                    selectedRecord = survey
                    searchedPlaceResult = null
                },
                mapController = webMapController,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Floating Header: Google Places Search Bar & Quick Village Jumps
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Google Places Search Bar
            GooglePlacesSearchBar(
                surveys = allSurveys,
                userLocation = userCurrentLocation,
                onPlaceSelected = { result, lat, lng ->
                    searchedPlaceResult = result
                    searchedPlaceCoords = Pair(lat, lng)

                    // If a survey was matched directly, select it
                    if (result.matchedSurveyDduId != null) {
                        val matched = allSurveys.find { it.survey.dduId == result.matchedSurveyDduId }
                        if (matched != null) {
                            selectedRecord = matched
                        }
                    }

                    if (useNativeSdk) {
                        coroutineScope.launch {
                            sdkController.animateTo(lat, lng, 18f)
                        }
                    } else {
                        targetLocation = Pair(lat, lng)
                        targetZoom = 18
                        webMapController.flyTo(lat, lng, 18)
                    }
                },
                onFilterClick = { showFilterSheet = true },
                isFilterActive = selectedFilter != "ALL",
                onLayerClick = { showLayerPicker = true },
                isSdkActive = useNativeSdk,
                isClusteringEnabled = isClusteringEnabled,
                onToggleClustering = {
                    isClusteringEnabled = !isClusteringEnabled
                    Toast.makeText(
                        context,
                        if (isClusteringEnabled) "Marker clustering enabled" else "Marker clustering disabled (showing individual pins)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Informative API Key Status Banner (if key not yet configured)
            AnimatedVisibility(visible = showApiKeyInfoBanner && !hasRealMapsApiKey) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Running Free High-Res Satellite",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure MAPS_API_KEY in AI Studio Secrets to unlock SDK 3D tilt.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { useNativeSdk = !useNativeSdk },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (useNativeSdk) "Use Web" else "Try SDK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { showApiKeyInfoBanner = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Village Jump Pills (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                villageJumps.forEach { jump ->
                    val isAll = jump.id == "ALL"
                    val count = if (isAll) {
                        allSurveys.size
                    } else {
                        allSurveys.count { it.survey.village.contains(jump.name.take(5), ignoreCase = true) }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 2.dp,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clickable {
                                if (isAll) {
                                    if (useNativeSdk) {
                                        coroutineScope.launch {
                                            sdkController.fitBounds(allSurveys)
                                        }
                                    } else {
                                        webMapController.fitAll()
                                    }
                                } else {
                                    if (useNativeSdk) {
                                        coroutineScope.launch {
                                            sdkController.animateTo(jump.lat, jump.lng, jump.zoom)
                                        }
                                    } else {
                                        webMapController.flyTo(jump.lat, jump.lng, jump.zoom.toInt())
                                    }
                                    val match = allSurveys.find {
                                        it.survey.village.contains(jump.name.take(5), ignoreCase = true)
                                    }
                                    if (match != null) {
                                        selectedRecord = match
                                    }
                                }
                            }
                            .height(30.dp)
                            .testTag("village_chip_${jump.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(jump.icon, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                jump.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Floating Action Controls Stack (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Layer Switcher FAB
            FloatingActionButton(
                onClick = { showLayerPicker = true },
                modifier = Modifier.size(42.dp).testTag("map_layers_button"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Map Layers", modifier = Modifier.size(20.dp))
            }

            // My Location FAB (Requests location permission & centers map on user GPS coordinates)
            FloatingActionButton(
                onClick = { onMyLocationClicked() },
                modifier = Modifier.size(42.dp).testTag("my_location_button"),
                containerColor = if (hasLocationPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (hasLocationPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3D Building Rooftop Tilt (Zoom 19 with 45° tilt)
            FloatingActionButton(
                onClick = {
                    val lat = selectedRecord?.survey?.gpsLatitude ?: 27.4312
                    val lng = selectedRecord?.survey?.gpsLongitude ?: 82.1892
                    if (useNativeSdk) {
                        coroutineScope.launch {
                            sdkController.tiltBuildingView(lat, lng)
                            Toast.makeText(context, "3D Building View (Roofs & Footprints)", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        webMapController.flyTo(lat, lng, 19)
                        Toast.makeText(context, "Zoomed to Building Level (19x)", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(42.dp).testTag("building_level_zoom_button"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Apartment, contentDescription = "Building Zoom", modifier = Modifier.size(20.dp))
            }

            // Zoom In (+)
            FloatingActionButton(
                onClick = {
                    if (useNativeSdk) {
                        coroutineScope.launch { sdkController.zoomIn() }
                    } else {
                        webMapController.zoomIn()
                    }
                },
                modifier = Modifier.size(42.dp).testTag("map_zoom_in_button"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // Zoom Out (-)
            FloatingActionButton(
                onClick = {
                    if (useNativeSdk) {
                        coroutineScope.launch { sdkController.zoomOut() }
                    } else {
                        webMapController.zoomOut()
                    }
                },
                modifier = Modifier.size(42.dp).testTag("map_zoom_out_button"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            // Recenter / Fit All Markers
            FloatingActionButton(
                onClick = {
                    if (useNativeSdk) {
                        coroutineScope.launch {
                            sdkController.fitBounds(displayedSurveys)
                        }
                    } else {
                        webMapController.fitAll()
                    }
                    Toast.makeText(context, "Fitting All Locations", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(42.dp).testTag("recenter_map_button"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit All", modifier = Modifier.size(20.dp))
            }

            // Browse List Sheet Button
            FloatingActionButton(
                onClick = { showListSheet = true },
                modifier = Modifier.size(42.dp).testTag("map_browse_list_button"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Browse List", modifier = Modifier.size(20.dp))
            }
        }

        // 4. Bottom Inspector Card for Selected Location
        AnimatedVisibility(
            visible = selectedRecord != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                        // Header: Icon, Name, Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
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
                                        modifier = Modifier.size(22.dp)
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
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = record.survey.status)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { selectedRecord = null },
                                    modifier = Modifier.size(32.dp).testTag("close_inspector_card_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Inspector",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // GPS & Contact Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("${record.survey.gpsLatitude}, ${record.survey.gpsLongitude}"))
                                        Toast.makeText(context, "Coordinates copied", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.4f°N, %.4f°E (±%.1fm)",
                                        record.survey.gpsLatitude,
                                        record.survey.gpsLongitude,
                                        record.survey.gpsAccuracyMeters
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy GPS",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            if (record.survey.contactNumber.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { callContact(record.survey.contactNumber) }
                                        .padding(vertical = 2.dp, horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call Contact",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = record.survey.contactNumber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Product Demand preview
                        if (record.products.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Demand: " + record.products.joinToString(", ") {
                                            "${it.productName} (${it.minQuantity.toInt()} ${it.unit} @ ₹${it.buyingPrice.toInt()})"
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Directions, 3D Building Zoom, Inspect Record
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { launchGoogleMapsDirections(record) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("open_in_google_maps_satellite_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onNavigateToRecordDetail(record.survey.dduId) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(40.dp)
                                    .testTag("inspect_record_details_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Inspect Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4b. Bottom Card for Searched Google Place / Address
        AnimatedVisibility(
            visible = searchedPlaceResult != null && selectedRecord == null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            searchedPlaceResult?.let { place ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("searched_place_inspector_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE0F2FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = place.primaryText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = place.secondaryText.ifBlank { place.fullAddress },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (place.source == com.example.data.places.PlaceResultSource.GOOGLE_PLACES) "Google Place" else "Address",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    searchedPlaceResult = null
                                    searchedPlaceCoords = null
                                },
                                modifier = Modifier.size(32.dp).testTag("close_searched_place_card")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // GPS Coordinates row
                        searchedPlaceCoords?.let { (lat, lng) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(String.format(Locale.US, "%.5f, %.5f", lat, lng)))
                                            Toast.makeText(context, "GPS copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Navigate in Google Maps, Zoom in
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val label = Uri.encode(place.primaryText)
                                        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)&z=18")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("directions_searched_place_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Directions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (useNativeSdk) {
                                            coroutineScope.launch {
                                                sdkController.animateTo(lat, lng, 19f)
                                            }
                                        } else {
                                            webMapController.flyTo(lat, lng, 19)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("zoom_searched_place_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zoom In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Layer Picker Bottom Sheet / Dialog
        if (showLayerPicker) {
            ModalBottomSheet(
                onDismissRequest = { showLayerPicker = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Map Layer & Engine",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Toggle Google Maps SDK native hardware-accelerated 3D view or web basemap",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key Status Card inside layer sheet
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasRealMapsApiKey) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (hasRealMapsApiKey) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasRealMapsApiKey) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (hasRealMapsApiKey) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (hasRealMapsApiKey) "Google Maps API Key: Verified ✅" else "Maps API: Running on Free High-Res Basemaps",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasRealMapsApiKey)
                                        "Native SDK 3D building perspectives & vector tiles active."
                                    else
                                        "Add MAPS_API_KEY in Secrets panel to unlock native 3D tilt.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Marker Clustering Switch
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Marker Clustering", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Group close points to prevent overcrowding", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isClusteringEnabled,
                                onCheckedChange = { isClusteringEnabled = it },
                                modifier = Modifier.testTag("clustering_switch")
                            )
                        }
                    }

                    layerOptions.forEach { opt ->
                        val isSelected = if (opt.isSdk) {
                            useNativeSdk && sdkMapType == opt.sdkMapType
                        } else {
                            !useNativeSdk && (
                                (opt.id == "WEB_VOYAGER" && webMapLayer == "VOYAGER") ||
                                (opt.id == "WEB_OSM" && webMapLayer == "STREET") ||
                                (opt.id == "WEB_ESRI" && webMapLayer == "ESRI")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (opt.isSdk) {
                                        useNativeSdk = true
                                        sdkMapType = opt.sdkMapType
                                    } else {
                                        useNativeSdk = false
                                        webMapLayer = when (opt.id) {
                                            "WEB_VOYAGER" -> "VOYAGER"
                                            "WEB_OSM" -> "STREET"
                                            "WEB_ESRI" -> "ESRI"
                                            else -> "HYBRID"
                                        }
                                        webMapController.setLayer(webMapLayer)
                                    }
                                    showLayerPicker = false
                                }
                                .testTag("select_layer_${opt.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(opt.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = opt.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (opt.isSdk) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "SDK",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF0284C7),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = opt.subtitle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 6. Filter Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Filter Field Entities",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    filterOptions.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedFilter = key
                                    showFilterSheet = false
                                }
                                .testTag("filter_option_$key")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                val count = if (key == "ALL") allSurveys.size else allSurveys.count { it.survey.surveyType == key }
                                Text(
                                    text = "$count locations",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 7. Browse All Locations Bottom Sheet
        if (showListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showListSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Locations (${displayedSurveys.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showListSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedSurveys, key = { it.survey.dduId }) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRecord = item
                                        if (useNativeSdk) {
                                            coroutineScope.launch {
                                                sdkController.animateTo(item.survey.gpsLatitude, item.survey.gpsLongitude, 18f)
                                            }
                                        } else {
                                            webMapController.flyTo(item.survey.gpsLatitude, item.survey.gpsLongitude, 18)
                                            webMapController.selectMarker(item.survey.dduId, false)
                                        }
                                        showListSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (item.survey.surveyType) {
                                            "INSTITUTION" -> "🏛️"
                                            "LOCAL_SHOP" -> "🏪"
                                            "SUPPLIER" -> "📦"
                                            else -> "🔍"
                                        },
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.survey.entityName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.survey.village} (${item.survey.tola}) • ${item.survey.entityType}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(status = item.survey.status)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
