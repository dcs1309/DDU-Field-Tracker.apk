package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.ExportTargetRecord
import com.example.ui.components.SingleRecordExportDialog
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakhyaDetailScreen(
    viewModel: FieldIntelligenceViewModel,
    screeningId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()
    val screening = remember(allScreenings, screeningId) {
        allScreenings.find { it.screeningId == screeningId }
    }

    if (screening == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Screening record not found")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    var showExportDialog by remember { mutableStateOf(false) }

    val readinessColor = when {
        screening.readinessScore >= 75 -> Color(0xFF15803D)
        screening.readinessScore >= 50 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = screening.entrepreneurName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Prospect Assessment Dossier (F1)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_sakhya_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("btn_export_sakhya_detail")
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export Dossier (PDF/Excel/PPTM)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToEdit(screening.screeningId) },
                        modifier = Modifier.testTag("btn_edit_sakhya_detail")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showExportDialog = true },
                containerColor = Color(0xFF15803D),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                text = { Text("Export Dossier (PDF/Excel/PPTM)", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("btn_export_sakhya_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Executive Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F382C)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DSDC — SAKHYA SCREENING CANDIDATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA3E635),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = screening.entrepreneurName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = screening.productAndValueChain,
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        // Readiness Score Circular Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = readinessColor
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${screening.readinessScore}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (screening.readinessScore >= 75) "Udyami Ready" else if (screening.readinessScore >= 50) "Developing" else "Early",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1F5142),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Monthly Profit", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                                Text("₹${screening.currentMonthlyProfitability.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Surface(
                            color = Color(0xFF1F5142),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("In Business", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                                Text("${screening.yearsInBusiness} years", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Surface(
                            color = Color(0xFF1F5142),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Break-Even", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                                Text(screening.timeToBreakEven, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 1. Business Background Card
            DossierSectionCard(title = "1. Business Background") {
                DossierField("Professional Training", if (screening.receivedProfessionalTraining) "Yes" else "No")
                if (screening.receivedProfessionalTraining && screening.trainingDetails.isNotBlank()) {
                    DossierField("Training Details", screening.trainingDetails)
                }
                DossierField("Years in Business", "${screening.yearsInBusiness} years (Certificate: ${if (screening.certificateAvailable) "Yes" else "No"})")
                DossierField("Prior Work Experience", "${screening.priorWorkExperienceYears} years")
                DossierField("How Got Into Business", screening.howGotIntoBusiness)
                if (screening.howGotIntoBusinessDescription.isNotBlank()) {
                    DossierField("Description in Own Words", screening.howGotIntoBusinessDescription)
                }
            }

            // 2. Income & Profitability Card
            DossierSectionCard(title = "2. Income & Profitability") {
                DossierField("Time to Break-Even", screening.timeToBreakEven)
                DossierField("Current Monthly Profitability", "₹${screening.currentMonthlyProfitability.toInt()}")
                DossierField("Partners (Profit-Sharing)", "${screening.partnersProfitSharingCount} (${screening.profitSharePercentPerPartner.toInt()}% per partner)")
                DossierField("Cost-to-Cost Members", "${screening.membersCostToCostBasisCount} members (₹${screening.earningsPerCostToCostMemberMonthly.toInt()}/month)")
            }

            // 3. Market Exposure Card
            DossierSectionCard(title = "3. Market Exposure") {
                DossierField("Marketing Responsibility", screening.dedicatedMarketingMember)
                DossierField("Market Levels Served", screening.marketLevelsServed.ifBlank { "Direct B2C" })
                DossierField("Largest Revenue Share", screening.largestRevenueShareMarketLevel)
            }

            // 4. Infrastructure & Operations Card
            DossierSectionCard(title = "4. Infrastructure & Operations") {
                DossierField("Setup Journey Milestones", screening.infrastructureJourneyMilestones.ifBlank { "Not specified" })
                DossierField("Infrastructure Sufficiency", screening.isInfrastructureSufficient)
                DossierField("Downtime Management & Impact", screening.downtimeManagementAndImpact.ifBlank { "Managed without disruption" })
            }

            // 5. Growth Plan & Input Management Card
            DossierSectionCard(title = "5. Growth Plan & Input Management") {
                DossierField("Expansion Aspirations", screening.expansionAspirations)
                if (screening.expansionDetails.isNotBlank()) {
                    DossierField("Expansion Details", screening.expansionDetails)
                }
                DossierField("Batch Production", "${screening.batchFrequency} frequency • ${screening.outputPerBatch} • ${screening.cyclesPerYear} cycles/yr")
                DossierField("Input Supply / Vendor Selection", screening.inputSupplySelectionBasis)
            }

            // 6. Business Outlook & Mindset Card
            DossierSectionCard(title = "6. Business Outlook & Mindset") {
                DossierField("Enterprise Mindset", screening.mindsetOrientation)
                DossierField("Shares Knowledge & Ideas Openly", screening.sharesKnowledgeOpenly)
            }

            // 7. Business Management & Records Card (Table format from PDF)
            DossierSectionCard(title = "7. Business Management & Records (Practice Audit)") {
                val practices = listOf(
                    "Financial records maintained" to screening.recordFinancial,
                    "Customer records maintained" to screening.recordCustomer,
                    "Vendor records maintained" to screening.recordVendor,
                    "Sales data tracked" to screening.recordSalesData,
                    "Statutory/compliance followed" to screening.recordStatutoryCompliance,
                    "Finance kept separate from household" to screening.recordFinanceSeparateHousehold,
                    "Transparency with partners/group" to screening.recordTransparencyPartners,
                    "Credit records clean (no delinquency)" to screening.recordCreditClean
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    practices.forEach { (label, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            val (badgeBg, badgeFg) = when (status.lowercase()) {
                                "practised" -> Color(0xFF15803D) to Color.White
                                "partial" -> Color(0xFFD97706) to Color.White
                                else -> Color(0xFFDC2626) to Color.White
                            }
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (screening.recordKeepingNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DossierField("Record-Keeping Notes", screening.recordKeepingNotes)
                }

                if (screening.productQualityMeasuresTaken.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DossierField("Product Quality Measures Taken", screening.productQualityMeasuresTaken)
                }
            }

            // 8. Technology & Digital Engagement Card
            DossierSectionCard(title = "8. Technology & Digital Engagement") {
                DossierField("Technology & Digital Practices", screening.techPracticesUsed.ifBlank { "None documented" })
                if (screening.futureAspiration3Years.isNotBlank()) {
                    DossierField("Future Aspiration (3 Years)", screening.futureAspiration3Years)
                }
            }

            // Administration Card
            DossierSectionCard(title = "Administration & Verification") {
                DossierField("Screening ID", screening.screeningId)
                DossierField("Field Fellow / DIC Team", screening.fieldFellowName)
                DossierField("Date of Screening", screening.screeningDate)
                if (screening.linkedDduId.isNotBlank()) {
                    DossierField("Linked Survey DDU ID", screening.linkedDduId)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showExportDialog) {
        val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
        SingleRecordExportDialog(
            targetRecord = ExportTargetRecord.SakhyaRecord(screening),
            userProfile = userProfile,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun DossierSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DossierField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
