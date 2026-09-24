package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OpportunityEntity
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.viewmodel.FieldIntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stage2AssessmentFormScreen(
    viewModel: FieldIntelligenceViewModel,
    assessmentId: String? = null,
    oppId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDduSelection: () -> Unit
) {
    val context = LocalContext.current
    val opportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val existingAssessments by viewModel.allStage2Assessments.collectAsStateWithLifecycle()

    // Determine initial assessment or opportunity to link
    val existingAssessment = remember(assessmentId, existingAssessments) {
        existingAssessments.firstOrNull { it.assessmentId == assessmentId }
    }
    val linkedOpp = remember(oppId, opportunities) {
        opportunities.firstOrNull { it.oppId == oppId }
    }

    // Header State
    var targetProductName by remember {
        mutableStateOf(existingAssessment?.productName ?: linkedOpp?.title ?: "School Uniforms")
    }
    var vatika by remember {
        mutableStateOf(existingAssessment?.vatika ?: "Balrampur Central")
    }
    var filledBy by remember {
        mutableStateOf(existingAssessment?.filledBy ?: "Field Fellow / DIC Team")
    }
    var assessmentDate by remember {
        mutableStateOf(existingAssessment?.assessmentDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var selectedOppId by remember {
        mutableStateOf(existingAssessment?.stage1OppId ?: linkedOpp?.oppId ?: "")
    }

    // Part A — Basic Information
    var demandFromLocalSurvey by remember {
        mutableStateOf(existingAssessment?.demandFromLocalSurvey ?: true)
    }
    var mainRawMaterialNeeded by remember {
        mutableStateOf(existingAssessment?.mainRawMaterialNeeded ?: "Poly-cotton uniform fabric, thread, buttons, canvas")
    }
    var rawMaterialAvailableLocally by remember {
        mutableStateOf(existingAssessment?.rawMaterialAvailableLocally ?: true)
    }
    var trainingNeededForQuality by remember {
        mutableStateOf(existingAssessment?.trainingNeededForQuality ?: true)
    }
    var trainingKindNeeded by remember {
        mutableStateOf(existingAssessment?.trainingKindNeeded ?: "Standardized pattern drafting & industrial machine stitching")
    }
    var trainedWomenAvailable by remember {
        mutableStateOf(existingAssessment?.trainedWomenAvailable ?: true)
    }
    var externalSupportNeeded by remember {
        mutableStateOf(existingAssessment?.externalSupportNeeded ?: "Master cutter trainer from Tezpur, sizing charts, SWSM branding")
    }

    // Part B — Priority Level (1 to 6)
    var selectedPriorityLevel by remember {
        mutableIntStateOf(existingAssessment?.priorityLevel ?: 1)
    }

    // Part C — Critical (Mandatory)
    var packagingLocallyAvailable by remember {
        mutableStateOf(existingAssessment?.packagingLocallyAvailable ?: true)
    }
    var canBeProducedInVillage by remember {
        mutableStateOf(existingAssessment?.canBeProducedInVillage ?: true)
    }
    val criticalProceedNext = packagingLocallyAvailable && canBeProducedInVillage

    // Part D — Sample Availability Check
    var isSampleAvailable by remember {
        mutableStateOf(existingAssessment?.isSampleAvailable ?: false)
    }

    // Part E — Readiness Check (7 factors)
    var rawMaterialCostStability by remember { mutableIntStateOf(existingAssessment?.rawMaterialCostStability ?: 1) }
    var trainerAvailable by remember { mutableIntStateOf(existingAssessment?.trainerAvailable ?: 1) }
    var productionOrRawMaterialAvailable by remember { mutableIntStateOf(existingAssessment?.productionOrRawMaterialAvailable ?: 1) }
    var workspaceAvailable by remember { mutableIntStateOf(existingAssessment?.workspaceAvailable ?: 1) }
    var electricityAvailable by remember { mutableIntStateOf(existingAssessment?.electricityAvailable ?: 1) }
    var waterAvailable by remember { mutableIntStateOf(existingAssessment?.waterAvailable ?: 1) }
    var storageAvailable by remember { mutableIntStateOf(existingAssessment?.storageAvailable ?: 1) }

    val readinessScore = rawMaterialCostStability + trainerAvailable +
            productionOrRawMaterialAvailable + workspaceAvailable +
            electricityAvailable + waterAvailable + storageAvailable

    // SWSM Decision & Notes
    var swsmDecisionNotes by remember {
        mutableStateOf(existingAssessment?.swsmDecisionNotes ?: "")
    }

    // Derived Decision
    val (computedDecision, _) = remember(criticalProceedNext, isSampleAvailable, readinessScore) {
        viewModel.calculateStage2Decision(criticalProceedNext, isSampleAvailable, readinessScore)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stage 2 · Village Assessment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Can our village produce this, competitively?",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Quick fill for rapid assessment
                            targetProductName = "School Uniforms"
                            mainRawMaterialNeeded = "Poly-cotton uniform fabric, thread, buttons"
                            rawMaterialAvailableLocally = true
                            trainingKindNeeded = "Pattern cutting & collar finishing"
                            trainedWomenAvailable = true
                            externalSupportNeeded = "Master cutter & SWSM branding tags"
                            selectedPriorityLevel = 1
                            packagingLocallyAvailable = true
                            canBeProducedInVillage = true
                            isSampleAvailable = true
                            rawMaterialCostStability = 1
                            trainerAvailable = 1
                            productionOrRawMaterialAvailable = 1
                            workspaceAvailable = 1
                            electricityAvailable = 1
                            waterAvailable = 1
                            storageAvailable = 1
                            swsmDecisionNotes = "Verified with Kasturba Gandhi Balika Vidyalaya requirements. Ready for Stage 3."
                            Toast.makeText(context, "Filled Stage 2 sample data", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Fill Sample", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Executive Header Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Factory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STAGE 2 PRODUCTION ASSESSMENT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (criticalProceedNext) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = if (criticalProceedNext) "CRITICAL: PASS" else "CRITICAL: FAIL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Work through Part A to Part E in order, then record the SWSM decision at the end.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Identification Section
            OutlinedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Product & Assessment Metadata",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = targetProductName,
                        onValueChange = { targetProductName = it },
                        label = { Text("Product Being Assessed *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stage2_product_name"),
                        singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = vatika,
                            onValueChange = { vatika = it },
                            label = { Text("Vatika / Cluster") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = filledBy,
                            onValueChange = { filledBy = it },
                            label = { Text("Filled By") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Part A — Basic Information
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Part A — Basic Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Q1: Demand from survey
                    BooleanQuestionRow(
                        question = "Whether Demand of product is there from Local Market Survey?",
                        value = demandFromLocalSurvey,
                        onValueChange = { demandFromLocalSurvey = it }
                    )

                    // Q2: Main raw material needed
                    OutlinedTextField(
                        value = mainRawMaterialNeeded,
                        onValueChange = { mainRawMaterialNeeded = it },
                        label = { Text("Main raw material needed") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Q3: Raw material available locally
                    BooleanQuestionRow(
                        question = "Whether Raw Material available (within Village & can be purchased from Local producer/Farmer)?",
                        value = rawMaterialAvailableLocally,
                        onValueChange = { rawMaterialAvailableLocally = it }
                    )

                    // Q4: Training needed for Quality
                    BooleanQuestionRow(
                        question = "Training needed for Quality?",
                        value = trainingNeededForQuality,
                        onValueChange = { trainingNeededForQuality = it }
                    )
                    if (trainingNeededForQuality) {
                        OutlinedTextField(
                            value = trainingKindNeeded,
                            onValueChange = { trainingKindNeeded = it },
                            label = { Text("What kind of quality training is needed?") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Q5: Trained women available
                    BooleanQuestionRow(
                        question = "Whether trained women / Vaibhavi are available?",
                        value = trainedWomenAvailable,
                        onValueChange = { trainedWomenAvailable = it }
                    )

                    // Q6: External support needed
                    OutlinedTextField(
                        value = externalSupportNeeded,
                        onValueChange = { externalSupportNeeded = it },
                        label = { Text("External support needed (trainer, branding, licence…)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Part B — Priority Level
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Part B — Priority Level",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Priority is set by where the raw material comes from and where the product will be sold. High priority products are sequenced first for Vatika growth.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val priorities = listOf(
                        Triple(1, "Purchased from Producer/Farmer", "Local"),
                        Triple(2, "Purchased from Local Market", "Local"),
                        Triple(3, "Outside of Local/nearest Market", "Local"),
                        Triple(4, "Purchased from Producer/Farmer", "Outside of Vatika/nearby market"),
                        Triple(5, "Purchased from Local Market", "Outside of Vatika/nearby market"),
                        Triple(6, "Outside of Local/nearest Market", "Outside of Vatika/nearby market")
                    )

                    priorities.forEach { (pLevel, rawMat, mkt) ->
                        val isSelected = selectedPriorityLevel == pLevel
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPriorityLevel = pLevel },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPriorityLevel = pLevel },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "Priority $pLevel",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (pLevel == 1) "Highest Priority" else if (pLevel == 6) "Lowest Priority" else "",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Raw Material: $rawMat",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Target Market: $mkt",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Part C — Critical (Mandatory)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (criticalProceedNext) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (criticalProceedNext) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Part C — Critical (Mandatory)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (criticalProceedNext) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                        )
                        Icon(
                            imageVector = if (criticalProceedNext) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (criticalProceedNext) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "All two must be Yes. A single No makes the product Not Suitable, regardless of the score in Part B.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    BooleanQuestionRow(
                        question = "1. Packaging locally available, or can be purchased by SWSM from outside/nearby market (within comfortable range)?",
                        value = packagingLocallyAvailable,
                        onValueChange = { packagingLocallyAvailable = it }
                    )

                    BooleanQuestionRow(
                        question = "2. Can this be produced in the village? (Check infrastructure / cultural acceptance / affordability / women producers available)",
                        value = canBeProducedInVillage,
                        onValueChange = { canBeProducedInVillage = it }
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (criticalProceedNext) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (criticalProceedNext) Icons.Default.ThumbUp else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (criticalProceedNext) "SELECTION: CRITICAL (ALL YES) -> PROCEED NEXT" else "SELECTION: DO NOT PROCEED (CRITICAL NO)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Part D — Sample Availability Check
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Part D — Sample Availability Check",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Whether Product is being produced locally & sample is available?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = isSampleAvailable,
                            onClick = { isSampleAvailable = true },
                            label = { Text("Yes (Sample Available — Score: 1)") },
                            leadingIcon = if (isSampleAvailable) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isSampleAvailable,
                            onClick = { isSampleAvailable = false },
                            label = { Text("No (Sample Not Ready — Score: 0)") },
                            leadingIcon = if (!isSampleAvailable) {
                                { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSampleAvailable) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSampleAvailable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSampleAvailable) Icons.Default.FastForward else Icons.Default.Rule,
                                contentDescription = null,
                                tint = if (isSampleAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSampleAvailable) {
                                    "Product Sample is ready! Proceed directly for Stage 3 DDU Designing (Readiness Ranking optional)."
                                } else {
                                    "Product Sample is not ready. Complete Readiness Check (Part E) to decide final ranking."
                                },
                                fontSize = 11.sp,
                                color = if (isSampleAvailable) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Part E — Readiness Check (Rating count out of 7)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Part E — Readiness Check",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Score each factor (0 or 1). Decides fully Suitable or Suitable after Support.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "$readinessScore / 7",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    ReadinessFactorItem(
                        title = "1. Raw material cost stability",
                        option0 = "0: A lot of fluctuation",
                        option1 = "1: Sometimes / Stable",
                        score = rawMaterialCostStability,
                        onScoreChanged = { rawMaterialCostStability = it }
                    )

                    ReadinessFactorItem(
                        title = "2. Whether Trainer available?",
                        option0 = "0: No",
                        option1 = "1: Yes",
                        score = trainerAvailable,
                        onScoreChanged = { trainerAvailable = it }
                    )

                    ReadinessFactorItem(
                        title = "3. Whether Production / Raw Material is available presently?",
                        option0 = "0: No",
                        option1 = "1: Yes",
                        score = productionOrRawMaterialAvailable,
                        onScoreChanged = { productionOrRawMaterialAvailable = it }
                    )

                    ReadinessFactorItem(
                        title = "4. Workspace available, based on the product type",
                        option0 = "0: Not Available",
                        option1 = "1: Available",
                        score = workspaceAvailable,
                        onScoreChanged = { workspaceAvailable = it }
                    )

                    ReadinessFactorItem(
                        title = "5. Electricity required and available",
                        option0 = "0: Required & Not Available",
                        option1 = "1: Not Required / Available",
                        score = electricityAvailable,
                        onScoreChanged = { electricityAvailable = it }
                    )

                    ReadinessFactorItem(
                        title = "6. Water available, based on the product type",
                        option0 = "0: Required & Not Available",
                        option1 = "1: Not Required / Available",
                        score = waterAvailable,
                        onScoreChanged = { waterAvailable = it }
                    )

                    ReadinessFactorItem(
                        title = "7. Storage required and available",
                        option0 = "0: Required & Not Available",
                        option1 = "1: Not Required / Available",
                        score = storageAvailable,
                        onScoreChanged = { storageAvailable = it }
                    )
                }
            }

            // SWSM Decision & Action Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SWSM Assessment Decision",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Computed Selection Status:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = computedDecision,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!criticalProceedNext) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = swsmDecisionNotes,
                        onValueChange = { swsmDecisionNotes = it },
                        label = { Text("SWSM Decision Remarks & Support Notes") },
                        placeholder = { Text("e.g. Master cutter trainer needed from Tezpur; approved for Stage 3 DDU Designing.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Button(
                        onClick = {
                            if (targetProductName.isBlank()) {
                                Toast.makeText(context, "Please enter product name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val newAssessmentId = existingAssessment?.assessmentId ?: "VPA-${UUID.randomUUID().toString().take(8).uppercase()}"
                            val entity = VillageProductionAssessmentEntity(
                                assessmentId = newAssessmentId,
                                stage1OppId = selectedOppId,
                                productName = targetProductName,
                                vatika = vatika,
                                filledBy = filledBy,
                                assessmentDate = assessmentDate,
                                demandFromLocalSurvey = demandFromLocalSurvey,
                                mainRawMaterialNeeded = mainRawMaterialNeeded,
                                rawMaterialAvailableLocally = rawMaterialAvailableLocally,
                                trainingNeededForQuality = trainingNeededForQuality,
                                trainingKindNeeded = trainingKindNeeded,
                                trainedWomenAvailable = trainedWomenAvailable,
                                externalSupportNeeded = externalSupportNeeded,
                                priorityLevel = selectedPriorityLevel,
                                packagingLocallyAvailable = packagingLocallyAvailable,
                                canBeProducedInVillage = canBeProducedInVillage,
                                criticalProceedNext = criticalProceedNext,
                                isSampleAvailable = isSampleAvailable,
                                rawMaterialCostStability = rawMaterialCostStability,
                                trainerAvailable = trainerAvailable,
                                productionOrRawMaterialAvailable = productionOrRawMaterialAvailable,
                                workspaceAvailable = workspaceAvailable,
                                electricityAvailable = electricityAvailable,
                                waterAvailable = waterAvailable,
                                storageAvailable = storageAvailable,
                                readinessScore = readinessScore,
                                criticalityRatingPass = criticalProceedNext,
                                samplingChecked = isSampleAvailable,
                                finalRankingOrder = selectedPriorityLevel,
                                finalSelectionStatus = computedDecision,
                                swsmDecisionNotes = swsmDecisionNotes,
                                isSyncedToFirestore = false,
                                updatedTimestamp = System.currentTimeMillis()
                            )

                            viewModel.saveStage2Assessment(entity)
                            Toast.makeText(context, "Stage 2 Assessment saved! Synced to Firestore.", Toast.LENGTH_SHORT).show()
                            onNavigateToDduSelection()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_save_stage2_assessment")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Assessment & Sync Firestore", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToDduSelection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Final Product List for DDU Selection")
                    }
                }
            }
        }
    }
}

@Composable
private fun BooleanQuestionRow(
    question: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = question,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value,
                onClick = { onValueChange(true) },
                label = { Text("Yes", fontSize = 12.sp) },
                leadingIcon = if (value) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = !value,
                onClick = { onValueChange(false) },
                label = { Text("No", fontSize = 12.sp) },
                leadingIcon = if (!value) {
                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun ReadinessFactorItem(
    title: String,
    option0: String,
    option1: String,
    score: Int,
    onScoreChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = score == 1,
                onClick = { onScoreChanged(1) },
                label = { Text(option1, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE8F5E9),
                    selectedLabelColor = Color(0xFF1B5E20)
                )
            )
            FilterChip(
                selected = score == 0,
                onClick = { onScoreChanged(0) },
                label = { Text(option0, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    selectedLabelColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}
