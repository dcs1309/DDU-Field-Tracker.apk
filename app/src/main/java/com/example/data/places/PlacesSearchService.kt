package com.example.data.places

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.SurveyWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

enum class PlaceResultSource {
    GOOGLE_PLACES,
    GEOCODER,
    LOCAL_FIELD_INTELLIGENCE
}

data class PlaceSearchResult(
    val id: String,
    val primaryText: String,
    val secondaryText: String,
    val fullAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val source: PlaceResultSource = PlaceResultSource.GOOGLE_PLACES,
    val placeTypes: List<String> = emptyList(),
    val matchedSurveyDduId: String? = null
)

class PlacesSearchService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val mapsApiKey: String by lazy {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isNotBlank() && !key.equals("MY_MAPS_API_KEY", ignoreCase = true) && !key.startsWith("MY_")) {
            key
        } else {
            ""
        }
    }

    val isGooglePlacesConfigured: Boolean
        get() = mapsApiKey.isNotBlank() && mapsApiKey.length >= 20

    /**
     * Searches for places matching the query using Google Places API with fallback to Android Geocoder
     * and local field intelligence survey points.
     */
    suspend fun searchPlaces(
        query: String,
        surveys: List<SurveyWithDetails> = emptyList(),
        proximityLat: Double? = null,
        proximityLng: Double? = null
    ): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<PlaceSearchResult>()

        // 1. First prioritize local matching Field Intelligence Surveys & Demands
        val localMatches = surveys.filter { item ->
            val s = item.survey
            s.entityName.contains(trimmed, ignoreCase = true) ||
            s.village.contains(trimmed, ignoreCase = true) ||
            s.tola.contains(trimmed, ignoreCase = true) ||
            s.dduId.contains(trimmed, ignoreCase = true) ||
            item.products.any { it.productName.contains(trimmed, ignoreCase = true) }
        }.take(4).map { item ->
            val s = item.survey
            PlaceSearchResult(
                id = "local_${s.dduId}",
                primaryText = s.entityName,
                secondaryText = "${s.dduId} • ${s.village}, ${s.tola.ifBlank { s.block }} (${s.entityType})",
                fullAddress = "${s.entityName}, ${s.village}, ${s.block}, ${s.district}",
                latitude = s.gpsLatitude,
                longitude = s.gpsLongitude,
                source = PlaceResultSource.LOCAL_FIELD_INTELLIGENCE,
                placeTypes = listOf(s.entityType.lowercase()),
                matchedSurveyDduId = s.dduId
            )
        }
        results.addAll(localMatches)

        // 2. Query Google Places API Autocomplete if API Key is available
        if (isGooglePlacesConfigured) {
            try {
                val googlePlaces = fetchGooglePlacesPredictions(trimmed, proximityLat, proximityLng)
                results.addAll(googlePlaces)
            } catch (e: Exception) {
                Log.w(TAG, "Google Places API autocomplete error: ${e.message}")
            }
        }

        // 3. Complement/Fallback with Android Native Geocoder (zero-key, works anywhere globally)
        try {
            val geocoderResults = fetchGeocoderResults(trimmed)
            // Deduplicate against Google Places results by address similarity
            geocoderResults.forEach { geoResult ->
                val alreadyExists = results.any {
                    it.primaryText.equals(geoResult.primaryText, ignoreCase = true) ||
                    (it.latitude != null && geoResult.latitude != null &&
                     Math.abs(it.latitude - geoResult.latitude) < 0.001 &&
                     Math.abs(it.longitude!! - geoResult.longitude!!) < 0.001)
                }
                if (!alreadyExists) {
                    results.add(geoResult)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Native Geocoder search error: ${e.message}")
        }

        results
    }

    /**
     * Fetches autocomplete predictions from Google Places REST API
     */
    private fun fetchGooglePlacesPredictions(
        input: String,
        proximityLat: Double?,
        proximityLng: Double?
    ): List<PlaceSearchResult> {
        val encodedInput = URLEncoder.encode(input, "UTF-8")
        val urlBuilder = StringBuilder(
            "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$encodedInput&key=$mapsApiKey&language=en"
        )

        // Bias towards current location or India field area
        if (proximityLat != null && proximityLng != null) {
            urlBuilder.append("&location=$proximityLat,$proximityLng&radius=50000")
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "Google Places HTTP error: ${response.code}")
            return emptyList()
        }

        val responseBody = response.body?.string() ?: return emptyList()
        val json = JSONObject(responseBody)
        val status = json.optString("status")

        if (status != "OK") {
            Log.w(TAG, "Google Places API status: $status")
            return emptyList()
        }

        val predictions = json.optJSONArray("predictions") ?: return emptyList()
        val list = mutableListOf<PlaceSearchResult>()

        for (i in 0 until predictions.length()) {
            val pred = predictions.getJSONObject(i)
            val placeId = pred.optString("place_id")
            val description = pred.optString("description")

            val structuredFormatting = pred.optJSONObject("structured_formatting")
            val mainText = structuredFormatting?.optString("main_text")?.ifBlank { null }
                ?: description.substringBefore(",")
            val secondaryText = structuredFormatting?.optString("secondary_text")?.ifBlank { null }
                ?: description.substringAfter(",", "")

            val typesArray = pred.optJSONArray("types")
            val typesList = mutableListOf<String>()
            if (typesArray != null) {
                for (j in 0 until typesArray.length()) {
                    typesList.add(typesArray.getString(j))
                }
            }

            list.add(
                PlaceSearchResult(
                    id = placeId,
                    primaryText = mainText.trim(),
                    secondaryText = secondaryText.trim().removePrefix(",").trim(),
                    fullAddress = description,
                    latitude = null, // Will be fetched via getPlaceDetails on tap
                    longitude = null,
                    source = PlaceResultSource.GOOGLE_PLACES,
                    placeTypes = typesList
                )
            )
        }

        return list
    }

    /**
     * Resolves exact coordinates and address for a Place ID or result
     */
    suspend fun resolvePlaceCoordinates(result: PlaceSearchResult): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        // Already has coordinates (from Local survey or Geocoder)
        if (result.latitude != null && result.longitude != null) {
            return@withContext Pair(result.latitude, result.longitude)
        }

        // Fetch from Google Places Details API
        if (isGooglePlacesConfigured && result.id.isNotBlank() && !result.id.startsWith("local_") && !result.id.startsWith("geo_")) {
            try {
                val encodedPlaceId = URLEncoder.encode(result.id, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=$encodedPlaceId&fields=name,formatted_address,geometry&key=$mapsApiKey"

                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val status = json.optString("status")
                        if (status == "OK") {
                            val resultObj = json.optJSONObject("result")
                            val geometry = resultObj?.optJSONObject("geometry")
                            val location = geometry?.optJSONObject("location")
                            if (location != null) {
                                val lat = location.optDouble("lat")
                                val lng = location.optDouble("lng")
                                if (!lat.isNaN() && !lng.isNaN()) {
                                    return@withContext Pair(lat, lng)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Google Place Details error: ${e.message}")
            }
        }

        // Fallback to Geocoding the full address
        try {
            val geocoderResults = fetchGeocoderResults(result.fullAddress)
            val first = geocoderResults.firstOrNull()
            if (first?.latitude != null && first.longitude != null) {
                return@withContext Pair(first.latitude, first.longitude)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder fallback error: ${e.message}")
        }

        null
    }

    /**
     * System Geocoder fallback that works natively across all Android versions
     */
    @Suppress("DEPRECATION")
    private suspend fun fetchGeocoderResults(query: String): List<PlaceSearchResult> {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (!Geocoder.isPresent()) return emptyList()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<List<PlaceSearchResult>> { continuation ->
                geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val mapped = addresses.mapIndexed { index, address -> addressToSearchResult(address, index) }
                        continuation.resume(mapped)
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(emptyList())
                    }
                })
            }
        } else {
            try {
                val list = geocoder.getFromLocationName(query, 5) ?: emptyList()
                list.mapIndexed { index, address -> addressToSearchResult(address, index) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun addressToSearchResult(address: Address, index: Int): PlaceSearchResult {
        val primary = address.featureName
            ?: address.subLocality
            ?: address.locality
            ?: address.subAdminArea
            ?: "Address Match"

        val secondaryParts = listOfNotNull(
            address.locality.takeIf { it != primary },
            address.adminArea,
            address.postalCode,
            address.countryName
        ).distinct()

        val fullAddress = (0..address.maxAddressLineIndex)
            .mapNotNull { address.getAddressLine(it) }
            .joinToString(", ")
            .ifBlank { "${primary}, ${secondaryParts.joinToString(", ")}" }

        return PlaceSearchResult(
            id = "geo_${address.latitude}_${address.longitude}_$index",
            primaryText = primary,
            secondaryText = secondaryParts.joinToString(", "),
            fullAddress = fullAddress,
            latitude = address.latitude,
            longitude = address.longitude,
            source = PlaceResultSource.GEOCODER,
            placeTypes = listOf("address")
        )
    }

    companion object {
        private const val TAG = "PlacesSearchService"
    }
}
