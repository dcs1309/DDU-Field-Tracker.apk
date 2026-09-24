package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SakhyaScreeningEntity
import com.example.viewmodel.FieldIntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakhyaScreeningFormScreen(
    viewModel: FieldIntelligenceViewModel,
    screeningId: String? = null,
    dduId: String? = null,
    onNavigateBack: () -> Unit,
    onSavedSuccessfully: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allSurveysWithDetails by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()
    val allSakhyaScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()

    val existingScreening = remember(screeningId, allSakhyaScreenings) {
        if (!screeningId.isNullOrBlank()) {
            allSakhyaScreenings.find { it.screeningId == screeningId }
        } else null
    }

    // Form fields state
    var currentScreeningId by remember {
        mutableStateOf(existingScreening?.screeningId ?: "SKH-${System.currentTimeMillis().toString().takeLast(6)}")
    }
    var linkedDduId by remember {
        mutableStateOf(existingScreening?.linkedDduId ?: dduId ?: "")
    }

    // Primary Info
    var entrepreneurName by remember { mutableStateOf(existingScreening?.entrepreneurName ?: "") }
    var productAndValueChain by remember { mutableStateOf(existingScreening?.productAndValueChain ?: "") }

    // Section 1: Business Background
    var receivedProfessionalTraining by remember { mutableStateOf(existingScreening?.receivedProfessionalTraining ?: true) }
    var trainingDetails by remember { mutableStateOf(existingScreening?.trainingDetails ?: "") }
    var yearsInBusinessText by remember { mutableStateOf(existingScreening?.yearsInBusiness?.toString() ?: "4.0") }
    var certificateAvailable by remember { mutableStateOf(existingScreening?.certificateAvailable ?: true) }
    var priorWorkExperienceYearsText by remember { mutableStateOf(existingScreening?.priorWorkExperienceYears?.toString() ?: "1.0") }
    var howGotIntoBusiness by remember { mutableStateOf(existingScreening?.howGotIntoBusiness ?: "Self-explored") }
    var howGotIntoBusinessDescription by remember { mutableStateOf(existingScreening?.howGotIntoBusinessDescription ?: "") }

    // Section 2: Income & Profitability
    var timeToBreakEven by remember { mutableStateOf(existingScreening?.timeToBreakEven ?: "8 months") }
    var currentMonthlyProfitabilityText by remember { mutableStateOf(existingScreening?.currentMonthlyProfitability?.toInt()?.toString() ?: "4500") }
    var partnersProfitSharingCountText by remember { mutableStateOf(existingScreening?.partnersProfitSharingCount?.toString() ?: "2") }
    var profitSharePercentPerPartnerText by remember { mutableStateOf(existingScreening?.profitSharePercentPerPartner?.toInt()?.toString() ?: "50") }
    var membersCostToCostBasisCountText by remember { mutableStateOf(existingScreening?.membersCostToCostBasisCount?.toString() ?: "1") }
    var earningsPerCostToCostMemberMonthlyText by remember { mutableStateOf(existingScreening?.earningsPerCostToCostMemberMonthly?.toInt()?.toString() ?: "1200") }

    // Section 3: Market Exposure
    var dedicatedMarketingMember by remember { mutableStateOf(existingScreening?.dedicatedMarketingMember ?: "Self-managed") }
    var selectedMarketLevels by remember {
        mutableStateOf(
            existingScreening?.marketLevelsServed?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf("B2C — direct household sales", "SHG / group sales")
        )
    }
    var largestRevenueShareMarketLevel by remember {
        mutableStateOf(existingScreening?.largestRevenueShareMarketLevel ?: "B2C — direct household sales")
    }

    // Section 4: Infrastructure & Operations
    var infrastructureJourneyMilestones by remember { mutableStateOf(existingScreening?.infrastructureJourneyMilestones ?: "") }
    var isInfrastructureSufficient by remember { mutableStateOf(existingScreening?.isInfrastructureSufficient ?: "Sufficient — but upgrades planned") }
    var downtimeManagementAndImpact by remember { mutableStateOf(existingScreening?.downtimeManagementAndImpact ?: "") }

    // Section 5: Growth Plan & Input Management
    var expansionAspirations by remember { mutableStateOf(existingScreening?.expansionAspirations ?: "Yes — market demand driven") }
    var expansionDetails by remember { mutableStateOf(existingScreening?.expansionDetails ?: "") }
    var batchFrequency by remember { mutableStateOf(existingScreening?.batchFrequency ?: "Weekly") }
    var outputPerBatch by remember { mutableStateOf(existingScreening?.outputPerBatch ?: "25 baskets") }
    var cyclesPerYearText by remember { mutableStateOf(existingScreening?.cyclesPerYear?.toString() ?: "48") }
    var inputSupplySelectionBasis by remember { mutableStateOf(existingScreening?.inputSupplySelectionBasis ?: "Cost-based selection") }

    // Section 6: Business Outlook & Mindset
    var mindsetOrientation by remember { mutableStateOf(existingScreening?.mindsetOrientation ?: "Mix of both") }
    var sharesKnowledgeOpenly by remember { mutableStateOf(existingScreening?.sharesKnowledgeOpenly ?: "Yes — freely") }

    // Section 7: Business Management & Records (Practised, Partial, Not done)
    var recordFinancial by remember { mutableStateOf(existingScreening?.recordFinancial ?: "Partial") }
    var recordCustomer by remember { mutableStateOf(existingScreening?.recordCustomer ?: "Practised") }
    var recordVendor by remember { mutableStateOf(existingScreening?.recordVendor ?: "Not done") }
    var recordSalesData by remember { mutableStateOf(existingScreening?.recordSalesData ?: "Practised") }
    var recordStatutoryCompliance by remember { mutableStateOf(existingScreening?.recordStatutoryCompliance ?: "Not done") }
    var recordFinanceSeparateHousehold by remember { mutableStateOf(existingScreening?.recordFinanceSeparateHousehold ?: "Partial") }
    var recordTransparencyPartners by remember { mutableStateOf(existingScreening?.recordTransparencyPartners ?: "Practised") }
    var recordCreditClean by remember { mutableStateOf(existingScreening?.recordCreditClean ?: "Practised") }
    var recordKeepingNotes by remember { mutableStateOf(existingScreening?.recordKeepingNotes ?: "") }
    var productQualityMeasuresTaken by remember { mutableStateOf(existingScreening?.productQualityMeasuresTaken ?: "") }

    // Section 8: Technology & Digital Engagement
    var selectedTechPractices by remember {
        mutableStateOf(
            existingScreening?.techPracticesUsed?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf("WhatsApp for marketing/orders", "UPI for payments")
        )
    }
    var futureAspiration3Years by remember { mutableStateOf(existingScreening?.futureAspiration3Years ?: "") }

    // Admin & Metadata
    var fieldFellowName by remember { mutableStateOf(existingScreening?.fieldFellowName ?: "Rakesh Sharma") }
    var screeningDate by remember {
        mutableStateOf(existingScreening?.screeningDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    // Active Section Tab (0 to 8)
    var selectedSectionIndex by remember { mutableIntStateOf(0) }
    val sectionTitles = listOf(
        "Primary Info",
        "1. Background",
        "2. Profitability",
        "3. Market",
        "4. Operations",
        "5. Growth Plan",
        "6. Mindset",
        "7. Records",
        "8. Digital & Admin"
    )

    // Calculate current live score
    val tempEntity = SakhyaScreeningEntity(
        screeningId = currentScreeningId,
        linkedDduId = linkedDduId,
        entrepreneurName = entrepreneurName,
        productAndValueChain = productAndValueChain,
        receivedProfessionalTraining = receivedProfessionalTraining,
        trainingDetails = trainingDetails,
        yearsInBusiness = yearsInBusinessText.toDoubleOrNull() ?: 1.0,
        certificateAvailable = certificateAvailable,
        priorWorkExperienceYears = priorWorkExperienceYearsText.toDoubleOrNull() ?: 0.0,
        howGotIntoBusiness = howGotIntoBusiness,
        howGotIntoBusinessDescription = howGotIntoBusinessDescription,
        timeToBreakEven = timeToBreakEven,
        currentMonthlyProfitability = currentMonthlyProfitabilityText.toDoubleOrNull() ?: 0.0,
        partnersProfitSharingCount = partnersProfitSharingCountText.toIntOrNull() ?: 0,
        profitSharePercentPerPartner = profitSharePercentPerPartnerText.toDoubleOrNull() ?: 0.0,
        membersCostToCostBasisCount = membersCostToCostBasisCountText.toIntOrNull() ?: 0,
        earningsPerCostToCostMemberMonthly = earningsPerCostToCostMemberMonthlyText.toDoubleOrNull() ?: 0.0,
        dedicatedMarketingMember = dedicatedMarketingMember,
        marketLevelsServed = selectedMarketLevels.joinToString(", "),
        largestRevenueShareMarketLevel = largestRevenueShareMarketLevel,
        infrastructureJourneyMilestones = infrastructureJourneyMilestones,
        isInfrastructureSufficient = isInfrastructureSufficient,
        downtimeManagementAndImpact = downtimeManagementAndImpact,
        expansionAspirations = expansionAspirations,
        expansionDetails = expansionDetails,
        batchFrequency = batchFrequency,
        outputPerBatch = outputPerBatch,
        cyclesPerYear = cyclesPerYearText.toIntOrNull() ?: 12,
        inputSupplySelectionBasis = inputSupplySelectionBasis,
        mindsetOrientation = mindsetOrientation,
        sharesKnowledgeOpenly = sharesKnowledgeOpenly,
        recordFinancial = recordFinancial,
        recordCustomer = recordCustomer,
        recordVendor = recordVendor,
        recordSalesData = recordSalesData,
        recordStatutoryCompliance = recordStatutoryCompliance,
        recordFinanceSeparateHousehold = recordFinanceSeparateHousehold,
        recordTransparencyPartners = recordTransparencyPartners,
        recordCreditClean = recordCreditClean,
        recordKeepingNotes = recordKeepingNotes,
        productQualityMeasuresTaken = productQualityMeasuresTaken,
        techPracticesUsed = selectedTechPractices.joinToString(", "),
        futureAspiration3Years = futureAspiration3Years,
        fieldFellowName = fieldFellowName,
        screeningDate = screeningDate
    )
    val liveScore = viewModel.calculateSakhyaReadinessScore(tempEntity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sakhya Screening Form",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Form F1",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "DSDC — Sakhya Screening",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_sakhya_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Pre-fill Sunita Devi sample button
                    TextButton(
                        onClick = {
                            entrepreneurName = "Sunita Devi"
                            productAndValueChain = "Bamboo handicrafts — non-timber forest produce value chain"
                            receivedProfessionalTraining = true
                            trainingDetails = "Trained by a Khadi & Village Industries master craftsperson, 3 years ago"
                            yearsInBusinessText = "4"
                            certificateAvailable = true
                            priorWorkExperienceYearsText = "1"
                            howGotIntoBusiness = "Self-explored"
                            howGotIntoBusinessDescription = "Started making baskets at home after seeing a neighbour sell them at the weekly haat"
                            timeToBreakEven = "8 months"
                            currentMonthlyProfitabilityText = "4500"
                            partnersProfitSharingCountText = "2"
                            profitSharePercentPerPartnerText = "50"
                            membersCostToCostBasisCountText = "1"
                            earningsPerCostToCostMemberMonthlyText = "1200"
                            dedicatedMarketingMember = "Self-managed"
                            selectedMarketLevels = setOf("B2C — direct household sales", "SHG / group sales")
                            largestRevenueShareMarketLevel = "B2C — direct household sales"
                            infrastructureJourneyMilestones = "Started with hand tools at home; added a shared workshop shed with SHG support in year 2"
                            isInfrastructureSufficient = "Sufficient — but upgrades planned"
                            downtimeManagementAndImpact = "Monsoon reduces raw material supply for ~6 weeks/year; manages with pre-monsoon stockpiling"
                            expansionAspirations = "Yes — market demand driven"
                            expansionDetails = "Wants to add dyeing unit to supply colored baskets to urban retailers"
                            batchFrequency = "Weekly"
                            outputPerBatch = "25 baskets"
                            cyclesPerYearText = "48"
                            inputSupplySelectionBasis = "Cost-based selection"
                            mindsetOrientation = "Mix of both"
                            sharesKnowledgeOpenly = "Yes — freely"
                            recordFinancial = "Partial"
                            recordCustomer = "Practised"
                            recordVendor = "Not done"
                            recordSalesData = "Practised"
                            recordStatutoryCompliance = "Not done"
                            recordFinanceSeparateHousehold = "Partial"
                            recordTransparencyPartners = "Practised"
                            recordCreditClean = "Practised"
                            recordKeepingNotes = "Uses a notebook for sales; no formal vendor ledger yet"
                            productQualityMeasuresTaken = "Visual inspection of each piece before sale; rejects pieces with uneven weaving"
                            selectedTechPractices = setOf("WhatsApp for marketing/orders", "UPI for payments")
                            futureAspiration3Years = "Wants to register a formal MEG and supply to two district-level retail outlets"
                            fieldFellowName = "Rakesh Sharma"
                            screeningDate = "2026-04-01"
                            Toast.makeText(context, "Loaded Sunita Devi (Sample Record)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_prefill_sample")
                    ) {
                        Text("Fill Sample", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // PDF Footer Replica: "Clear form" and "Save record"
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            entrepreneurName = ""
                            productAndValueChain = ""
                            trainingDetails = ""
                            howGotIntoBusinessDescription = ""
                            infrastructureJourneyMilestones = ""
                            downtimeManagementAndImpact = ""
                            expansionDetails = ""
                            recordKeepingNotes = ""
                            productQualityMeasuresTaken = ""
                            futureAspiration3Years = ""
                            Toast.makeText(context, "Form cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_clear_form"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear form")
                    }

                    Button(
                        onClick = {
                            if (entrepreneurName.isBlank()) {
                                Toast.makeText(context, "Please enter Entrepreneur or Vaibhavi Name", Toast.LENGTH_SHORT).show()
                                selectedSectionIndex = 0
                                return@Button
                            }
                            if (productAndValueChain.isBlank()) {
                                Toast.makeText(context, "Please enter Product & Value Chain", Toast.LENGTH_SHORT).show()
                                selectedSectionIndex = 0
                                return@Button
                            }

                            val finalScreening = tempEntity.copy(
                                readinessScore = liveScore,
                                status = if (liveScore >= 75) "RECOMMENDED_FOR_UDYAMI" else if (liveScore >= 50) "DEVELOPING" else "EARLY_STAGE"
                            )

                            viewModel.saveSakhyaScreening(finalScreening) { savedId ->
                                Toast.makeText(context, "Sakhya Screening Saved! Score: $liveScore%", Toast.LENGTH_LONG).show()
                                onSavedSuccessfully(savedId)
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_save_sakhya_record"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4D3E), // DSDC Forestry / Agriculture Green
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Card with DSDC Programme Branding & Live Score
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F382C) // Forest Deep Green from PDF
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "DSDC — SAKHYA SCREENING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA3E635),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Sakhya Prospect Screening Form",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Primary information — business background, market exposure, and enterprise mindset",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetadataPill(text = "Form F1", bg = Color(0xFF1F5142), fg = Color.White)
                            MetadataPill(text = "All value chains", bg = Color(0xFF1F5142), fg = Color.White)
                            MetadataPill(text = "Field fellow / DIC team", bg = Color(0xFF1F5142), fg = Color.White)
                        }

                        // Readiness Score Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (liveScore >= 75) Color(0xFF22C55E) else if (liveScore >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$liveScore%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (liveScore >= 75) "Udyami Ready" else if (liveScore >= 50) "Developing" else "Early",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Section Tabs (0 to 8)
            ScrollableTabRow(
                selectedTabIndex = selectedSectionIndex,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                sectionTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSectionIndex == index,
                        onClick = { selectedSectionIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedSectionIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Main Content Body based on selected tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedSectionIndex) {
                    0 -> PrimaryInfoSection(
                        entrepreneurName = entrepreneurName,
                        onEntrepreneurNameChange = { entrepreneurName = it },
                        productAndValueChain = productAndValueChain,
                        onProductAndValueChainChange = { productAndValueChain = it },
                        linkedDduId = linkedDduId,
                        onLinkedDduIdChange = { linkedDduId = it },
                        surveys = allSurveysWithDetails.map { it.survey }
                    )
                    1 -> BusinessBackgroundSection(
                        receivedProfessionalTraining = receivedProfessionalTraining,
                        onReceivedTrainingChange = { receivedProfessionalTraining = it },
                        trainingDetails = trainingDetails,
                        onTrainingDetailsChange = { trainingDetails = it },
                        yearsInBusiness = yearsInBusinessText,
                        onYearsInBusinessChange = { yearsInBusinessText = it },
                        certificateAvailable = certificateAvailable,
                        onCertificateAvailableChange = { certificateAvailable = it },
                        priorWorkExperience = priorWorkExperienceYearsText,
                        onPriorWorkExperienceChange = { priorWorkExperienceYearsText = it },
                        howGotIntoBusiness = howGotIntoBusiness,
                        onHowGotIntoBusinessChange = { howGotIntoBusiness = it },
                        howGotIntoBusinessDescription = howGotIntoBusinessDescription,
                        onDescriptionChange = { howGotIntoBusinessDescription = it }
                    )
                    2 -> IncomeProfitabilitySection(
                        timeToBreakEven = timeToBreakEven,
                        onTimeToBreakEvenChange = { timeToBreakEven = it },
                        monthlyProfitability = currentMonthlyProfitabilityText,
                        onMonthlyProfitabilityChange = { currentMonthlyProfitabilityText = it },
                        partnersCount = partnersProfitSharingCountText,
                        onPartnersCountChange = { partnersProfitSharingCountText = it },
                        profitSharePercent = profitSharePercentPerPartnerText,
                        onProfitSharePercentChange = { profitSharePercentPerPartnerText = it },
                        costToCostMembers = membersCostToCostBasisCountText,
                        onCostToCostMembersChange = { membersCostToCostBasisCountText = it },
                        earningsPerCostMember = earningsPerCostToCostMemberMonthlyText,
                        onEarningsPerCostMemberChange = { earningsPerCostToCostMemberMonthlyText = it }
                    )
                    3 -> MarketExposureSection(
                        dedicatedMarketingMember = dedicatedMarketingMember,
                        onDedicatedMarketingMemberChange = { dedicatedMarketingMember = it },
                        selectedMarketLevels = selectedMarketLevels,
                        onMarketLevelToggle = { level ->
                            selectedMarketLevels = if (selectedMarketLevels.contains(level)) {
                                selectedMarketLevels - level
                            } else {
                                selectedMarketLevels + level
                            }
                        },
                        largestRevenueShare = largestRevenueShareMarketLevel,
                        onLargestRevenueShareChange = { largestRevenueShareMarketLevel = it }
                    )
                    4 -> InfrastructureOperationsSection(
                        journeyMilestones = infrastructureJourneyMilestones,
                        onJourneyMilestonesChange = { infrastructureJourneyMilestones = it },
                        isSufficient = isInfrastructureSufficient,
                        onIsSufficientChange = { isInfrastructureSufficient = it },
                        downtimeManagement = downtimeManagementAndImpact,
                        onDowntimeManagementChange = { downtimeManagementAndImpact = it }
                    )
                    5 -> GrowthPlanSection(
                        expansionAspirations = expansionAspirations,
                        onExpansionAspirationsChange = { expansionAspirations = it },
                        expansionDetails = expansionDetails,
                        onExpansionDetailsChange = { expansionDetails = it },
                        batchFrequency = batchFrequency,
                        onBatchFrequencyChange = { batchFrequency = it },
                        outputPerBatch = outputPerBatch,
                        onOutputPerBatchChange = { outputPerBatch = it },
                        cyclesPerYear = cyclesPerYearText,
                        onCyclesPerYearChange = { cyclesPerYearText = it },
                        inputSupplyBasis = inputSupplySelectionBasis,
                        onInputSupplyBasisChange = { inputSupplySelectionBasis = it }
                    )
                    6 -> BusinessOutlookMindsetSection(
                        orientation = mindsetOrientation,
                        onOrientationChange = { mindsetOrientation = it },
                        sharesKnowledge = sharesKnowledgeOpenly,
                        onSharesKnowledgeChange = { sharesKnowledgeOpenly = it }
                    )
                    7 -> BusinessManagementRecordsSection(
                        recordFinancial = recordFinancial,
                        onRecordFinancialChange = { recordFinancial = it },
                        recordCustomer = recordCustomer,
                        onRecordCustomerChange = { recordCustomer = it },
                        recordVendor = recordVendor,
                        onRecordVendorChange = { recordVendor = it },
                        recordSalesData = recordSalesData,
                        onRecordSalesDataChange = { recordSalesData = it },
                        recordStatutoryCompliance = recordStatutoryCompliance,
                        onRecordStatutoryComplianceChange = { recordStatutoryCompliance = it },
                        recordFinanceSeparateHousehold = recordFinanceSeparateHousehold,
                        onRecordFinanceSeparateHouseholdChange = { recordFinanceSeparateHousehold = it },
                        recordTransparencyPartners = recordTransparencyPartners,
                        onRecordTransparencyPartnersChange = { recordTransparencyPartners = it },
                        recordCreditClean = recordCreditClean,
                        onRecordCreditCleanChange = { recordCreditClean = it },
                        recordNotes = recordKeepingNotes,
                        onRecordNotesChange = { recordKeepingNotes = it },
                        qualityMeasures = productQualityMeasuresTaken,
                        onQualityMeasuresChange = { productQualityMeasuresTaken = it }
                    )
                    8 -> TechnologyAndAdminSection(
                        selectedTechPractices = selectedTechPractices,
                        onTechPracticeToggle = { tech ->
                            selectedTechPractices = if (selectedTechPractices.contains(tech)) {
                                selectedTechPractices - tech
                            } else {
                                selectedTechPractices + tech
                            }
                        },
                        futureAspiration = futureAspiration3Years,
                        onFutureAspirationChange = { futureAspiration3Years = it },
                        fieldFellowName = fieldFellowName,
                        onFieldFellowNameChange = { fieldFellowName = it },
                        screeningDate = screeningDate,
                        onScreeningDateChange = { screeningDate = it }
                    )
                }

                // Section Navigation Steppers (Previous / Next)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (selectedSectionIndex > 0) {
                        OutlinedButton(
                            onClick = { selectedSectionIndex -= 1 },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (selectedSectionIndex < sectionTitles.size - 1) {
                        Button(
                            onClick = { selectedSectionIndex += 1 },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Next: ${sectionTitles[selectedSectionIndex + 1]}")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 0: PRIMARY IDENTIFICATION
// -------------------------------------------------------------
@Composable
private fun PrimaryInfoSection(
    entrepreneurName: String,
    onEntrepreneurNameChange: (String) -> Unit,
    productAndValueChain: String,
    onProductAndValueChainChange: (String) -> Unit,
    linkedDduId: String,
    onLinkedDduIdChange: (String) -> Unit,
    surveys: List<com.example.data.model.SurveyEntity>
) {
    SectionCard(
        title = "PRIMARY INFORMATION",
        color = Color(0xFF1B4D3E),
        icon = Icons.Default.Person
    ) {
        // Field I: Name of Vaibhavi / MEG / Individual Entrepreneur
        Text(
            text = "I. Name of Vaibhavi / MEG / Individual Entrepreneur",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Full name as per Aadhaar or group registration",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = entrepreneurName,
            onValueChange = onEntrepreneurNameChange,
            placeholder = { Text("e.g. Sunita Devi / Maa Saraswati MEG") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_sakhya_name"),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Field II: Type of product and value chain
        Text(
            text = "II. Type of product and value chain",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "State the main product category they produce and the value chain it belongs to",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = productAndValueChain,
            onValueChange = onProductAndValueChainChange,
            placeholder = { Text("e.g. Bamboo handicrafts — non-timber forest produce value chain") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_sakhya_product_valuechain"),
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
            singleLine = false,
            minLines = 2
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Optional Link to existing DDU survey
        Text(
            text = "Link to Field Survey (Optional)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            text = "Connect this screening to an existing village institutional or enterprise survey record",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = linkedDduId,
            onValueChange = onLinkedDduIdChange,
            placeholder = { Text("e.g. DDU-BAL-2026-000124") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            singleLine = true
        )
    }
}

// -------------------------------------------------------------
// SECTION 1: BUSINESS BACKGROUND
// -------------------------------------------------------------
@Composable
private fun BusinessBackgroundSection(
    receivedProfessionalTraining: Boolean,
    onReceivedTrainingChange: (Boolean) -> Unit,
    trainingDetails: String,
    onTrainingDetailsChange: (String) -> Unit,
    yearsInBusiness: String,
    onYearsInBusinessChange: (String) -> Unit,
    certificateAvailable: Boolean,
    onCertificateAvailableChange: (Boolean) -> Unit,
    priorWorkExperience: String,
    onPriorWorkExperienceChange: (String) -> Unit,
    howGotIntoBusiness: String,
    onHowGotIntoBusinessChange: (String) -> Unit,
    howGotIntoBusinessDescription: String,
    onDescriptionChange: (String) -> Unit
) {
    SectionCard(
        title = "1. BUSINESS BACKGROUND (CONVERSATIONAL)",
        color = Color(0xFF10B981),
        icon = Icons.Default.WorkHistory
    ) {
        // Question 1: Professional Training
        Text(
            text = "1. Did you receive professional training from an expert?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Probe: from whom, where, how long ago",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = receivedProfessionalTraining,
                onClick = { onReceivedTrainingChange(true) },
                label = { Text("Yes") },
                leadingIcon = if (receivedProfessionalTraining) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = !receivedProfessionalTraining,
                onClick = { onReceivedTrainingChange(false) },
                label = { Text("No") }
            )
        }

        OutlinedTextField(
            value = trainingDetails,
            onValueChange = onTrainingDetailsChange,
            label = { Text("Training Details (from whom, where, how long ago)") },
            placeholder = { Text("e.g. Trained by a Khadi & Village Industries master craftsperson, 3 years ago") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Question 2: Years in Business & Certification
        Text(
            text = "2. Number of years in this business",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Count from first production/sale, not from training",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = yearsInBusiness,
                onValueChange = onYearsInBusinessChange,
                label = { Text("Years in business") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = priorWorkExperience,
                onValueChange = onPriorWorkExperienceChange,
                label = { Text("Prior work exp (yrs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Certificate available?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = certificateAvailable,
                onClick = { onCertificateAvailableChange(true) },
                label = { Text("Yes") }
            )
            FilterChip(
                selected = !certificateAvailable,
                onClick = { onCertificateAvailableChange(false) },
                label = { Text("No") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Question 3: How did you get into this business?
        Text(
            text = "3. How did you get into this business?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Select all that apply — then ask them to describe in their own words",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val origins = listOf("Family tradition", "Self-explored", "Promoted by someone", "Government scheme", "SHG / group", "Other")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            origins.forEach { origin ->
                FilterChip(
                    selected = howGotIntoBusiness.contains(origin),
                    onClick = { onHowGotIntoBusinessChange(origin) },
                    label = { Text(origin, fontSize = 12.sp) }
                )
            }
        }

        OutlinedTextField(
            value = howGotIntoBusinessDescription,
            onValueChange = onDescriptionChange,
            label = { Text("Description in own words") },
            placeholder = { Text("e.g. Started making baskets at home after seeing a neighbour sell them at the weekly haat") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}

// -------------------------------------------------------------
// SECTION 2: INCOME & PROFITABILITY
// -------------------------------------------------------------
@Composable
private fun IncomeProfitabilitySection(
    timeToBreakEven: String,
    onTimeToBreakEvenChange: (String) -> Unit,
    monthlyProfitability: String,
    onMonthlyProfitabilityChange: (String) -> Unit,
    partnersCount: String,
    onPartnersCountChange: (String) -> Unit,
    profitSharePercent: String,
    onProfitSharePercentChange: (String) -> Unit,
    costToCostMembers: String,
    onCostToCostMembersChange: (String) -> Unit,
    earningsPerCostMember: String,
    onEarningsPerCostMemberChange: (String) -> Unit
) {
    SectionCard(
        title = "2. INCOME & PROFITABILITY",
        color = Color(0xFFD97706),
        icon = Icons.Default.CurrencyRupee
    ) {
        // Question 4: Break-even & Profitability
        Text(
            text = "4. How long did the business take to reach break-even?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "From first investment to first month with no loss",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = timeToBreakEven,
                onValueChange = onTimeToBreakEvenChange,
                label = { Text("Time to break-even") },
                placeholder = { Text("e.g. 8 months") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = monthlyProfitability,
                onValueChange = onMonthlyProfitabilityChange,
                label = { Text("Monthly profit (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Question 5: Partners & Members
        Text(
            text = "5. How many members are engaged as business partners?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Partners share profit; cost-to-cost means paid per day/piece",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = partnersCount,
                onValueChange = onPartnersCountChange,
                label = { Text("Partners (profit-sharing)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = profitSharePercent,
                onValueChange = onProfitSharePercentChange,
                label = { Text("Profit share % / partner") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = costToCostMembers,
                onValueChange = onCostToCostMembersChange,
                label = { Text("Cost-to-cost members") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = earningsPerCostMember,
                onValueChange = onEarningsPerCostMemberChange,
                label = { Text("Earnings/member (₹/mo)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                singleLine = true
            )
        }
    }
}

// -------------------------------------------------------------
// SECTION 3: MARKET EXPOSURE
// -------------------------------------------------------------
@Composable
private fun MarketExposureSection(
    dedicatedMarketingMember: String,
    onDedicatedMarketingMemberChange: (String) -> Unit,
    selectedMarketLevels: Set<String>,
    onMarketLevelToggle: (String) -> Unit,
    largestRevenueShare: String,
    onLargestRevenueShareChange: (String) -> Unit
) {
    SectionCard(
        title = "3. MARKET EXPOSURE",
        color = Color(0xFF1E3A8A),
        icon = Icons.Default.Storefront
    ) {
        // Question 6: Dedicated Marketing Member
        Text(
            text = "6. Do you have a dedicated marketing team member?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Who takes responsibility for sales and buyer relationships?",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val marketingOptions = listOf("Yes — dedicated member", "Self-managed", "No marketing activity")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            marketingOptions.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDedicatedMarketingMemberChange(opt) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = dedicatedMarketingMember == opt,
                        onClick = { onDedicatedMarketingMemberChange(opt) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(opt, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Question 7: Market Levels Served
        Text(
            text = "7. What market levels are currently served? Which has largest revenue share?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        val marketLevels = listOf(
            "B2C — direct household sales",
            "B2B — traders / shops",
            "Institutional buyers",
            "Online platforms",
            "Export / outside district",
            "SHG / group sales"
        )

        Text(text = "Market levels currently served (select all that apply):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            marketLevels.forEach { level ->
                FilterChip(
                    selected = selectedMarketLevels.contains(level),
                    onClick = { onMarketLevelToggle(level) },
                    label = { Text(level, fontSize = 12.sp) },
                    leadingIcon = if (selectedMarketLevels.contains(level)) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Largest revenue share level:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = largestRevenueShare,
            onValueChange = onLargestRevenueShareChange,
            placeholder = { Text("e.g. B2C — direct household sales") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// -------------------------------------------------------------
// SECTION 4: INFRASTRUCTURE & OPERATIONS
// -------------------------------------------------------------
@Composable
private fun InfrastructureOperationsSection(
    journeyMilestones: String,
    onJourneyMilestonesChange: (String) -> Unit,
    isSufficient: String,
    onIsSufficientChange: (String) -> Unit,
    downtimeManagement: String,
    onDowntimeManagementChange: (String) -> Unit
) {
    SectionCard(
        title = "4. INFRASTRUCTURE & OPERATIONS",
        color = Color(0xFF4F46E5),
        icon = Icons.Default.PrecisionManufacturing
    ) {
        // Question 8: Setup journey
        Text(
            text = "8. Describe the infrastructure setup journey — major milestones from start to today",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = journeyMilestones,
            onValueChange = onJourneyMilestonesChange,
            placeholder = { Text("e.g. Started with hand tools at home; added a shared workshop shed with SHG support in year 2") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Question 9: Sufficiency
        Text(
            text = "9. Is the current infrastructure sufficient? Any planned upgrades?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val sufficiencyOptions = listOf(
            "Sufficient — no changes planned",
            "Sufficient — but upgrades planned",
            "Not sufficient"
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            sufficiencyOptions.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onIsSufficientChange(opt) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = isSufficient == opt,
                        onClick = { onIsSufficientChange(opt) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(opt, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Question 10: Downtime
        Text(
            text = "10. How is downtime managed? Are there major downtime events and what is their impact?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = downtimeManagement,
            onValueChange = onDowntimeManagementChange,
            placeholder = { Text("e.g. Monsoon reduces raw material supply for ~6 weeks/year; manages with pre-monsoon stockpiling") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}

// -------------------------------------------------------------
// SECTION 5: GROWTH PLAN & INPUT MANAGEMENT
// -------------------------------------------------------------
@Composable
private fun GrowthPlanSection(
    expansionAspirations: String,
    onExpansionAspirationsChange: (String) -> Unit,
    expansionDetails: String,
    onExpansionDetailsChange: (String) -> Unit,
    batchFrequency: String,
    onBatchFrequencyChange: (String) -> Unit,
    outputPerBatch: String,
    onOutputPerBatchChange: (String) -> Unit,
    cyclesPerYear: String,
    onCyclesPerYearChange: (String) -> Unit,
    inputSupplyBasis: String,
    onInputSupplyBasisChange: (String) -> Unit
) {
    SectionCard(
        title = "5. GROWTH PLAN & INPUT MANAGEMENT",
        color = Color(0xFF15803D),
        icon = Icons.Default.TrendingUp
    ) {
        // Question 11: Expansion aspirations
        Text(
            text = "11. Are there expansion aspirations? What and why — market demand or profitability driven?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val aspirations = listOf(
            "Yes — market demand driven",
            "Yes — profitability driven",
            "No expansion plans"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspirations.forEach { asp ->
                FilterChip(
                    selected = expansionAspirations == asp,
                    onClick = { onExpansionAspirationsChange(asp) },
                    label = { Text(asp, fontSize = 12.sp) }
                )
            }
        }

        OutlinedTextField(
            value = expansionDetails,
            onValueChange = onExpansionDetailsChange,
            label = { Text("Expansion details (what & why)") },
            placeholder = { Text("e.g. Wants to add dyeing unit to supply colored baskets to urban retailers") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Question 12: Production batches
        Text(
            text = "12. Production batches — describe cycles, frequencies, and typical output",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = batchFrequency,
                onValueChange = onBatchFrequencyChange,
                label = { Text("Frequency") },
                placeholder = { Text("Weekly") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = outputPerBatch,
                onValueChange = onOutputPerBatchChange,
                label = { Text("Output / batch") },
                placeholder = { Text("25 baskets") },
                modifier = Modifier.weight(1.2f),
                singleLine = true
            )
            OutlinedTextField(
                value = cyclesPerYear,
                onValueChange = onCyclesPerYearChange,
                label = { Text("Cycles / yr") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.9f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Question 13: Input supply
        Text(
            text = "13. Input supply — vendor selection and storage practices",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val vendors = listOf("Defined regular vendors", "Quality-based selection", "Cost-based selection", "Logistics / proximity", "Ad hoc purchases")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            vendors.forEach { v ->
                FilterChip(
                    selected = inputSupplyBasis.contains(v),
                    onClick = { onInputSupplyBasisChange(v) },
                    label = { Text(v, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 6: BUSINESS OUTLOOK & MINDSET
// -------------------------------------------------------------
@Composable
private fun BusinessOutlookMindsetSection(
    orientation: String,
    onOrientationChange: (String) -> Unit,
    sharesKnowledge: String,
    onSharesKnowledgeChange: (String) -> Unit
) {
    SectionCard(
        title = "6. BUSINESS OUTLOOK & MINDSET",
        color = Color(0xFF0D9488),
        icon = Icons.Default.Psychology
    ) {
        // Question 14: Self-centric vs Opportunity-driven
        Text(
            text = "14. Is the business self-centric or opportunity-driven?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Self-centric = manages for personal needs; opportunity-driven = scans for market/growth",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val orientations = listOf("Mostly self-centric", "Mix of both", "Opportunity-driven")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            orientations.forEach { item ->
                FilterChip(
                    selected = orientation == item,
                    onClick = { onOrientationChange(item) },
                    label = { Text(item, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Question 15: Shares knowledge
        Text(
            text = "15. Does she share knowledge and business ideas openly with others?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val sharingOptions = listOf("Yes — freely", "Selective sharing", "Rarely shares")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sharingOptions.forEach { item ->
                FilterChip(
                    selected = sharesKnowledge == item,
                    onClick = { onSharesKnowledgeChange(item) },
                    label = { Text(item, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 7: BUSINESS MANAGEMENT & RECORDS
// -------------------------------------------------------------
@Composable
private fun BusinessManagementRecordsSection(
    recordFinancial: String,
    onRecordFinancialChange: (String) -> Unit,
    recordCustomer: String,
    onRecordCustomerChange: (String) -> Unit,
    recordVendor: String,
    onRecordVendorChange: (String) -> Unit,
    recordSalesData: String,
    onRecordSalesDataChange: (String) -> Unit,
    recordStatutoryCompliance: String,
    onRecordStatutoryComplianceChange: (String) -> Unit,
    recordFinanceSeparateHousehold: String,
    onRecordFinanceSeparateHouseholdChange: (String) -> Unit,
    recordTransparencyPartners: String,
    onRecordTransparencyPartnersChange: (String) -> Unit,
    recordCreditClean: String,
    onRecordCreditCleanChange: (String) -> Unit,
    recordNotes: String,
    onRecordNotesChange: (String) -> Unit,
    qualityMeasures: String,
    onQualityMeasuresChange: (String) -> Unit
) {
    SectionCard(
        title = "7. BUSINESS MANAGEMENT & RECORDS",
        color = Color(0xFFB45309),
        icon = Icons.Default.FactCheck
    ) {
        Text(
            text = "16. Record-keeping and compliance practices",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Rate each practice: ✓ Practised well · ~ Partially · ✗ Not done",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Compliance Table Rows
        CompliancePracticeRow("Financial records maintained", recordFinancial, onRecordFinancialChange)
        CompliancePracticeRow("Customer records maintained", recordCustomer, onRecordCustomerChange)
        CompliancePracticeRow("Vendor records maintained", recordVendor, onRecordVendorChange)
        CompliancePracticeRow("Sales data tracked", recordSalesData, onRecordSalesDataChange)
        CompliancePracticeRow("Statutory/compliance followed", recordStatutoryCompliance, onRecordStatutoryComplianceChange)
        CompliancePracticeRow("Finance kept separate from household", recordFinanceSeparateHousehold, onRecordFinanceSeparateHouseholdChange)
        CompliancePracticeRow("Transparency with partners/group", recordTransparencyPartners, onRecordTransparencyPartnersChange)
        CompliancePracticeRow("Credit records clean (no delinquency)", recordCreditClean, onRecordCreditCleanChange)

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = recordNotes,
            onValueChange = onRecordNotesChange,
            label = { Text("Record-keeping notes") },
            placeholder = { Text("e.g. Uses a notebook for sales; no formal vendor ledger yet") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Question 17: Product quality measures
        Text(
            text = "17. How is product quality ensured? What specific measures are taken?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = qualityMeasures,
            onValueChange = onQualityMeasuresChange,
            placeholder = { Text("e.g. Visual inspection of each piece before sale; rejects pieces with uneven weaving") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}

@Composable
private fun CompliancePracticeRow(
    label: String,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "Practised" to "✓",
                    "Partial" to "~",
                    "Not done" to "✗"
                ).forEach { (key, symbol) ->
                    val isSelected = currentValue.equals(key, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) {
                            when (key) {
                                "Practised" -> Color(0xFF15803D)
                                "Partial" -> Color(0xFFD97706)
                                else -> Color(0xFFDC2626)
                            }
                        } else Color.Transparent,
                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier
                            .clickable { onValueChange(key) }
                            .padding(2.dp)
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 8: TECHNOLOGY & ADMIN
// -------------------------------------------------------------
@Composable
private fun TechnologyAndAdminSection(
    selectedTechPractices: Set<String>,
    onTechPracticeToggle: (String) -> Unit,
    futureAspiration: String,
    onFutureAspirationChange: (String) -> Unit,
    fieldFellowName: String,
    onFieldFellowNameChange: (String) -> Unit,
    screeningDate: String,
    onScreeningDateChange: (String) -> Unit
) {
    SectionCard(
        title = "8. TECHNOLOGY & DIGITAL ENGAGEMENT",
        color = Color(0xFF1E293B),
        icon = Icons.Default.Devices
    ) {
        // Question 18: Digital practices
        Text(
            text = "18. Technology and digital practices used for business",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val techPractices = listOf(
            "Online apps for records/accounting",
            "WhatsApp for marketing/orders",
            "UPI for payments",
            "Watches YouTube / learning videos",
            "Aware of digital credit / market platforms",
            "Experiments with new products / tech"
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            techPractices.forEach { tech ->
                FilterChip(
                    selected = selectedTechPractices.contains(tech),
                    onClick = { onTechPracticeToggle(tech) },
                    label = { Text(tech, fontSize = 12.sp) },
                    leadingIcon = if (selectedTechPractices.contains(tech)) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Question 19: Future aspiration
        Text(
            text = "19. Future aspiration — where do they see their enterprise in 3 years?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = futureAspiration,
            onValueChange = onFutureAspirationChange,
            placeholder = { Text("e.g. Wants to register a formal MEG and supply to two district-level retail outlets") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ADMINISTRATION",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = fieldFellowName,
                onValueChange = onFieldFellowNameChange,
                label = { Text("Field fellow name") },
                modifier = Modifier.weight(1.2f),
                singleLine = true
            )
            OutlinedTextField(
                value = screeningDate,
                onValueChange = onScreeningDateChange,
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPONENTS
// -------------------------------------------------------------
@Composable
private fun SectionCard(
    title: String,
    color: Color,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun MetadataPill(text: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
