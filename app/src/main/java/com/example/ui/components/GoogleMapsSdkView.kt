package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SurveyWithDetails
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import kotlinx.coroutines.launch
import java.util.Locale

data class FieldCategoryMarkerStyle(
    val mainColorInt: Int,
    val composeColor: Color,
    val iconEmoji: String,
    val label: String
)

fun getMarkerStyleForCategory(type: String): FieldCategoryMarkerStyle {
    val normalized = type.uppercase(Locale.ROOT).trim()
    return when {
        normalized.contains("INSTITUTION") || normalized.contains("SCHOOL") || normalized.contains("HOSPITAL") || normalized.contains("GOVT") -> {
            FieldCategoryMarkerStyle(
                mainColorInt = android.graphics.Color.parseColor("#15803D"), // Rich Green for institutional demand
                composeColor = Color(0xFF15803D),
                iconEmoji = when {
                    normalized.contains("HOSPITAL") -> "🏥"
                    normalized.contains("SCHOOL") -> "🏫"
                    else -> "🏛️"
                },
                label = "Institutional Demand"
            )
        }
        normalized.contains("SHOP") || normalized.contains("RETAIL") || normalized.contains("STORE") || normalized.contains("MERCHANT") -> {
            FieldCategoryMarkerStyle(
                mainColorInt = android.graphics.Color.parseColor("#0284C7"), // Vibrant Blue for local shop
                composeColor = Color(0xFF0284C7),
                iconEmoji = "🏪",
                label = "Local Shop"
            )
        }
        else -> {
            // Livelihood ecosystem (Suppliers, Artisans, Farmers, SHGs, Field Observations)
            FieldCategoryMarkerStyle(
                mainColorInt = android.graphics.Color.parseColor("#EA580C"), // Radiant Orange for livelihood ecosystem
                composeColor = Color(0xFFEA580C),
                iconEmoji = when {
                    normalized.contains("SUPPLIER") -> "📦"
                    normalized.contains("TEXTILE") || normalized.contains("SAKHYA") -> "🧵"
                    else -> "🌾"
                },
                label = "Livelihood Ecosystem"
            )
        }
    }
}

// Data class representing village bounds
data class SdkVillageCluster(
    val name: String,
    val center: LatLng,
    val radiusMeters: Double,
    val color: Color
)

// Data class representing building compound
data class SdkBuildingFootprint(
    val name: String,
    val bounds: List<LatLng>,
    val color: Color,
    val iconEmoji: String
)

/**
 * Cluster Item adapter for DDU Field Intelligence surveys
 */
data class SurveyClusterItem(
    val surveyWithDetails: SurveyWithDetails,
    val isSelected: Boolean
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(
        surveyWithDetails.survey.gpsLatitude,
        surveyWithDetails.survey.gpsLongitude
    )

    override fun getTitle(): String = surveyWithDetails.survey.entityName
    override fun getSnippet(): String = "${surveyWithDetails.survey.village} (${surveyWithDetails.survey.entityType})"
    override fun getZIndex(): Float = if (isSelected) 100f else 1f
}

// Data class representing searched place pin
data class SearchedPlacePin(
    val title: String,
    val snippet: String,
    val position: LatLng
)

/**
 * Controller for Native Google Maps SDK
 */
class GoogleMapsSdkController {
    var cameraPositionState: CameraPositionState? = null

    suspend fun animateTo(lat: Double, lng: Double, zoom: Float = 18f, tilt: Float = 0f) {
        val position = CameraPosition.builder()
            .target(LatLng(lat, lng))
            .zoom(zoom)
            .tilt(tilt)
            .build()
        cameraPositionState?.animate(CameraUpdateFactory.newCameraPosition(position), 800)
    }

    suspend fun zoomIn() {
        cameraPositionState?.animate(CameraUpdateFactory.zoomIn(), 400)
    }

    suspend fun zoomOut() {
        cameraPositionState?.animate(CameraUpdateFactory.zoomOut(), 400)
    }

    suspend fun fitBounds(surveys: List<SurveyWithDetails>) {
        if (surveys.isEmpty()) return
        val builder = LatLngBounds.builder()
        surveys.forEach {
            builder.include(LatLng(it.survey.gpsLatitude, it.survey.gpsLongitude))
        }
        val bounds = builder.build()
        cameraPositionState?.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120), 800)
    }

    suspend fun tiltBuildingView(lat: Double, lng: Double) {
        val position = CameraPosition.builder()
            .target(LatLng(lat, lng))
            .zoom(19.2f)
            .tilt(45f)
            .bearing(30f)
            .build()
        cameraPositionState?.animate(CameraUpdateFactory.newCameraPosition(position), 1000)
    }
}

