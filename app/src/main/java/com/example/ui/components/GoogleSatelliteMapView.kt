package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SurveyWithDetails
import org.json.JSONArray
import org.json.JSONObject

/**
 * Controller allowing external Composable components (FABs, chips, search)
 * to imperatively and reliably command the map view.
 */
class MapController {
    var webView: WebView? = null
        internal set

    fun zoomIn() {
        webView?.evaluateJavascript("if (window.zoomIn) window.zoomIn();", null)
    }

    fun zoomOut() {
        webView?.evaluateJavascript("if (window.zoomOut) window.zoomOut();", null)
    }

    fun recenter() {
        webView?.evaluateJavascript("if (window.recenterMap) window.recenterMap();", null)
    }

    fun flyTo(lat: Double, lng: Double, zoom: Int = 18) {
        webView?.evaluateJavascript("if (window.flyToLocation) window.flyToLocation($lat, $lng, $zoom);", null)
    }

    fun selectMarker(dduId: String, pan: Boolean = true) {
        webView?.evaluateJavascript("if (window.selectMarker) window.selectMarker('$dduId', $pan);", null)
    }

    fun setLayer(layer: String) {
        webView?.evaluateJavascript("if (window.setMapLayer) window.setMapLayer('$layer');", null)
    }

    fun fitAll() {
        webView?.evaluateJavascript("if (window.fitAllMarkers) window.fitAllMarkers();", null)
    }
}

@Composable
fun rememberMapController(): MapController = remember { MapController() }

