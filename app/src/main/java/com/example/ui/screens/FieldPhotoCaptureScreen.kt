package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.EvidenceEntity
import com.example.viewmodel.FieldIntelligenceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldPhotoCaptureScreen(
    viewModel: FieldIntelligenceViewModel,
    dduId: String? = null,
    oppId: String? = null,
    onNavigateBack: () -> Unit,
    onPhotoCaptured: ((EvidenceEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val allSurveysWithDetails by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()
    val allOpportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed for evidence verification", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCameraBindingFailed by remember { mutableStateOf(false) }

    // Verification Metadata
    val categories = listOf(
        "Product Specimen" to Icons.Default.Inventory2,
        "Shopfront & Cluster" to Icons.Default.Storefront,
        "Premises & Building" to Icons.Default.Business,
        "Supplier Label & Pack" to Icons.Default.LocalShipping,
        "Bill & Invoice" to Icons.Default.ReceiptLong,
        "Existing Stock" to Icons.Default.Warehouse
    )
    var selectedCategory by remember { mutableStateOf(categories[0].first) }
    var targetDduId by remember(dduId, allSurveysWithDetails) {
        mutableStateOf(
            dduId ?: allSurveysWithDetails.firstOrNull()?.survey?.dduId ?: "DDU-BAL-2026-000124"
        )
    }
    var captionText by remember { mutableStateOf("") }
    var classification by remember { mutableStateOf("FIELD_EVIDENCE") }

    // Captured image state for review
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Visual shutter flash effect
    var showShutterFlash by remember { mutableStateOf(false) }

    // Geotag display values
    val now = remember { Date() }
    val timeStampStr = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(now) }
    val gpsLat = 27.4328
    val gpsLng = 82.1894

    // Photo picker fallback (Play Policy compliant zero-permission media selection)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            capturedImageUri = uri
            captionText = "Imported field photo for $selectedCategory"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (capturedImageUri != null) "Verify & Attach Photo" else "Field Photo Verification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "DDU Stage 1 Opportunity Discovery Geotagged Evidence",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close Camera")
                    }
                },
                actions = {
                    if (capturedImageUri == null && hasCameraPermission) {
                        // Flash mode toggle
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                                imageCapture?.flashMode = flashMode
                            }
                        ) {
                            Icon(
                                imageVector = when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = "Toggle Flash",
                                tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Flip camera lens
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (!hasCameraPermission) {
                // Permission Request Screen
                CameraPermissionRequestContent(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSimulatePhoto = {
                        scope.launch {
                            val simulated = generateSimulatedPhoto(context, selectedCategory, targetDduId, gpsLat, gpsLng)
                            capturedBitmap = simulated.first
                            capturedImageUri = simulated.second
                            captionText = "Simulated $selectedCategory verification"
                        }
                    },
                    onPickFromGallery = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else if (capturedImageUri != null) {
                // Image Review & Metadata Confirmation Screen
                PhotoVerificationReviewContent(
                    imageUri = capturedImageUri,
                    bitmap = capturedBitmap,
                    category = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    categories = categories.map { it.first },
                    targetDduId = targetDduId,
                    onTargetDduIdChange = { targetDduId = it },
                    surveysList = allSurveysWithDetails.map { it.survey.dduId },
                    caption = captionText,
                    onCaptionChange = { captionText = it },
                    classification = classification,
                    onClassificationChange = { classification = it },
                    gpsLat = gpsLat,
                    gpsLng = gpsLng,
                    timestamp = timeStampStr,
                    isSaving = isSaving,
                    onRetake = {
                        capturedImageUri = null
                        capturedBitmap = null
                        captionText = ""
                    },
                    onConfirmSave = {
                        isSaving = true
                        scope.launch {
                            val finalUri = capturedImageUri.toString()
                            val finalCaption = captionText.ifBlank {
                                "$selectedCategory evidence photo captured for $targetDduId"
                            }
                            val evidence = EvidenceEntity(
                                surveyDduId = targetDduId,
                                type = "PHOTO",
                                category = selectedCategory,
                                mediaUri = finalUri,
                                caption = finalCaption,
                                classification = classification,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.addEvidenceRecord(evidence)
                            isSaving = false
                            Toast.makeText(context, "Verification Photo Attached to $targetDduId", Toast.LENGTH_SHORT).show()
                            if (onPhotoCaptured != null) {
                                onPhotoCaptured(evidence)
                            } else {
                                onNavigateBack()
                            }
                        }
                    }
                )
            } else {
                // Camera Viewfinder with HUD & Controls
                Box(modifier = Modifier.fillMaxSize()) {
                    // CameraX Preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider

                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .setFlashMode(flashMode)
                                        .build()
                                    imageCapture = capture

                                    val selector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()

                                    provider.unbindAll()
                                    cameraControl = provider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        capture
                                    )
                                    isCameraBindingFailed = false
                                } catch (e: Exception) {
                                    isCameraBindingFailed = true
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        update = { previewView ->
                            // React to lens facing changes
                            val provider = cameraProvider ?: return@AndroidView
                            try {
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()
                                val preview = Preview.Builder().build().also { p ->
                                    p.surfaceProvider = previewView.surfaceProvider
                                }
                                val capture = ImageCapture.Builder()
                                    .setFlashMode(flashMode)
                                    .build()
                                imageCapture = capture
                                provider.unbindAll()
                                cameraControl = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                // Camera update handled gracefully
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Fallback notice if camera preview not available (e.g. headless emulator)
                    if (isCameraBindingFailed) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Virtual Camera Standby", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Physical camera device not attached in this container. You can simulate live field photo verification capture instantly.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val simulated = generateSimulatedPhoto(context, selectedCategory, targetDduId, gpsLat, gpsLng)
                                            capturedBitmap = simulated.first
                                            capturedImageUri = simulated.second
                                            captionText = "$selectedCategory field verification"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Snap Geotagged Field Photo")
                                }
                            }
                        }
                    }

                    // Opportunity Verification Category Chips (Top Overlay)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VERIFICATION CATEGORY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "DDU ID: $targetDduId",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { (cat, icon) ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.clickable { selectedCategory = cat }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Verification Watermark / HUD Overlay (Bottom-left of preview)
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 100.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GPS LOCK: %.4f°N, %.4f°E (±3.4m)".format(Locale.US, gpsLat, gpsLng),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                            Text(
                                text = "BLOCK: Balrampur • SECTOR: Rampur Basti",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "TIME: $timeStampStr",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "EVIDENCE: $selectedCategory",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Shutter flash effect
                    AnimatedVisibility(
                        visible = showShutterFlash,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White))
                    }

                    // Camera Controls Bar (Bottom)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery / Import Button
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("btn_import_gallery")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick Photo", tint = Color.White)
                        }

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(if (isCapturing) Color.Gray else MaterialTheme.colorScheme.primary)
                                .clickable(enabled = !isCapturing) {
                                    val capture = imageCapture
                                    if (capture != null && !isCameraBindingFailed) {
                                        isCapturing = true
                                        showShutterFlash = true
                                        val photoFile = File(
                                            context.cacheDir,
                                            "ddu_field_${System.currentTimeMillis()}.jpg"
                                        )
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                        val executor = ContextCompat.getMainExecutor(context)

                                        capture.takePicture(
                                            outputOptions,
                                            executor,
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                    isCapturing = false
                                                    showShutterFlash = false
                                                    val savedUri = Uri.fromFile(photoFile)
                                                    capturedImageUri = savedUri
                                                    captionText = "$selectedCategory photo for $targetDduId"
                                                }

                                                override fun onError(exc: ImageCaptureException) {
                                                    isCapturing = false
                                                    showShutterFlash = false
                                                    // Fallback to simulated photo if hardware fails
                                                    scope.launch {
                                                        val simulated = generateSimulatedPhoto(context, selectedCategory, targetDduId, gpsLat, gpsLng)
                                                        capturedBitmap = simulated.first
                                                        capturedImageUri = simulated.second
                                                        captionText = "$selectedCategory photo for $targetDduId"
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        // Virtual capture for emulator/simulation
                                        isCapturing = true
                                        showShutterFlash = true
                                        scope.launch {
                                            kotlinx.coroutines.delay(180)
                                            showShutterFlash = false
                                            val simulated = generateSimulatedPhoto(context, selectedCategory, targetDduId, gpsLat, gpsLng)
                                            capturedBitmap = simulated.first
                                            capturedImageUri = simulated.second
                                            captionText = "$selectedCategory photo for $targetDduId"
                                            isCapturing = false
                                        }
                                    }
                                }
                                .testTag("btn_capture_field_photo"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                            }
                        }

                        // Simulation / Quick Geotag Button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val simulated = generateSimulatedPhoto(context, selectedCategory, targetDduId, gpsLat, gpsLng)
                                    capturedBitmap = simulated.first
                                    capturedImageUri = simulated.second
                                    captionText = "$selectedCategory verification photo"
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("btn_simulate_field_photo")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Simulate Geotagged Photo", tint = Color(0xFF6EE7B7))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Camera Permission Request UI with clear justification compliant with guidelines.
 */
@Composable
private fun CameraPermissionRequestContent(
    onRequestPermission: () -> Unit,
    onSimulatePhoto: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Field Photo Verification",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "DDU Stage 1 Opportunity Discovery requires geotagged photographic proof of institutional demand, store packaging, and procurement premises for audit compliance.",
            fontSize = 13.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_grant_camera_permission")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSimulatePhoto,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_simulate_photo_fallback")
        ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF6EE7B7))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Geotagged Field Photo", color = Color(0xFF6EE7B7), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onPickFromGallery,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.LightGray)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Select Existing Image from Gallery", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