@Composable
fun rememberGoogleMapsSdkController(): GoogleMapsSdkController = remember { GoogleMapsSdkController() }

@Composable
fun GoogleMapsSdkView(
    surveys: List<SurveyWithDetails>,
    selectedSurvey: SurveyWithDetails?,
    mapType: MapType, // MapType.HYBRID, MapType.SATELLITE, MapType.NORMAL, MapType.TERRAIN
    onSurveySelected: (SurveyWithDetails) -> Unit,
    modifier: Modifier = Modifier,
    controller: GoogleMapsSdkController = rememberGoogleMapsSdkController(),
    is3dBuildingEnabled: Boolean = true,
    isMyLocationEnabled: Boolean = false,
    isClusteringEnabled: Boolean = true,
    searchedPlacePin: SearchedPlacePin? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val initialCenter = remember(surveys) {
        if (surveys.isNotEmpty()) {
            LatLng(surveys[0].survey.gpsLatitude, surveys[0].survey.gpsLongitude)
        } else {
            LatLng(27.4320, 82.1850)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 15.5f)
    }

    // Attach camera state to controller
    LaunchedEffect(cameraPositionState) {
        controller.cameraPositionState = cameraPositionState
    }

    // Prepare cluster items
    val clusterItems = remember(surveys, selectedSurvey?.survey?.dduId) {
        surveys.map { item ->
            SurveyClusterItem(
                surveyWithDetails = item,
                isSelected = selectedSurvey?.survey?.dduId == item.survey.dduId
            )
        }
    }

    val villageClusters = remember {
        listOf(
            SdkVillageCluster("Rampur Tola (DDU Hub)", LatLng(27.4312, 82.1892), 240.0, Color(0xFF10B981)),
            SdkVillageCluster("Juri Healthcare (Hospital)", LatLng(27.4280, 82.1950), 210.0, Color(0xFF06B6D4)),
            SdkVillageCluster("Balrampur Commercial Hub", LatLng(27.4350, 82.1820), 260.0, Color(0xFFF59E0B)),
            SdkVillageCluster("Grazi Village Cluster", LatLng(27.4410, 82.1760), 220.0, Color(0xFF8B5CF6)),
            SdkVillageCluster("Pipra Agricultural Zone", LatLng(27.4265, 82.1870), 200.0, Color(0xFFEC4899))
        )
    }

    val buildingFootprints = remember {
        listOf(
            SdkBuildingFootprint(
                name = "Balrampur Govt. Middle School",
                bounds = listOf(
                    LatLng(27.4315, 82.1889),
                    LatLng(27.4315, 82.1896),
                    LatLng(27.4309, 82.1896),
                    LatLng(27.4309, 82.1889)
                ),
                color = Color(0xFF00796B),
                iconEmoji = "🏛️"
            ),
            SdkBuildingFootprint(
                name = "Dorika Hospital & Ward Complex",
                bounds = listOf(
                    LatLng(27.4284, 82.1946),
                    LatLng(27.4284, 82.1954),
                    LatLng(27.4276, 82.1954),
                    LatLng(27.4276, 82.1946)
                ),
                color = Color(0xFF00796B),
                iconEmoji = "🏥"
            ),
            SdkBuildingFootprint(
                name = "Times Clinic Medical Block",
                bounds = listOf(
                    LatLng(27.4354, 82.1816),
                    LatLng(27.4354, 82.1824),
                    LatLng(27.4346, 82.1824),
                    LatLng(27.4346, 82.1816)
                ),
                color = Color(0xFF00796B),
                iconEmoji = "🩺"
            ),
            SdkBuildingFootprint(
                name = "FS General Merchant & Market Block",
                bounds = listOf(
                    LatLng(27.4414, 82.1756),
                    LatLng(27.4414, 82.1764),
                    LatLng(27.4406, 82.1764),
                    LatLng(27.4406, 82.1756)
                ),
                color = Color(0xFFE65100),
                iconEmoji = "🏪"
            ),
            SdkBuildingFootprint(
                name = "Gram Panchayat Bhawan & Community Hall",
                bounds = listOf(
                    LatLng(27.4335, 82.1865),
                    LatLng(27.4335, 82.1872),
                    LatLng(27.4328, 82.1872),
                    LatLng(27.4328, 82.1865)
                ),
                color = Color(0xFF0284C7),
                iconEmoji = "🏢"
            ),
            SdkBuildingFootprint(
                name = "Balrampur Krishi Seva Kendra Depot",
                bounds = listOf(
                    LatLng(27.4290, 82.1875),
                    LatLng(27.4290, 82.1883),
                    LatLng(27.4283, 82.1883),
                    LatLng(27.4283, 82.1875)
                ),
                color = Color(0xFFE65100),
                iconEmoji = "🌾"
            ),
            SdkBuildingFootprint(
                name = "Anganwadi Center & Child Nutrition Hub",
                bounds = listOf(
                    LatLng(27.4302, 82.1905),
                    LatLng(27.4302, 82.1911),
                    LatLng(27.4296, 82.1911),
                    LatLng(27.4296, 82.1905)
                ),
                color = Color(0xFF7B1FA2),
                iconEmoji = "👶"
            )
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    val mapProperties = remember(mapType, is3dBuildingEnabled, isMyLocationEnabled) {
        MapProperties(
            mapType = mapType,
            isBuildingEnabled = is3dBuildingEnabled,
            isMyLocationEnabled = isMyLocationEnabled,
            isTrafficEnabled = false
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = {
                // Background tap
            }
        ) {
            // 1. Draw Village Cluster Circles with halos
            villageClusters.forEach { cluster ->
                Circle(
                    center = cluster.center,
                    radius = cluster.radiusMeters,
                    fillColor = cluster.color.copy(alpha = 0.12f),
                    strokeColor = cluster.color,
                    strokeWidth = 3f,
                    clickable = true,
                    onClick = {
                        coroutineScope.launch {
                            controller.animateTo(cluster.center.latitude, cluster.center.longitude, zoom = 17.5f)
                        }
                    }
                )
            }

            // 2. Draw Building Compound Footprints
            buildingFootprints.forEach { footprint ->
                Polygon(
                    points = footprint.bounds,
                    fillColor = footprint.color.copy(alpha = 0.35f),
                    strokeColor = footprint.color,
                    strokeWidth = 4f,
                    clickable = true,
                    onClick = {
                        val centerLat = (footprint.bounds[0].latitude + footprint.bounds[2].latitude) / 2
                        val centerLng = (footprint.bounds[0].longitude + footprint.bounds[2].longitude) / 2
                        coroutineScope.launch {
                            controller.tiltBuildingView(centerLat, centerLng)
                        }
                    }
                )
            }

            // 3. Clustered Markers (Prevents overcrowding in dense village clusters)
            if (isClusteringEnabled) {
                Clustering(
                    items = clusterItems,
                    onClusterClick = { cluster ->
                        // Smoothly fit the bounds of all markers in the cluster
                        val builder = LatLngBounds.builder()
                        cluster.items.forEach { builder.include(it.position) }
                        val bounds = builder.build()
                        coroutineScope.launch {
                            val update = CameraUpdateFactory.newLatLngBounds(bounds, 120)
                            controller.cameraPositionState?.animate(update, 600)
                        }
                        true
                    },
                    onClusterItemClick = { item ->
                        onSurveySelected(item.surveyWithDetails)
                        coroutineScope.launch {
                            controller.animateTo(item.position.latitude, item.position.longitude, zoom = 18f)
                        }
                        true
                    },
                    clusterContent = { cluster ->
                        val count = cluster.size
                        val badgeColor = when {
                            count >= 8 -> Color(0xFFE65100) // Vibrant orange
                            count >= 4 -> Color(0xFF0284C7) // Sky blue
                            else -> Color(0xFF00796B)       // Teal
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$count",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "DDU",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    clusterItemContent = { item ->
                        val isSelected = selectedSurvey?.survey?.dduId == item.surveyWithDetails.survey.dduId
                        val type = item.surveyWithDetails.survey.surveyType
                        val style = getMarkerStyleForCategory(type)

                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x6638BDF8))
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 38.dp else 32.dp)
                                        .clip(CircleShape)
                                        .background(style.composeColor)
                                        .border(2.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.iconEmoji,
                                        fontSize = if (isSelected) 17.sp else 14.sp
                                    )
                                }
                                // Teardrop pointer needle
                                Box(
                                    modifier = Modifier
                                        .offset(y = (-3).dp)
                                        .size(8.dp)
                                        .rotate(45f)
                                        .background(style.composeColor)
                                )
                            }
                        }
                    }
                )
            } else {
                // Fallback unclustered raw markers
                surveys.forEach { surveyWithDetails ->
                    val survey = surveyWithDetails.survey
                    val isSelected = selectedSurvey?.survey?.dduId == survey.dduId
                    val position = LatLng(survey.gpsLatitude, survey.gpsLongitude)

                    key(survey.dduId) {
                        val customIcon = remember(survey.surveyType, isSelected) {
                            createCustomMarkerBitmap(
                                type = survey.surveyType,
                                isSelected = isSelected
                            )
                        }

                        val markerState = remember(survey.dduId) { MarkerState(position = position) }
                        markerState.position = position

                        Marker(
                            state = markerState,
                            title = survey.entityName,
                            snippet = "${survey.village} (${survey.entityType})",
                            icon = customIcon,
                            anchor = Offset(0.5f, 0.94f),
                            zIndex = if (isSelected) 100f else 1f,
                            onClick = {
                                onSurveySelected(surveyWithDetails)
                                coroutineScope.launch {
                                    controller.animateTo(survey.gpsLatitude, survey.gpsLongitude, zoom = 18f)
                                }
                                true
                            }
                        )
                    }
                }
            }

            // 4. Searched Place Pin from Google Places API
            if (searchedPlacePin != null) {
                val searchPinState = remember(searchedPlacePin.position) { MarkerState(position = searchedPlacePin.position) }
                searchPinState.position = searchedPlacePin.position
                Marker(
                    state = searchPinState,
                    title = searchedPlacePin.title,
                    snippet = searchedPlacePin.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 200f
                )
            }
        }
    }
}