class MapBridge(
    private val onMarkerSelected: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMarkerClick(dduId: String) {
        mainHandler.post {
            onMarkerSelected(dduId)
        }
    }

    @JavascriptInterface
    fun logFromJs(msg: String) {
        Log.d("GoogleSatelliteMapJS", msg)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun GoogleSatelliteMapView(
    surveys: List<SurveyWithDetails>,
    selectedSurvey: SurveyWithDetails?,
    mapLayerType: String, // "HYBRID", "SATELLITE", "VOYAGER", "STREET", "ESRI"
    targetLocation: Pair<Double, Double>?,
    targetZoom: Int?,
    onSurveySelected: (SurveyWithDetails) -> Unit,
    modifier: Modifier = Modifier,
    mapController: MapController = rememberMapController()
) {
    val bridge = remember(surveys) {
        MapBridge { dduId ->
            val found = surveys.find { it.survey.dduId == dduId }
            if (found != null) {
                onSurveySelected(found)
            }
        }
    }

    // Effect: Update selected marker in the WebView
    LaunchedEffect(selectedSurvey?.survey?.dduId) {
        val id = selectedSurvey?.survey?.dduId
        if (id != null) {
            mapController.selectMarker(id, pan = false)
        }
    }

    // Effect: Switch tile layer
    LaunchedEffect(mapLayerType) {
        mapController.setLayer(mapLayerType)
    }

    // Effect: Pan/Zoom to target location (e.g. on village jump or building zoom)
    LaunchedEffect(targetLocation, targetZoom) {
        if (targetLocation != null) {
            val zoom = targetZoom ?: 18
            mapController.flyTo(targetLocation.first, targetLocation.second, zoom)
        }
    }

    // Effect: Update markers if survey list changes
    LaunchedEffect(surveys) {
        val geoJsonData = generateSurveysJson(surveys)
        mapController.webView?.evaluateJavascript("if (window.updateMarkers) window.updateMarkers($geoJsonData);", null)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                mapController.webView = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                // Prevent Jetpack Compose from stealing pinch, drag, and pan touch gestures
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("if (!window.mapInitialized && window.tryInit) { window.tryInit(); }", null)
                        selectedSurvey?.survey?.dduId?.let { id ->
                            view?.evaluateJavascript("if (window.selectMarker) window.selectMarker('$id', false);", null)
                        }
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.w("GoogleSatelliteMap", "WebView resource error: ${error?.description} for ${request?.url}")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("GoogleSatelliteMapChrome", "${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})")
                        return true
                    }
                }

                addJavascriptInterface(bridge, "AndroidBridge")

                val initialHtml = buildMapHtml(surveys, selectedSurvey?.survey?.dduId, mapLayerType)
                loadDataWithBaseURL("https://www.google.com/", initialHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            mapController.webView = webView
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Serializes surveys and product demands into JSON for Leaflet and Vector engines.
 */
private fun generateSurveysJson(surveys: List<SurveyWithDetails>): String {
    val array = JSONArray()
    for (item in surveys) {
        val obj = JSONObject()
        obj.put("dduId", item.survey.dduId)
        obj.put("name", item.survey.entityName)
        obj.put("type", item.survey.surveyType)
        obj.put("entityType", item.survey.entityType)
        obj.put("village", item.survey.village)
        obj.put("tola", item.survey.tola)
        obj.put("lat", item.survey.gpsLatitude)
        obj.put("lng", item.survey.gpsLongitude)
        obj.put("accuracy", item.survey.gpsAccuracyMeters)
        obj.put("status", item.survey.status)
        obj.put("contact", item.survey.contactPerson)
        obj.put("phone", item.survey.contactNumber)
        val productsDesc = if (item.products.isNotEmpty()) {
            item.products.take(2).joinToString(", ") { "${it.productName} (${it.minQuantity.toInt()}${it.unit})" }
        } else {
            "No recorded products"
        }
        obj.put("products", productsDesc)
        array.put(obj)
    }
    return array.toString()
}

/**
 * Builds the resilient, multi-layer Leaflet + Interactive Vector Canvas Map HTML.
 * Features:
 * - Multi-CDN Leaflet loader with automatic fallback
 * - Google Satellite Hybrid, Google Pure Satellite, CartoDB Voyager, Esri World Imagery, OpenStreetMap
 * - maxNativeZoom: 19 so zooming into building level (19-20) never produces blank 404 tiles
 * - Precise iconAnchor for pixel-accurate pin tapping
 * - Village Clusters, Building compound footprints, and offline vector fallback
 */
private fun buildMapHtml(
    surveys: List<SurveyWithDetails>,
    initialSelectedId: String?,
    initialLayer: String
): String {
    val surveysJson = generateSurveysJson(surveys)
    val defaultId = initialSelectedId ?: ""

    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Field Intelligence Map</title>
  
  <!-- Leaflet CSS from reliable Cloudflare CDN with jsDelivr fallback -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" 
        onerror="this.onerror=null;this.href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css';" />
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.css" />
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.Default.css" />
  
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      width: 100%;
      height: 100%;
      background: #090e17;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      overflow: hidden;
      user-select: none;
      -webkit-user-select: none;
      touch-action: none;
    }
    
    #map-container {
      position: relative;
      width: 100%;
      height: 100%;
    }

    #map {
      width: 100%;
      height: 100%;
      background: #090e17;
      touch-action: none;
    }

    #vector-canvas {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      display: none;
      background: #080d14;
      touch-action: none;
    }
    
    /* Pixel-perfect Leaflet Pin Marker Styles */
    .ddu-marker-wrap {
      display: flex;
      flex-direction: column;
      align-items: center;
      cursor: pointer;
      position: relative;
      width: 38px;
      height: 48px;
    }
    
    .pin-body {
      width: 36px;
      height: 36px;
      border-radius: 50% 50% 50% 0;
      transform: rotate(-45deg);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 10px rgba(0,0,0,0.6), 0 0 0 2px #ffffff;
      transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.25s ease;
    }
    
    .ddu-marker-wrap.selected .pin-body {
      transform: rotate(-45deg) scale(1.25);
      box-shadow: 0 0 0 3px #ffffff, 0 0 20px #38bdf8, 0 6px 14px rgba(0,0,0,0.8);
      z-index: 9999 !important;
    }
    
    .pin-icon {
      transform: rotate(45deg);
      font-size: 15px;
      line-height: 1;
    }
    
    .pin-tag {
      position: absolute;
      top: 38px;
      background: rgba(15, 23, 42, 0.95);
      color: #ffffff;
      font-size: 9px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      white-space: nowrap;
      box-shadow: 0 2px 6px rgba(0,0,0,0.7);
      border: 1px solid rgba(255, 255, 255, 0.35);
      max-width: 100px;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: center;
      pointer-events: none;
    }

    /* Cluster Marker Styles */
    .ddu-cluster-wrap {
      background: transparent;
      border: none;
    }
    .ddu-cluster-bubble {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-weight: 800;
      font-size: 14px;
      line-height: 1;
      border: 3px solid #ffffff;
      box-shadow: 0 4px 12px rgba(0,0,0,0.6);
      transition: transform 0.2s ease;
    }
    .ddu-cluster-bubble:hover {
      transform: scale(1.1);
    }
    .ddu-cluster-bubble small {
      font-size: 7px;
      letter-spacing: 0.5px;
      opacity: 0.9;
    }
    .ddu-cluster-small { background: #00796B; }
    .ddu-cluster-medium { background: #0284C7; }
    .ddu-cluster-large { background: #E65100; }

    /* Status badge pill */
    #status-banner {
      position: absolute;
      bottom: 8px;
      left: 10px;
      z-index: 1000;
      background: rgba(15, 23, 42, 0.88);
      color: #94a3b8;
      font-size: 10px;
      font-weight: 600;
      padding: 3px 8px;
      border-radius: 6px;
      border: 1px solid rgba(255,255,255,0.12);
      pointer-events: none;
    }
  </style>

  <!-- Leaflet JS from reliable Cloudflare CDN with jsDelivr fallback -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/leaflet.markercluster.js"></script>
  <script>
    if (typeof L === 'undefined') {
      document.write('<script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"><\/script>');
    }
  </script>
</head>
<body>
  <div id="map-container">
    <div id="map"></div>
    <canvas id="vector-canvas"></canvas>
    <div id="status-banner">🛰️ Loading Field Map...</div>
  </div>

  <script>
    var map = null;
    var currentLayer = null;
    var markersMap = {};
    var buildingPolygons = [];
    var villageHalos = [];
    var selectedId = "$defaultId";
    var surveysData = $surveysJson;
    var activeEngine = "leaflet"; // "leaflet" or "vector"
    var isMapInitialized = false;

    // Village Clusters & Boundaries
    var villageClusters = [
      { name: "Rampur Tola Cluster (DDU Tailoring Hub)", center: [27.4312, 82.1892], radius: 240, color: "#10b981" },
      { name: "Juri Healthcare Cluster (Dorika Hospital)", center: [27.4280, 82.1950], radius: 210, color: "#06b6d4" },
      { name: "Balrampur Bazar Commercial Hub", center: [27.4350, 82.1820], radius: 260, color: "#f59e0b" },
      { name: "Grazi Village Cluster", center: [27.4410, 82.1760], radius: 220, color: "#8b5cf6" },
      { name: "Pipra Tola (Agricultural Zone)", center: [27.4265, 82.1870], radius: 200, color: "#ec4899" }
    ];

    // Building Compound Footprints
    var buildingFootprints = [
      {
        name: "Balrampur Govt. Middle School Campus",
        bounds: [[27.4315, 82.1889], [27.4315, 82.1896], [27.4309, 82.1896], [27.4309, 82.1889]],
        color: "#00796B",
        icon: "🏛️"
      },
      {
        name: "Dorika Hospital & Ward Complex",
        bounds: [[27.4284, 82.1946], [27.4284, 82.1954], [27.4276, 82.1954], [27.4276, 82.1946]],
        color: "#00796B",
        icon: "🏥"
      },
      {
        name: "Times Clinic Medical Block",
        bounds: [[27.4354, 82.1816], [27.4354, 82.1824], [27.4346, 82.1824], [27.4346, 82.1816]],
        color: "#00796B",
        icon: "🩺"
      },
      {
        name: "FS General Merchant & Market Block",
        bounds: [[27.4414, 82.1756], [27.4414, 82.1764], [27.4406, 82.1764], [27.4406, 82.1756]],
        color: "#E65100",
        icon: "🏪"
      },
      {
        name: "Primary Health Center (PHC Balrampur)",
        bounds: [[27.4365, 82.1845], [27.4365, 82.1853], [27.4358, 82.1853], [27.4358, 82.1845]],
        color: "#00796B",
        icon: "🏛️"
      },
      {
        name: "Gram Panchayat Bhawan & Community Hall",
        bounds: [[27.4335, 82.1865], [27.4335, 82.1872], [27.4328, 82.1872], [27.4328, 82.1865]],
        color: "#0284c7",
        icon: "🏢"
      },
      {
        name: "Balrampur Krishi Seva Kendra & Depot",
        bounds: [[27.4290, 82.1875], [27.4290, 82.1883], [27.4283, 82.1883], [27.4283, 82.1875]],
        color: "#E65100",
        icon: "🌾"
      },
      {
        name: "Anganwadi Center & Child Nutrition Hub",
        bounds: [[27.4302, 82.1905], [27.4302, 82.1911], [27.4296, 82.1911], [27.4296, 82.1905]],
        color: "#7B1FA2",
        icon: "👶"
      }
    ];

    var tileLayers = {};

    function initLeafletMap() {
      try {
        if (typeof L === 'undefined') {
          throw new Error("Leaflet library not loaded");
        }

        // Multiple resilient tile providers with maxNativeZoom: 19 so zooming into building level (19-20) upscales seamlessly
        var googleHybrid = L.tileLayer('https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
          subdomains: ['0', '1', '2', '3'],
          maxZoom: 20,
          maxNativeZoom: 19,
          attribution: '© Google Satellite'
        });

        var googleSatellite = L.tileLayer('https://mt{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
          subdomains: ['0', '1', '2', '3'],
          maxZoom: 20,
          maxNativeZoom: 19,
          attribution: '© Google'
        });

        var cartoVoyager = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
          subdomains: 'abcd',
          maxZoom: 20,
          maxNativeZoom: 19,
          attribution: '© CartoDB'
        });

        var osmStreet = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          maxNativeZoom: 19,
          attribution: '© OpenStreetMap'
        });

        var esriWorld = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
          maxZoom: 19,
          maxNativeZoom: 18,
          attribution: '© Esri'
        });

        tileLayers = {
          HYBRID: googleHybrid,
          SATELLITE: googleSatellite,
          VOYAGER: cartoVoyager,
          STREET: osmStreet,
          ESRI: esriWorld
        };

        map = L.map('map', {
          center: [27.4320, 82.1850],
          zoom: 15,
          zoomControl: false,
          attributionControl: false
        });

        var initialKey = '$initialLayer';
        if (!tileLayers[initialKey]) initialKey = 'HYBRID';
        currentLayer = tileLayers[initialKey];
        currentLayer.addTo(map);

        drawVillageClusters();
        drawBuildingFootprints();
        renderMarkers(surveysData);

        if (selectedId && markersMap[selectedId]) {
          selectMarker(selectedId, false);
        }

        activeEngine = "leaflet";
        isMapInitialized = true;
        window.mapInitialized = true;
        document.getElementById('status-banner').textContent = "🛰️ Satellite Hybrid Active";
      } catch (err) {
        console.warn("Leaflet map initialization failed, falling back to Vector Canvas Engine:", err);
        initVectorMap();
      }
    }

    function drawVillageClusters() {
      if (!map) return;
      villageClusters.forEach(function(c) {
        var circle = L.circle(c.center, {
          radius: c.radius,
          color: c.color,
          weight: 2,
          dashArray: '6, 6',
          fillColor: c.color,
          fillOpacity: 0.1
        }).addTo(map);

        circle.bindTooltip("<div style='font-weight:bold; font-size:10px; color:" + c.color + ";'>🏘️ " + c.name + "</div>", {
          permanent: false,
          direction: 'top'
        });
        villageHalos.push(circle);
      });
    }

    function drawBuildingFootprints() {
      if (!map) return;
      buildingFootprints.forEach(function(b) {
        var polygon = L.polygon(b.bounds, {
          color: b.color,
          weight: 2,
          fillColor: b.color,
          fillOpacity: 0.32,
          dashArray: '4, 4'
        }).addTo(map);

        polygon.bindTooltip("<div style='font-size:10px; font-weight:700;'>" + b.icon + " " + b.name + "</div>", {
          permanent: false,
          direction: 'center'
        });
        buildingPolygons.push(polygon);
      });
    }

    var markersClusterGroup = null;

    function renderMarkers(surveys) {
      if (activeEngine === "vector") {
        vectorState.surveys = surveys;
        renderVectorMap();
        return;
      }
      if (!map) return;

      if (markersClusterGroup) {
        map.removeLayer(markersClusterGroup);
      }
      Object.keys(markersMap).forEach(function(k) {
        if (markersMap[k].marker) {
          map.removeLayer(markersMap[k].marker);
        }
      });
      markersMap = {};

      var useCluster = (typeof L.markerClusterGroup !== 'undefined');
      if (useCluster) {
        markersClusterGroup = L.markerClusterGroup({
          maxClusterRadius: 40,
          spiderfyOnMaxZoom: true,
          showCoverageOnHover: false,
          zoomToBoundsOnClick: true,
          iconCreateFunction: function(cluster) {
            var count = cluster.getChildCount();
            var cClass = 'ddu-cluster-small';
            if (count >= 8) cClass = 'ddu-cluster-large';
            else if (count >= 4) cClass = 'ddu-cluster-medium';
            return L.divIcon({
              html: '<div class="ddu-cluster-bubble ' + cClass + '"><span>' + count + '</span><small>DDU</small></div>',
              className: 'ddu-cluster-wrap',
              iconSize: [44, 44]
            });
          }
        });
      } else {
        markersClusterGroup = L.layerGroup();
      }

      surveys.forEach(function(s) {
        var markerColor = "#15803D";
        var emoji = "🏛️";
        var t = (s.type || "").toUpperCase();
        if (t.indexOf("INSTITUTION") !== -1 || t.indexOf("SCHOOL") !== -1 || t.indexOf("HOSPITAL") !== -1 || t.indexOf("GOVT") !== -1) {
          markerColor = "#15803D"; // Green for institutional demand
          emoji = (t.indexOf("HOSPITAL") !== -1) ? "🏥" : (t.indexOf("SCHOOL") !== -1) ? "🏫" : "🏛️";
        } else if (t.indexOf("SHOP") !== -1 || t.indexOf("RETAIL") !== -1 || t.indexOf("STORE") !== -1 || t.indexOf("MERCHANT") !== -1) {
          markerColor = "#0284C7"; // Blue for local shop
          emoji = "🏪";
        } else {
          markerColor = "#EA580C"; // Orange for livelihood ecosystem
          emoji = (t.indexOf("SUPPLIER") !== -1) ? "📦" : (t.indexOf("TEXTILE") !== -1 || t.indexOf("SAKHYA") !== -1) ? "🧵" : "🌾";
        }

        var isSelected = (s.dduId === selectedId);
        var iconHtml = 
          "<div class='ddu-marker-wrap " + (isSelected ? "selected" : "") + "' id='pin-" + s.dduId + "'>" +
            "<div class='pin-body' style='background: " + markerColor + ";'>" +
              "<span class='pin-icon'>" + emoji + "</span>" +
            "</div>" +
            "<div class='pin-tag'>" + s.name + "</div>" +
          "</div>";

        // Accurate anchor pointing to the tip of the pin
        var customIcon = L.divIcon({
          className: 'ddu-icon-wrapper',
          html: iconHtml,
          iconSize: [38, 48],
          iconAnchor: [19, 36]
        });

        var marker = L.marker([s.lat, s.lng], { icon: customIcon });

        marker.on('click', function() {
          selectMarker(s.dduId, true);
          if (window.AndroidBridge && window.AndroidBridge.onMarkerClick) {
            window.AndroidBridge.onMarkerClick(s.dduId);
          }
        });

        markersClusterGroup.addLayer(marker);

        markersMap[s.dduId] = {
          marker: marker,
          data: s
        };
      });

      map.addLayer(markersClusterGroup);
    }

    // =========================================================================
    // BUILT-IN INTERACTIVE VECTOR CANVAS MAP ENGINE (OFFLINE FALLBACK & FAILSAFE)
    // =========================================================================
    var vectorCanvas = null;
    var vectorCtx = null;
    var vectorState = {
      centerLat: 27.4320,
      centerLng: 82.1850,
      zoomScale: 28000,
      isDragging: false,
      lastTouchX: 0,
      lastTouchY: 0,
      surveys: surveysData
    };

    function initVectorMap() {
      activeEngine = "vector";
      isMapInitialized = true;
      window.mapInitialized = true;
      document.getElementById('map').style.display = 'none';
      vectorCanvas = document.getElementById('vector-canvas');
      vectorCanvas.style.display = 'block';
      vectorCtx = vectorCanvas.getContext('2d');
      document.getElementById('status-banner').textContent = "🛰️ High-Precision Vector Satellite Engine";

      resizeVectorCanvas();
      window.addEventListener('resize', resizeVectorCanvas);
      setupVectorTouchListeners();
      renderVectorMap();
    }

    function resizeVectorCanvas() {
      if (!vectorCanvas) return;
      var dpr = window.devicePixelRatio || 1;
      vectorCanvas.width = vectorCanvas.clientWidth * dpr;
      vectorCanvas.height = vectorCanvas.clientHeight * dpr;
      renderVectorMap();
    }

    function projectToScreen(lat, lng) {
      if (!vectorCanvas) return { x: 0, y: 0 };
      var width = vectorCanvas.clientWidth;
      var height = vectorCanvas.clientHeight;
      var dLng = lng - vectorState.centerLng;
      var dLat = lat - vectorState.centerLat;
      var x = width / 2 + dLng * vectorState.zoomScale;
      var y = height / 2 - dLat * vectorState.zoomScale * 1.15;
      return { x: x, y: y };
    }

    function renderVectorMap() {
      if (!vectorCtx || !vectorCanvas) return;
      var ctx = vectorCtx;
      var dpr = window.devicePixelRatio || 1;
      var w = vectorCanvas.clientWidth;
      var h = vectorCanvas.clientHeight;

      ctx.save();
      ctx.scale(dpr, dpr);

      // Deep satellite grid background
      ctx.fillStyle = "#070c14";
      ctx.fillRect(0, 0, w, h);

      // Radial satellite background glow
      var grad = ctx.createRadialGradient(w / 2, h / 2, 40, w / 2, h / 2, Math.max(w, h));
      grad.addColorStop(0, "#0e1a2b");
      grad.addColorStop(1, "#05090f");
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, w, h);

      // Draw coordinate grid lines
      ctx.strokeStyle = "rgba(56, 189, 248, 0.08)";
      ctx.lineWidth = 1;
      for (var x = 0; x < w; x += 60) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, h);
        ctx.stroke();
      }
      for (var y = 0; y < h; y += 60) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(w, y);
        ctx.stroke();
      }

      // Draw village clusters
      villageClusters.forEach(function(c) {
        var pt = projectToScreen(c.center[0], c.center[1]);
        var r = (c.radius / 111320) * vectorState.zoomScale;

        ctx.beginPath();
        ctx.arc(pt.x, pt.y, Math.max(r, 20), 0, Math.PI * 2);
        ctx.fillStyle = c.color + "18";
        ctx.fill();
        ctx.setLineDash([6, 6]);
        ctx.strokeStyle = c.color;
        ctx.lineWidth = 1.8;
        ctx.stroke();
        ctx.setLineDash([]);

        ctx.fillStyle = c.color;
        ctx.font = "bold 11px sans-serif";
        ctx.textAlign = "center";
        ctx.fillText("🏘️ " + c.name, pt.x, pt.y - Math.max(r, 20) - 6);
      });

      // Draw building footprints
      buildingFootprints.forEach(function(b) {
        if (!b.bounds || b.bounds.length < 4) return;
        ctx.beginPath();
        var p0 = projectToScreen(b.bounds[0][0], b.bounds[0][1]);
        ctx.moveTo(p0.x, p0.y);
        for (var i = 1; i < b.bounds.length; i++) {
          var pi = projectToScreen(b.bounds[i][0], b.bounds[i][1]);
          ctx.lineTo(pi.x, pi.y);
        }
        ctx.closePath();
        ctx.fillStyle = b.color + "44";
        ctx.fill();
        ctx.setLineDash([4, 4]);
        ctx.strokeStyle = b.color;
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.setLineDash([]);

        var centerPt = projectToScreen(
          (b.bounds[0][0] + b.bounds[2][0]) / 2,
          (b.bounds[0][1] + b.bounds[2][1]) / 2
        );
        ctx.fillStyle = "#ffffff";
        ctx.font = "bold 10px sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(b.icon + " " + b.name, centerPt.x, centerPt.y);
      });

      // Draw survey markers
      vectorState.surveys.forEach(function(s) {
        var pt = projectToScreen(s.lat, s.lng);
        var isSelected = (s.dduId === selectedId);

        var color = "#15803D";
        var emoji = "🏛️";
        var t = ((s.type || "") + " " + (s.entityType || "")).toUpperCase();
        if (t.indexOf("INSTITUTION") !== -1 || t.indexOf("SCHOOL") !== -1 || t.indexOf("HOSPITAL") !== -1 || t.indexOf("GOVT") !== -1) {
          color = "#15803D"; // Green for institutional demand
          emoji = (t.indexOf("HOSPITAL") !== -1) ? "🏥" : (t.indexOf("SCHOOL") !== -1) ? "🏫" : "🏛️";
        } else if (t.indexOf("SHOP") !== -1 || t.indexOf("RETAIL") !== -1 || t.indexOf("STORE") !== -1 || t.indexOf("MERCHANT") !== -1) {
          color = "#0284C7"; // Blue for local shop
          emoji = "🏪";
        } else {
          color = "#EA580C"; // Orange for livelihood ecosystem
          emoji = (t.indexOf("SUPPLIER") !== -1) ? "📦" : (t.indexOf("TEXTILE") !== -1 || t.indexOf("SAKHYA") !== -1) ? "🧵" : "🌾";
        }

        // Selection glow
        if (isSelected) {
          ctx.beginPath();
          ctx.arc(pt.x, pt.y, 24, 0, Math.PI * 2);
          ctx.fillStyle = "rgba(56, 189, 248, 0.4)";
          ctx.fill();
        }

        // Pin body
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, isSelected ? 15 : 12, 0, Math.PI * 2);
        ctx.fillStyle = color;
        ctx.fill();
        ctx.lineWidth = isSelected ? 3 : 2;
        ctx.strokeStyle = "#ffffff";
        ctx.stroke();

        // Pin text icon
        ctx.fillStyle = "#ffffff";
        ctx.font = (isSelected ? "14px" : "11px") + " sans-serif";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(emoji, pt.x, pt.y);

        // Label Pill
        var labelText = s.name;
        ctx.font = "bold 10px sans-serif";
        var textWidth = ctx.measureText(labelText).width;
        var pillW = textWidth + 14;
        var pillH = 18;
        var pillX = pt.x - pillW / 2;
        var pillY = pt.y + (isSelected ? 20 : 16);

        ctx.fillStyle = "rgba(15, 23, 42, 0.92)";
        ctx.beginPath();
        ctx.roundRect(pillX, pillY, pillW, pillH, 5);
        ctx.fill();
        ctx.strokeStyle = isSelected ? "#38bdf8" : "rgba(255,255,255,0.3)";
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = "#ffffff";
        ctx.textBaseline = "middle";
        ctx.fillText(labelText, pt.x, pillY + pillH / 2);
      });

      ctx.restore();
    }

    function setupVectorTouchListeners() {
      if (!vectorCanvas) return;
      var lastDist = 0;

      vectorCanvas.addEventListener('touchstart', function(e) {
        if (e.touches.length === 1) {
          vectorState.isDragging = true;
          vectorState.lastTouchX = e.touches[0].clientX;
          vectorState.lastTouchY = e.touches[0].clientY;
        } else if (e.touches.length === 2) {
          vectorState.isDragging = false;
          var dx = e.touches[0].clientX - e.touches[1].clientX;
          var dy = e.touches[0].clientY - e.touches[1].clientY;
          lastDist = Math.hypot(dx, dy);
        }
      }, { passive: false });

      vectorCanvas.addEventListener('touchmove', function(e) {
        e.preventDefault();
        if (e.touches.length === 1 && vectorState.isDragging) {
          var dx = e.touches[0].clientX - vectorState.lastTouchX;
          var dy = e.touches[0].clientY - vectorState.lastTouchY;
          vectorState.lastTouchX = e.touches[0].clientX;
          vectorState.lastTouchY = e.touches[0].clientY;

          vectorState.centerLng -= dx / vectorState.zoomScale;
          vectorState.centerLat += dy / (vectorState.zoomScale * 1.15);
          renderVectorMap();
        } else if (e.touches.length === 2) {
          var dx = e.touches[0].clientX - e.touches[1].clientX;
          var dy = e.touches[0].clientY - e.touches[1].clientY;
          var dist = Math.hypot(dx, dy);
          if (lastDist > 0) {
            var factor = dist / lastDist;
            vectorState.zoomScale = Math.min(Math.max(vectorState.zoomScale * factor, 8000), 120000);
            renderVectorMap();
          }
          lastDist = dist;
        }
      }, { passive: false });

      vectorCanvas.addEventListener('touchend', function(e) {
        if (e.touches.length === 0) {
          vectorState.isDragging = false;
        }
      });

      vectorCanvas.addEventListener('click', function(e) {
        var rect = vectorCanvas.getBoundingClientRect();
        var clickX = e.clientX - rect.left;
        var clickY = e.clientY - rect.top;

        for (var i = vectorState.surveys.length - 1; i >= 0; i--) {
          var s = vectorState.surveys[i];
          var pt = projectToScreen(s.lat, s.lng);
          var dist = Math.hypot(clickX - pt.x, clickY - pt.y);
          if (dist < 26) {
            selectMarker(s.dduId, true);
            if (window.AndroidBridge && window.AndroidBridge.onMarkerClick) {
              window.AndroidBridge.onMarkerClick(s.dduId);
            }
            return;
          }
        }
      });
    }

    // =========================================================================
    // EXPOSED PUBLIC CONTROLLER FUNCTIONS
    // =========================================================================
    window.selectMarker = function(dduId, pan) {
      selectedId = dduId;

      if (activeEngine === "leaflet" && map) {
        var prevPin = document.querySelector('.ddu-marker-wrap.selected');
        if (prevPin) prevPin.classList.remove('selected');

        var target = markersMap[dduId];
        if (target) {
          var currentPin = document.querySelector('#pin-' + dduId);
          if (currentPin) currentPin.classList.add('selected');

          if (pan !== false) {
            map.flyTo([target.data.lat, target.data.lng], Math.max(map.getZoom(), 17), {
              duration: 0.8
            });
          }
        }
      } else {
        var match = vectorState.surveys.find(function(s) { return s.dduId === dduId; });
        if (match && pan !== false) {
          vectorState.centerLat = match.lat;
          vectorState.centerLng = match.lng;
          vectorState.zoomScale = Math.max(vectorState.zoomScale, 45000);
        }
        renderVectorMap();
      }
    };

    window.zoomIn = function() {
      if (activeEngine === "leaflet" && map) {
        map.zoomIn();
      } else {
        vectorState.zoomScale = Math.min(vectorState.zoomScale * 1.35, 120000);
        renderVectorMap();
      }
    };

    window.zoomOut = function() {
      if (activeEngine === "leaflet" && map) {
        map.zoomOut();
      } else {
        vectorState.zoomScale = Math.max(vectorState.zoomScale / 1.35, 8000);
        renderVectorMap();
      }
    };

    window.recenterMap = function() {
      if (activeEngine === "leaflet" && map) {
        map.flyTo([27.4320, 82.1850], 15, { duration: 0.8 });
      } else {
        vectorState.centerLat = 27.4320;
        vectorState.centerLng = 82.1850;
        vectorState.zoomScale = 28000;
        renderVectorMap();
      }
    };

    window.flyToLocation = function(lat, lng, zoom) {
      if (activeEngine === "leaflet" && map) {
        map.flyTo([lat, lng], zoom || 18, { duration: 0.8 });
      } else {
        vectorState.centerLat = lat;
        vectorState.centerLng = lng;
        var z = zoom || 18;
        vectorState.zoomScale = Math.min(Math.max(12000 * Math.pow(1.3, z - 13), 10000), 120000);
        renderVectorMap();
      }
    };

    window.setMapLayer = function(layerType) {
      if (activeEngine === "leaflet" && map && tileLayers) {
        if (currentLayer) map.removeLayer(currentLayer);
        var target = tileLayers[layerType] || tileLayers['HYBRID'];
        if (target) {
          currentLayer = target;
          currentLayer.addTo(map);
          document.getElementById('status-banner').textContent = "🛰️ Layer: " + layerType;
        }
      }
    };

    window.updateMarkers = function(newSurveys) {
      surveysData = newSurveys;
      renderMarkers(newSurveys);
    };

    window.fitAllMarkers = function() {
      if (activeEngine === "leaflet" && map) {
        var group = [];
        Object.keys(markersMap).forEach(function(k) {
          group.push(markersMap[k].marker);
        });
        if (group.length > 0) {
          var featureGroup = L.featureGroup(group);
          map.fitBounds(featureGroup.getBounds().pad(0.2));
        }
      } else {
        window.recenterMap();
      }
    };

    window.tryInit = function() {
      if (isMapInitialized) return;
      if (typeof L !== 'undefined') {
        initLeafletMap();
      } else {
        initVectorMap();
      }
    };

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
      window.tryInit();
    } else {
      document.addEventListener('DOMContentLoaded', window.tryInit);
      window.addEventListener('load', window.tryInit);
      setTimeout(window.tryInit, 500);
    }
  </script>
</body>
</html>
""".trimIndent()
}