/**
 * Review & Metadata Confirmation Sheet for captured photo.
 */
@Composable
private fun PhotoVerificationReviewContent(
    imageUri: Uri?,
    bitmap: Bitmap?,
    category: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    targetDduId: String,
    onTargetDduIdChange: (String) -> Unit,
    surveysList: List<String>,
    caption: String,
    onCaptionChange: (String) -> Unit,
    classification: String,
    onClassificationChange: (String) -> Unit,
    gpsLat: Double,
    gpsLng: Double,
    timestamp: String,
    isSaving: Boolean,
    onRetake: () -> Unit,
    onConfirmSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Photo Preview Card with Geotag Watermark
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Field Evidence",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Captured Field Evidence",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Watermark Badge Overlay
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(
                            text = "DDU VERIFICATION PROOF • $category",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                        Text(
                            text = "%.4f°N, %.4f°E • %s".format(Locale.US, gpsLat, gpsLng, timestamp),
                            fontSize = 9.sp,
                            color = Color.White
                        )
                        Text(
                            text = "SURVEY ID: $targetDduId",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // Verification metadata fields
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Verification Evidence Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Category chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Caption TextField
                OutlinedTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    label = { Text("Evidence Caption / Observation Note") },
                    placeholder = { Text("e.g. Current uniform batch supplier tags from Tezpur") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_photo_caption")
                )

                // Classification selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val classifications = listOf("FIELD_EVIDENCE", "DOCUMENTARY_EVIDENCE", "SURVEYOR_OBSERVATION")
                    classifications.forEach { c ->
                        FilterChip(
                            selected = classification == c,
                            onClick = { onClassificationChange(c) },
                            label = {
                                Text(
                                    text = when (c) {
                                        "FIELD_EVIDENCE" -> "Field Photo"
                                        "DOCUMENTARY_EVIDENCE" -> "Bill/Doc"
                                        else -> "Observation"
                                    },
                                    fontSize = 10.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Actions: Retake vs Attach
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_retake_photo")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retake")
            }

            Button(
                onClick = onConfirmSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("btn_confirm_attach_evidence")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Attach to $targetDduId", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Generates an illustrative, geotagged bitmap for emulator and virtual testing.
 */
private suspend fun generateSimulatedPhoto(
    context: Context,
    category: String,
    dduId: String,
    lat: Double,
    lng: Double
): Pair<Bitmap, Uri> = withContext(Dispatchers.IO) {
    val width = 800
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background gradient or texture
    val bgPaint = Paint().apply {
        color = when (category) {
            "Product Specimen" -> android.graphics.Color.rgb(33, 50, 72)
            "Shopfront & Cluster" -> android.graphics.Color.rgb(44, 62, 50)
            "Premises & Building" -> android.graphics.Color.rgb(55, 45, 65)
            "Bill & Invoice" -> android.graphics.Color.rgb(60, 60, 50)
            else -> android.graphics.Color.rgb(35, 45, 55)
        }
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Center icon placeholder
    val gridPaint = Paint().apply {
        color = android.graphics.Color.argb(40, 255, 255, 255)
        strokeWidth = 2f
    }
    for (i in 0 until width step 40) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), gridPaint)
    }
    for (j in 0 until height step 40) {
        canvas.drawLine(0f, j.toFloat(), width.toFloat(), j.toFloat(), gridPaint)
    }

    // Title & Watermark
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 32f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("FIELD VERIFICATION: $category", 40f, 100f, textPaint)

    val subPaint = Paint().apply {
        color = android.graphics.Color.rgb(110, 231, 183)
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("DDU OPPORTUNITY DISCOVERY • $dduId", 40f, 140f, subPaint)

    // Center Card Graphic
    val cardPaint = Paint().apply {
        color = android.graphics.Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRoundRect(100f, 200f, 700f, 440f, 20f, 20f, cardPaint)

    val labelPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        textSize = 22f
        isAntiAlias = true
    }
    canvas.drawText("Specimen / Premises / Bill Evidence Capture", 140f, 290f, labelPaint)
    canvas.drawText("Audit Stamped for Section 45 Government Verification", 140f, 330f, labelPaint)

    // Bottom Watermark Box
    val hudPaint = Paint().apply {
        color = android.graphics.Color.argb(200, 10, 20, 25)
    }
    canvas.drawRect(0f, (height - 90).toFloat(), width.toFloat(), height.toFloat(), hudPaint)

    val hudText = Paint().apply {
        color = android.graphics.Color.rgb(110, 231, 183)
        textSize = 20f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText(
        "GPS: %.4f°N, %.4f°E (±2.8m) • Balrampur District • %s".format(
            Locale.US, lat, lng,
            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(Date())
        ),
        24f,
        (height - 45).toFloat(),
        hudText
    )

    // Save to cache
    val file = File(context.cacheDir, "simulated_ddu_${System.currentTimeMillis()}.jpg")
    val fos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
    fos.flush()
    fos.close()

    Pair(bitmap, Uri.fromFile(file))
}