/**
 * Creates high-performance crisp vector bitmap descriptors for Google Maps SDK markers
 * Custom teardrop shape styled according to the field data category:
 * - Green for Institutional Demand (🏛️ / 🏫 / 🏥)
 * - Blue for Local Shop (🏪)
 * - Orange for Livelihood Ecosystem (🌾 / 🧵 / 📦)
 */
private fun createCustomMarkerBitmap(
    type: String,
    isSelected: Boolean
): BitmapDescriptor {
    val style = getMarkerStyleForCategory(type)
    val width = if (isSelected) 116 else 92
    val height = (width * 1.30f).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val centerX = width / 2f
    val headRadius = width * 0.36f
    val headCenterY = headRadius + (if (isSelected) 10f else 6f)
    val tipY = height - 8f

    // 1. Soft Ground Shadow underneath pin tip
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.argb(70, 0, 0, 0)
    canvas.drawOval(centerX - 16f, height - 12f, centerX + 16f, height - 2f, paint)

    // 2. Selection Glow Halo
    if (isSelected) {
        paint.color = android.graphics.Color.parseColor("#38BDF8")
        paint.alpha = 140
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawCircle(centerX, headCenterY, headRadius + 6f, paint)
    }

    // 3. Teardrop Pin Body Path
    val path = android.graphics.Path()
    val angle = 36.0
    val rad = Math.toRadians(angle)
    val cosA = (Math.cos(rad) * headRadius).toFloat()
    val sinA = (Math.sin(rad) * headRadius).toFloat()

    val leftX = centerX - cosA
    val leftY = headCenterY + sinA

    path.moveTo(leftX, leftY)
    val oval = android.graphics.RectF(
        centerX - headRadius,
        headCenterY - headRadius,
        centerX + headRadius,
        headCenterY + headRadius
    )
    path.arcTo(oval, 180f - angle.toFloat(), 180f + 2 * angle.toFloat(), false)
    path.lineTo(centerX, tipY)
    path.close()

    // Fill Teardrop with Category Color (Green / Blue / Orange)
    paint.style = Paint.Style.FILL
    paint.color = style.mainColorInt
    paint.alpha = 255
    canvas.drawPath(path, paint)

    // White Border around Teardrop
    paint.style = Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = if (isSelected) 5.5f else 4f
    canvas.drawPath(path, paint)

    // 4. Inner White Circle Badge
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    val innerBadgeRadius = headRadius * 0.70f
    canvas.drawCircle(centerX, headCenterY, innerBadgeRadius, paint)

    // 5. Category Icon / Emoji Glyph
    paint.textSize = if (isSelected) 28f else 22f
    paint.textAlign = Paint.Align.CENTER
    val textY = headCenterY - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(style.iconEmoji, centerX, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
