package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.FieldIntelligenceSummaryCard
import com.example.ui.theme.*
import com.example.viewmodel.FieldIntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyWizardScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    onSurveySubmitted: () -> Unit,
    onNavigateToRecordDetail: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Modes: EXPRESS (Fast 1-page log) or GUIDED (5-step in-depth audit)
    var surveyMode by remember { mutableStateOf("EXPRESS") }
    var selectedType by remember { mutableStateOf(SurveyType.INSTITUTION) }
    var currentStep by remember { mutableStateOf(1) } // 1 to 5 for guided mode

    // Fields
    var village by remember { mutableStateOf("Rampur Tola") }
    var tola by remember { mutableStateOf("Main Basti") }
    var block by remember { mutableStateOf("Balrampur") }
    var district by remember { mutableStateOf("Balrampur") }

    var isGpsCaptured by remember { mutableStateOf(true) }
    var gpsLat by remember { mutableStateOf(27.4312) }
    var gpsLng by remember { mutableStateOf(82.1892) }
    var gpsAccuracy by remember { mutableStateOf(3.8f) }

    val currentDateStr = remember {
        try {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        } catch (e: Exception) {
            "23 Sep 2026"
        }
    }
    val currentTimeStr = remember {
        try {
            SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        } catch (e: Exception) {
            "10:00 AM"
        }
    }

    var entityName by remember { mutableStateOf("Balrampur Model School") }
    var entityType by remember { mutableStateOf("School") }
    var contactPerson by remember { mutableStateOf("Principal Deben Kalita") }
    var contactNumber by remember { mutableStateOf("+91 9435887766") }

    // Demand fields for Express mode
    var expressProductName by remember { mutableStateOf("School Uniforms") }
    var expressCategory by remember { mutableStateOf("Textile") }
    var expressQty by remember { mutableStateOf("250") }
    var expressUnit by remember { mutableStateOf("Set") }
    var expressPrice by remember { mutableStateOf("380") }
    var expressFrequency by remember { mutableStateOf("Quarterly") }
    var expressSupplier by remember { mutableStateOf("City Dress Centre (Outside Block)") }
    var expressSource by remember { mutableStateOf("Outside Block") }

    // Products & Evidence for Guided mode
    var productsList by remember { mutableStateOf(listOf<ProductEntity>()) }
    var evidenceList by remember { mutableStateOf(listOf<EvidenceEntity>()) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // Success confirmation dialog state
    var submittedDduId by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    fun submitSurvey(isDraft: Boolean) {
        val resolvedProducts = if (surveyMode == "EXPRESS") {
            listOf(
                ProductEntity(
                    surveyDduId = "",
                    productName = expressProductName.ifBlank { "General Supplies" },
                    category = expressCategory,
                    brand = "Regional",
                    unit = expressUnit,
                    minQuantity = expressQty.toDoubleOrNull() ?: 100.0,
                    maxQuantity = (expressQty.toDoubleOrNull() ?: 100.0) * 1.5,
                    buyingPrice = expressPrice.toDoubleOrNull() ?: 200.0,
                    buyingFrequency = expressFrequency,
                    currentSupplier = expressSupplier.ifBlank { "External Distributor" },
                    supplierContact = "+91 9435000000",
                    currentSource = expressSource,
                    potentialOpportunity = "Identified DDU local enterprise opportunity replacement"
                )
            )
        } else {
            productsList
        }

        viewModel.submitNewSurvey(
            surveyType = selectedType,
            entityName = entityName.ifBlank { "${selectedType.displayName} Record" },
            entityType = entityType,
            contactPerson = contactPerson,
            contactNumber = contactNumber,
            village = village,
            tola = tola,
            block = block,
            district = district,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            gpsAccuracy = gpsAccuracy,
            gpsConfirmed = isGpsCaptured,
            productsList = resolvedProducts,
            evidenceList = evidenceList,
            isDraft = isDraft,
            onSuccess = { newId ->
                submittedDduId = newId
                showSuccessDialog = true
                Toast.makeText(context, "Survey Logged: $newId", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (surveyMode == "EXPRESS") "Quick Survey Entry" else "Guided Discovery Survey",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedType.displayName} • ${village.ifBlank { "Balrampur" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("survey_wizard_close")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { submitSurvey(isDraft = true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("save_draft_button")
                    ) {
                        Text(
                            text = "Save Draft",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Mode Switcher Banner (Express vs Guided)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isExpress = surveyMode == "EXPRESS"
                    Surface(
                        color = if (isExpress) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { surveyMode = "EXPRESS" }
                            .testTag("mode_express_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isExpress) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Express Quick Log",
                                fontSize = 12.sp,
                                fontWeight = if (isExpress) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpress) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val isGuided = surveyMode == "GUIDED"
                    Surface(
                        color = if (isGuided) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { surveyMode = "GUIDED" }
                            .testTag("mode_guided_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = null,
                                tint = if (isGuided) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "5-Step Deep Audit",
                                fontSize = 12.sp,
                                fontWeight = if (isGuided) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGuided) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Survey Type Selection Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SurveyType.entries) { type ->
                    val isSelected = type == selectedType
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = type
                            when (type) {
                                SurveyType.INSTITUTION -> {
                                    entityName = "Model High School"
                                    entityType = "School"
                                    expressProductName = "School Uniforms"
                                    expressQty = "250"
                                    expressPrice = "380"
                                }
                                SurveyType.LOCAL_SHOP -> {
                                    entityName = "Maa Tara General Store"
                                    entityType = "Grocery"
                                    expressProductName = "Packaged Breads & Buns"
                                    expressQty = "400"
                                    expressPrice = "28"
                                }
                                SurveyType.SUPPLIER -> {
                                    entityName = "Tezpur Uniform Dist"
                                    entityType = "Wholesaler"
                                    expressProductName = "School Uniform Fabric"
                                    expressQty = "1000"
                                    expressPrice = "120"
                                }
                                SurveyType.CUSTOMER_LEAD -> {
                                    entityName = "Panchayat Midday Meal Centre"
                                    entityType = "Govt Institution"
                                    expressProductName = "Fresh Brown Eggs"
                                    expressQty = "1500"
                                    expressPrice = "6.5"
                                }
                                SurveyType.FIELD_OBSERVATION -> {
                                    entityName = "Village Haat Tailoring Lead"
                                    entityType = "NGO"
                                    expressProductName = "Ladies Petticoats & Nighties"
                                    expressQty = "150"
                                    expressPrice = "190"
                                }
                            }
                        },
                        label = {
                            Text(
                                text = type.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("survey_type_chip_${type.name}")
                    )
                }
            }

            if (surveyMode == "EXPRESS") {
                // Express Mode: Fast single-page form with zero friction
                ExpressFormContent(
                    selectedType = selectedType,
                    entityName = entityName,
                    onEntityNameChange = { entityName = it },
                    entityType = entityType,
                    onEntityTypeChange = { entityType = it },
                    village = village,
                    onVillageChange = { village = it },
                    tola = tola,
                    onTolaChange = { tola = it },
                    contactPerson = contactPerson,
                    onContactPersonChange = { contactPerson = it },
                    contactNumber = contactNumber,
                    onContactNumberChange = { contactNumber = it },
                    productName = expressProductName,
                    onProductNameChange = { expressProductName = it },
                    quantity = expressQty,
                    onQuantityChange = { expressQty = it },
                    unit = expressUnit,
                    onUnitChange = { expressUnit = it },
                    price = expressPrice,
                    onPriceChange = { expressPrice = it },
                    frequency = expressFrequency,
                    onFrequencyChange = { expressFrequency = it },
                    supplier = expressSupplier,
                    onSupplierChange = { expressSupplier = it },
                    sourcing = expressSource,
                    onSourcingChange = { expressSource = it },
                    isGpsCaptured = isGpsCaptured,
                    gpsLat = gpsLat,
                    gpsLng = gpsLng,
                    onCaptureGps = {
                        isGpsCaptured = true
                        gpsLat = 27.4312 + (Math.random() - 0.5) * 0.01
                        gpsLng = 82.1892 + (Math.random() - 0.5) * 0.01
                    },
                    onSubmit = { submitSurvey(isDraft = false) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Guided 5-Step Mode
                Column(modifier = Modifier.weight(1f)) {
                    StepperProgressRow(
                        currentStep = currentStep,
                        onStepClick = { step -> currentStep = step }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentStep) {
                            1 -> Step1Location(
                                village = village,
                                onVillageChange = { village = it },
                                tola = tola,
                                onTolaChange = { tola = it },
                                block = block,
                                onBlockChange = { block = it },
                                district = district,
                                onDistrictChange = { district = it },
                                isGpsCaptured = isGpsCaptured,
                                gpsLat = gpsLat,
                                gpsLng = gpsLng,
                                gpsAccuracy = gpsAccuracy,
                                onCaptureGps = {
                                    isGpsCaptured = true
                                    gpsLat = 27.4312 + (Math.random() - 0.5) * 0.005
                                    gpsLng = 82.1892 + (Math.random() - 0.5) * 0.005
                                    gpsAccuracy = 3.6f
                                },
                                currentDate = currentDateStr,
                                currentTime = currentTimeStr,
                                onNext = { currentStep = 2 }
                            )

                            2 -> Step2EntityInfo(
                                surveyType = selectedType,
                                entityName = entityName,
                                onEntityNameChange = { entityName = it },
                                entityType = entityType,
                                onEntityTypeChange = { entityType = it },
                                contactPerson = contactPerson,
                                onContactPersonChange = { contactPerson = it },
                                contactNumber = contactNumber,
                                onContactNumberChange = { contactNumber = it },
                                onNext = { currentStep = 3 },
                                onBack = { currentStep = 1 }
                            )

                            3 -> Step3ProductsDemand(
                                products = productsList,
                                onAddProductClick = { showAddProductDialog = true },
                                onDeleteProduct = { product ->
                                    productsList = productsList.filter { it != product }
                                },
                                onNext = { currentStep = 4 },
                                onBack = { currentStep = 2 }
                            )

                            4 -> Step4EvidenceCapture(
                                evidenceList = evidenceList,
                                onAddPhoto = { category, caption ->
                                    evidenceList = evidenceList + EvidenceEntity(
                                        surveyDduId = "",
                                        type = "PHOTO",
                                        category = category,
                                        mediaUri = "res://photo_field_${System.currentTimeMillis()}",
                                        caption = caption.ifBlank { "Field photograph for $category" },
                                        classification = "FIELD_EVIDENCE"
                                    )
                                },
                                onRecordVoice = { caption, transcript ->
                                    evidenceList = evidenceList + EvidenceEntity(
                                        surveyDduId = "",
                                        type = "VOICE_NOTE",
                                        category = "Institution",
                                        mediaUri = "res://voice_field_${System.currentTimeMillis()}",
                                        caption = caption,
                                        transcription = transcript,
                                        durationSeconds = 48,
                                        classification = "RESPONDENT_INFORMATION"
                                    )
                                },
                                onDeleteEvidence = { ev ->
                                    evidenceList = evidenceList.filter { it != ev }
                                },
                                onNext = { currentStep = 5 },
                                onBack = { currentStep = 3 }
                            )

                            5 -> Step5ReviewSubmit(
                                surveyType = selectedType,
                                entityName = entityName,
                                entityType = entityType,
                                village = village,
                                tola = tola,
                                block = block,
                                isGpsCaptured = isGpsCaptured,
                                products = productsList,
                                evidence = evidenceList,
                                onBack = { currentStep = 4 },
                                onSubmit = { submitSurvey(isDraft = false) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onProductAdded = { newProduct ->
                productsList = productsList + newProduct
                showAddProductDialog = false
            }
        )
    }

    // Success Confirmation Dialog
    if (showSuccessDialog && submittedDduId != null) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onSurveySubmitted()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Survey Logged Successfully", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Field record has been saved to offline storage and queued for synchronization.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "DDU ID: ${submittedDduId ?: "Pending"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Entity: ${entityName.ifBlank { "Survey Record" }} ($village)",
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Demand: $expressProductName ($expressQty $expressUnit)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dduId = submittedDduId
                        showSuccessDialog = false
                        if (onNavigateToRecordDetail != null && dduId != null) {
                            onNavigateToRecordDetail(dduId)
                        } else {
                            onSurveySubmitted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("dialog_view_record_button")
                ) {
                    Text("View Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        onSurveySubmitted()
                    },
                    modifier = Modifier.testTag("dialog_done_button")
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun ExpressFormContent(
    selectedType: SurveyType,
    entityName: String,
    onEntityNameChange: (String) -> Unit,
    entityType: String,
    onEntityTypeChange: (String) -> Unit,
    village: String,
    onVillageChange: (String) -> Unit,
    tola: String,
    onTolaChange: (String) -> Unit,
    contactPerson: String,
    onContactPersonChange: (String) -> Unit,
    contactNumber: String,
    onContactNumberChange: (String) -> Unit,
    productName: String,
    onProductNameChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    supplier: String,
    onSupplierChange: (String) -> Unit,
    sourcing: String,
    onSourcingChange: (String) -> Unit,
    isGpsCaptured: Boolean,
    gpsLat: Double,
    gpsLng: Double,
    onCaptureGps: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Presets
        item {
            Text(
                text = "Quick Presets:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    "Balrampur Govt High" to "School Uniforms",
                    "CHC Hospital" to "Hospital Linen",
                    "Maa Tara Kirana" to "Packaged Breads",
                    "Grazi Basti Dhaba" to "Cleaning Phynyl",
                    "Juri Poultry Lead" to "Brown Eggs"
                )
                items(presets) { (presetEntity, presetProd) ->
                    AssistChip(
                        onClick = {
                            onEntityNameChange(presetEntity)
                            onProductNameChange(presetProd)
                        },
                        label = { Text("$presetEntity ($presetProd)", fontSize = 10.sp) }
                    )
                }
            }
        }

        // Section 1: Entity & Location
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("1. Entity & Administrative Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = entityName,
                        onValueChange = onEntityNameChange,
                        label = { Text("${selectedType.displayName} Name *") },
                        placeholder = { Text("e.g. Balrampur Govt School") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("express_entity_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = village,
                            onValueChange = onVillageChange,
                            label = { Text("Village *") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("express_village")
                        )
                        OutlinedTextField(
                            value = tola,
                            onValueChange = onTolaChange,
                            label = { Text("Tola / Locality") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = onContactPersonChange,
                            label = { Text("Contact Person") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = onContactNumberChange,
                            label = { Text("Phone Number") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: Demand & Product Requirements
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("2. Product Demand & Sourcing Gap", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = productName,
                        onValueChange = onProductNameChange,
                        label = { Text("Product / Requirement *") },
                        placeholder = { Text("e.g. School Uniforms, Hospital Linen") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("express_product_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = onQuantityChange,
                            label = { Text("Volume / Qty") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("express_qty")
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = onUnitChange,
                            label = { Text("Unit") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = onPriceChange,
                            label = { Text("Rate (₹)") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("express_price")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = supplier,
                        onValueChange = onSupplierChange,
                        label = { Text("Current Supplier & Origin *") },
                        placeholder = { Text("e.g. Tezpur Garment Wholesaler (Outside Block)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("express_supplier")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val sourcingOptions = listOf("Outside Block", "Outside District", "Local Village")
                        sourcingOptions.forEach { opt ->
                            FilterChip(
                                selected = sourcing == opt,
                                onClick = { onSourcingChange(opt) },
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(opt, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: GPS Coordinates & Verification
        item {
            val isDark = MaterialTheme.colorScheme.background == DduBackgroundDark
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFECFDF5)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF047857) else Color(0xFFA7F3D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF059669) else Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GPS Location Confirmed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                            )
                            Text(
                                text = String.format(Locale.US, "%.4f°N, %.4f°E (±3.8m)", gpsLat, gpsLng),
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onCaptureGps,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("express_recapture_gps")
                    ) {
                        Text("Recapture", fontSize = 11.sp, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46))
                    }
                }
            }
        }

        // Section 4: Big Unmissable Log Survey Button
        item {
            Button(
                onClick = onSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("express_log_survey_button")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOG FIELD SURVEY NOW",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StepperProgressRow(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf("Location", "Entity", "Products", "Evidence", "Review")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isCurrent = stepNumber == currentStep
            val isCompleted = stepNumber < currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStepClick(stepNumber) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isCompleted -> Color(0xFF10B981)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Step1Location(
    village: String,
    onVillageChange: (String) -> Unit,
    tola: String,
    onTolaChange: (String) -> Unit,
    block: String,
    onBlockChange: (String) -> Unit,
    district: String,
    onDistrictChange: (String) -> Unit,
    isGpsCaptured: Boolean,
    gpsLat: Double,
    gpsLng: Double,
    gpsAccuracy: Float,
    onCaptureGps: () -> Unit,
    currentDate: String,
    currentTime: String,
    onNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Administrative Location", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = village,
                        onValueChange = onVillageChange,
                        label = { Text("Village *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_village")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tola,
                        onValueChange = onTolaChange,
                        label = { Text("Tola / Locality") },
                        modifier = Modifier.fillMaxWidth().testTag("input_tola")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = block, onValueChange = onBlockChange, label = { Text("Block *") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = district, onValueChange = onDistrictChange, label = { Text("District *") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("GPS Coordinates", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isGpsCaptured) "GPS Captured (Confirmed)" else "GPS Pending",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isGpsCaptured) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                            Text(
                                text = String.format(Locale.US, "%.4f°N, %.4f°E (±%.1fm)", gpsLat, gpsLng, gpsAccuracy),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onCaptureGps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGpsCaptured) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isGpsCaptured) "Recapture" else "Capture", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("step1_next_button")
            ) {
                Text("Next: Entity Info", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Step2EntityInfo(
    surveyType: SurveyType,
    entityName: String,
    onEntityNameChange: (String) -> Unit,
    entityType: String,
    onEntityTypeChange: (String) -> Unit,
    contactPerson: String,
    onContactPersonChange: (String) -> Unit,
    contactNumber: String,
    onContactNumberChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val types = listOf("School", "Hospital", "Hostel", "Dhaba", "Grocery", "Wholesaler", "NGO", "Other")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("${surveyType.displayName} Details", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = entityName,
                        onValueChange = onEntityNameChange,
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_entity_name")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Category / Type:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(types) { t ->
                            FilterChip(
                                selected = entityType == t,
                                onClick = { onEntityTypeChange(t) },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = onContactPersonChange,
                        label = { Text("Contact Person") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = onContactNumberChange,
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp)) { Text("Back") }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.5f).height(48.dp)
                ) {
                    Text("Next: Products Demand", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Step3ProductsDemand(
    products: List<ProductEntity>,
    onAddProductClick: () -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Product Demands (${products.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddProductClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Demand", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No specific product added yet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "A default product demand will automatically be mapped if skipped.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(products) { prod ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(prod.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${prod.minQuantity.toInt()} ${prod.unit} • ₹${prod.buyingPrice.toInt()}/unit • ${prod.currentSupplier}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteProduct(prod) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp)) { Text("Back") }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.5f).height(48.dp)
                ) {
                    Text("Next: Evidence", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Step4EvidenceCapture(
    evidenceList: List<EvidenceEntity>,
    onAddPhoto: (String, String) -> Unit,
    onRecordVoice: (String, String) -> Unit,
    onDeleteEvidence: (EvidenceEntity) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Attached Evidence (${evidenceList.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        // AI Voice Transcription Assistant Box
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Voice-to-Note: Record conversational field interviews with teachers, buyers or entrepreneurs. Gemini transcribes and summaries automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAddPhoto("Premises", "Photograph of school building and procurement gate") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Snap Photo", fontSize = 11.sp)
                }
                Button(
                    onClick = { onRecordVoice("Voice Interview", "Principal states uniforms are procured from Tezpur due to no local stitching cluster.") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Voice", fontSize = 11.sp)
                }
            }
        }

        items(evidenceList) { ev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(ev.caption, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${ev.type} • ${ev.classification}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDeleteEvidence(ev) }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp)) { Text("Back") }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.5f).height(48.dp)
                ) {
                    Text("Next: Review & Submit", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Step5ReviewSubmit(
    surveyType: SurveyType,
    entityName: String,
    entityType: String,
    village: String,
    tola: String,
    block: String,
    isGpsCaptured: Boolean,
    products: List<ProductEntity>,
    evidence: List<EvidenceEntity>,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val completeness = remember(entityName, products, evidence, isGpsCaptured) {
        var score = 0
        if (entityName.isNotBlank()) score += 30
        if (village.isNotBlank()) score += 15
        if (isGpsCaptured) score += 20
        if (products.isNotEmpty()) score += 25
        if (evidence.isNotEmpty()) score += 10
        score.coerceIn(40, 100)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Review & Final Submission", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quality & Completeness", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("$completeness%", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { completeness / 100f },
                        color = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        item {
            FieldIntelligenceSummaryCard(
                summaryText = "$entityName in $village was surveyed for institutional procurement. Data indicates recurring demand sourced outside block with high DDU opportunity linkage."
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                Button(
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.5f).height(50.dp).testTag("step5_submit_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit Record", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onProductAdded: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf("School Uniforms") }
    var qty by remember { mutableStateOf("200") }
    var unit by remember { mutableStateOf("Set") }
    var price by remember { mutableStateOf("380") }
    var supplier by remember { mutableStateOf("City Dress Centre (Outside Block)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product Demand", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Rate (₹)") })
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onProductAdded(
                        ProductEntity(
                            surveyDduId = "",
                            productName = name,
                            category = "General",
                            brand = "Regional",
                            unit = unit,
                            minQuantity = qty.toDoubleOrNull() ?: 100.0,
                            maxQuantity = (qty.toDoubleOrNull() ?: 100.0) * 1.5,
                            buyingPrice = price.toDoubleOrNull() ?: 200.0,
                            buyingFrequency = "Monthly",
                            currentSupplier = supplier,
                            supplierContact = "",
                            currentSource = "Outside Block"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
