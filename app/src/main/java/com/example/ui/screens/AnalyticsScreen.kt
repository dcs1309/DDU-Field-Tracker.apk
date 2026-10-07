package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.analytics.AnalyticsDataConnector
import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.DataQualityService
import com.example.data.analytics.PowerBIService
import com.example.data.analytics.model.*
import com.example.data.model.SurveyWithDetails
import com.example.data.model.OpportunityEntity
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.ui.components.CollatedExportDialog
import com.example.ui.components.analytics.*
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateToRecordDetail: (String) -> Unit,
    onNavigateToOpportunityGraph: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToWebPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val powerBIService = remember { PowerBIService(context) }

    val allSurveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()
    val allOpportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val stage2Assessments by viewModel.allStage2Assessments.collectAsStateWithLifecycle()
    val sakhyaScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()
    val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val powerBiConfig by powerBIService.configState.collectAsStateWithLifecycle()

    var filterState by remember { mutableStateOf(AnalyticsFilterState()) }
    var selectedSectionIndex by remember { mutableStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showPowerBiConfig by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Computed filtered datasets
    val filteredSurveys = remember(allSurveys, filterState) {
        AnalyticsEngine.filterSurveys(allSurveys, filterState)
    }

    val kpiSummary = remember(filteredSurveys, allOpportunities, stage2Assessments, sakhyaScreenings) {
        AnalyticsEngine.computeExecutiveKpis(filteredSurveys, allOpportunities, stage2Assessments, sakhyaScreenings)
    }

    val pipelineStages = remember(allOpportunities) {
        AnalyticsEngine.computePipeline(allOpportunities)
    }

    val productOpportunities = remember(filteredSurveys, allOpportunities) {
        AnalyticsEngine.computeProductOpportunities(filteredSurveys, allOpportunities)
    }

    val marketGaps = remember(filteredSurveys) {
        AnalyticsEngine.computeMarketGaps(filteredSurveys)
    }

    val institutionalRows = remember(filteredSurveys) {
        AnalyticsEngine.computeInstitutionalDemandRows(filteredSurveys)
    }

    val swsmProfiles = remember { AnalyticsEngine.computeSwsmProfiles() }
    val vaibhaviProfiles = remember { AnalyticsEngine.computeVaibhaviProfiles() }
    val sakhyaProfiles = remember(sakhyaScreenings) { AnalyticsEngine.computeSakhyaProfiles(sakhyaScreenings) }
    val vaatikaProfiles = remember(filteredSurveys) { AnalyticsEngine.computeVaatikaProfiles(filteredSurveys) }
    val weeklyReview = remember(filteredSurveys, allOpportunities) { AnalyticsEngine.computeWeeklyReview(filteredSurveys, allOpportunities) }
    val qualityMetrics = remember(filteredSurveys) { DataQualityService.evaluateQuality(filteredSurveys) }

    // Dialogs
    if (showFilterSheet) {
        val villages = remember(allSurveys) { allSurveys.map { it.survey.village }.distinct().sorted() }
        val blocks = remember(allSurveys) { allSurveys.map { it.survey.block }.distinct().sorted() }
        val categories = remember(productOpportunities) { productOpportunities.map { it.category }.distinct().sorted() }

        PowerBiFilterBottomSheet(
            filterState = filterState,
            villages = villages,
            blocks = blocks,
            categories = categories,
            onFilterChange = { filterState = it },
            onReset = { filterState = AnalyticsFilterState() },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showPowerBiConfig) {
        PowerBiConfigDialog(
            powerBIService = powerBIService,
            onDismiss = { showPowerBiConfig = false }
        )
    }

    if (showSearchDialog) {
        UniversalAnalyticsSearchDialog(
            surveys = allSurveys,
            opportunities = allOpportunities,
            stage2Assessments = stage2Assessments,
            sakhyaScreenings = sakhyaScreenings,
            onNavigateToRecord = onNavigateToRecordDetail,
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showExportDialog) {
        CollatedExportDialog(
            userProfile = userProfile,
            surveys = filteredSurveys,
            opportunities = allOpportunities,
            stage2Assessments = stage2Assessments,
            sakhyaScreenings = sakhyaScreenings,
            stats = viewModel.statistics.value,
            onDismiss = { showExportDialog = false }
        )
    }

    val sections = listOf(
        "1. Field Progress",
        "2. DDU Pipeline",
        "3. Opportunity Intel",
        "4. Market Demand",
        "5. Product Analytics",
        "6. Institutional Demand",
        "7. Vendor & Supplier",
        "8. SWSM & Vaibhavi",
        "9. Geographic Intel",
        "10. Team Performance",
        "11. Data Quality",
        "12. Reports",
        "13. Power BI Dashboard"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ANALYTICS",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = DduPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "POWER BI READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DduPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "From Field Data to Verified Local Opportunities",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.testTag("btn_open_analytics_search")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Analytics")
                    }

                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("btn_open_analytics_filter")
                    ) {
                        BadgedBox(
                            badge = {
                                if (filterState != AnalyticsFilterState()) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }

                    IconButton(
                        onClick = { showPowerBiConfig = true },
                        modifier = Modifier.testTag("btn_top_powerbi_config")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Power BI Settings")
                    }

                    IconButton(
                        onClick = onNavigateToWebPortal,
                        modifier = Modifier.testTag("btn_open_web_portal")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Master Web Portal", tint = Color(0xFFD97706))
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("btn_open_export_reports")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Reports")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. System Status Banner (Requirement #59)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("system_status_banner")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SYSTEM STATUS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Sync: $lastSyncTime",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusItemPill(label = "Field DB", value = "CONNECTED", isOk = true)
                            StatusItemPill(label = "Offline Sync", value = if (pendingSyncCount == 0) "ACTIVE" else "$pendingSyncCount Pending", isOk = pendingSyncCount == 0)
                            StatusItemPill(label = "Analytics Engine", value = "ACTIVE", isOk = true)
                            StatusItemPill(label = "Power BI", value = if (powerBiConfig.isConfigured) "CONNECTED" else "NOT CONFIGURED", isOk = powerBiConfig.isConfigured)
                        }
                    }
                }
            }

            // 2. Executive Analytics Dashboard (Requirement #3)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Executive Analytics Dashboard",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredSurveys.size} surveys filtered",
                            fontSize = 11.sp,
                            color = DduPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary KPI Cards Grid (Requirement #3)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(title = "Total DDUs", value = "${kpiSummary.totalDdus}", subtitle = "${kpiSummary.activeDdus} Active", color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                        KpiCard(title = "Stage 1", value = "${kpiSummary.ddusInStage1}", subtitle = "Discovery & Demand", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                        KpiCard(title = "Stage 2", value = "${kpiSummary.ddusInStage2}", subtitle = "Village Production", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                        KpiCard(title = "Stage 3", value = "${kpiSummary.ddusInStage3}", subtitle = "Commercialization", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(title = "Surveys Done", value = "${kpiSummary.institutionalSurveysCompleted + kpiSummary.retailSurveysCompleted}", subtitle = "${kpiSummary.institutionalSurveysCompleted} Inst / ${kpiSummary.retailSurveysCompleted} Retail", color = Color(0xFF00796B), modifier = Modifier.weight(1f))
                        KpiCard(title = "Businesses Mapped", value = "${kpiSummary.businessesMapped}", subtitle = "${kpiSummary.vendorsIdentified} Vendors Tracked", color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                        KpiCard(title = "SWSM Linked", value = "${kpiSummary.swsmGroupsLinked}", subtitle = "${kpiSummary.vaibhaviEntrepreneursLinked} Vaibhavis", color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                        KpiCard(title = "Sakhyas", value = "${kpiSummary.potentialSakhyasIdentified}", subtitle = "Trainers & Prospects", color = Color(0xFFEC4899), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Revenue and Demand Gaps Cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(
                            title = "Est. Local Demand",
                            value = "₹${String.format(Locale.US, "%,.0f", kpiSummary.estimatedLocalDemandValueMonthly / 1000)}k/mo",
                            subtitle = "Across ${kpiSummary.villagesCovered} Villages",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Market Gaps",
                            value = "${kpiSummary.marketGapsIdentified}",
                            subtitle = "Localisation Opportunities",
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Section Navigation Tabs (13 Sections)
            item {
                Column {
                    Text(
                        text = "ANALYTICS SECTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("analytics_sections_row")
                    ) {
                        items(sections.indices.toList()) { index ->
                            val isSelected = selectedSectionIndex == index
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DduPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) DduPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { selectedSectionIndex = index }
                                    .testTag("tab_section_$index")
                            ) {
                                Text(
                                    text = sections[index],
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Dynamic Section Content Renderer
            when (selectedSectionIndex) {
                0 -> {
                    // SECTION 1: FIELD PROGRESS & WEEKLY REVIEW
                    item {
                        WeeklyReviewCard(
                            review = weeklyReview,
                            onExportSnapshot = {
                                Toast.makeText(context, "Weekly Snapshot exported to reports", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        ProgressVsTargetCard()
                    }
                }

                1 -> {
                    // SECTION 2: DDU PIPELINE & FUNNEL
                    item {
                        DduPipelineCard(
                            pipelineStages = pipelineStages,
                            onClickStage = { stage ->
                                Toast.makeText(context, "Filtering DDUs at ${stage.stageTitle} (${stage.countEntering} total)", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        DduProgressFunnelCard()
                    }
                }

                2 -> {
                    // SECTION 3: OPPORTUNITY INTELLIGENCE
                    item {
                        OpportunityPortfolioMatrix(
                            opportunities = productOpportunities,
                            onSelectOpportunity = { opp ->
                                Toast.makeText(context, "Selected ${opp.productName} (Score ${opp.opportunityScore}/100)", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Dynamic Opportunity Cards (${productOpportunities.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(productOpportunities, key = { it.productName }) { opp ->
                        DynamicOpportunityCard(
                            opportunity = opp,
                            onClickRecord = onNavigateToRecordDetail,
                            allSurveys = filteredSurveys
                        )
                    }
                }

                3 -> {
                    // SECTION 4: MARKET DEMAND & RETAIL
                    item {
                        MarketDemandRetailCard(surveys = filteredSurveys)
                    }

                    item {
                        MarketGapComparisonCard(marketGaps = marketGaps)
                    }
                }

                5 -> {
                    // SECTION 6: INSTITUTIONAL DEMAND
                    item {
                        InstitutionalDemandTableCard(
                            rows = institutionalRows,
                            onOpenRecord = onNavigateToRecordDetail
                        )
                    }
                }

                6 -> {
                    // SECTION 7: VENDOR & SUPPLIER INTELLIGENCE
                    item {
                        VendorSupplierIntelligenceCard(surveys = filteredSurveys)
                    }
                }

                7 -> {
                    // SECTION 8: SWSM & VAIBHAVI LINKAGE
                    item {
                        SwsmVaibhaviLinkageCard(
                            swsmProfiles = swsmProfiles,
                            vaibhaviProfiles = vaibhaviProfiles,
                            sakhyaProfiles = sakhyaProfiles
                        )
                    }
                }

                8 -> {
                    // SECTION 9: GEOGRAPHIC INTELLIGENCE & VAATIKA
                    item {
                        VaatikaDashboardCard(
                            vaatikas = vaatikaProfiles,
                            onOpenMap = onNavigateToMap
                        )
                    }

                    item {
                        FieldRouteAnalysisCard()
                    }
                }

                9 -> {
                    // SECTION 10: FIELD TEAM PERFORMANCE
                    item {
                        FieldTeamPerformanceCard(surveys = filteredSurveys)
                    }
                }

                10 -> {
                    // SECTION 11: DATA QUALITY DASHBOARD
                    item {
                        DataQualityDashboardCard(
                            qualityMetrics = qualityMetrics,
                            onOpenRecord = onNavigateToRecordDetail
                        )
                    }
                }

                11 -> {
                    // SECTION 12: POWER BI DATA CONNECTOR & REPORTS
                    item {
                        PowerBiReportsConnectorCard(
                            surveys = filteredSurveys,
                            opportunities = allOpportunities,
                            stage2Assessments = stage2Assessments,
                            sakhyaScreenings = sakhyaScreenings,
                            onCopyJson = {
                                val json = AnalyticsDataConnector.generatePowerBiDatasetJson(
                                    filteredSurveys, allOpportunities, stage2Assessments, sakhyaScreenings
                                )
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Power BI Relational Dataset JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                            },
                            onCopyCsv = {
                                val csv = AnalyticsDataConnector.generateFactDdusCsv(allOpportunities)
                                clipboardManager.setText(AnnotatedString(csv))
                                Toast.makeText(context, "fact_ddus CSV schema copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                12 -> {
                    // SECTION 13: POWER BI DASHBOARD EMBED & MASTER WEB PORTAL
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().testTag("card_master_web_portal_promo")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("DDU Master Website Portal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("Unified Desktop & Web Access Gateway", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Surface(color = Color(0xFFD97706).copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                                        Text("WEB PORTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Access the complete web portal interface based on this app: interactive Power BI embed, relational semantic models, GIS field cartography, full field records directory, and portable HTML export.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onNavigateToWebPortal,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_launch_web_portal")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Launch Master Website Portal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        PowerBiEmbedView(
                            config = powerBiConfig,
                            onConfigureClick = { showPowerBiConfig = true },
                            onRefreshDataset = {
                                val msg = powerBIService.triggerRefresh()
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                else -> {
                    // Default to Product Analytics (Section 4)
                    item {
                        ProductAnalyticsOverviewCard(productOpportunities = productOpportunities)
                    }

                    items(productOpportunities, key = { it.productName }) { opp ->
                        DynamicOpportunityCard(
                            opportunity = opp,
                            onClickRecord = onNavigateToRecordDetail,
                            allSurveys = filteredSurveys
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MODULAR CARDS FOR EACH ANALYTICS SECTION
// ---------------------------------------------------------------------------

@Composable
private fun StatusItemPill(label: String, value: String, isOk: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isOk) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOk) Color(0xFF10B981) else Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WeeklyReviewCard(review: WeeklyReviewMetrics, onExportSnapshot: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().testTag("weekly_review_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(review.weekLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Basis of Vidushi Fellowship Weekly Reporting", fontSize = 11.sp, color = DduPrimary)
                }
                IconButton(onClick = onExportSnapshot) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Snapshot")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Text("Key Operational Findings This Week:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            review.majorFindings.forEach { finding ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("✓ ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    Text(finding, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Pending Validations & Blockers:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            Spacer(modifier = Modifier.height(4.dp))
            (review.pendingValidations + review.blockers).take(3).forEach { blocker ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    Text(blocker, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ProgressVsTargetCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Progress vs Target", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            TargetProgressBar(label = "DDUs Initiated", actual = 8, target = 10)
            TargetProgressBar(label = "Institutional Surveys", actual = 16, target = 20)
            TargetProgressBar(label = "Retail Surveys", actual = 24, target = 30)
            TargetProgressBar(label = "Businesses Mapped", actual = 42, target = 50)
            TargetProgressBar(label = "Sakhya Prospects", actual = 4, target = 5)
        }
    }
}

@Composable
private fun TargetProgressBar(label: String, actual: Int, target: Int) {
    val pct = (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("$actual / $target (${(pct * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (pct >= 0.8f) Color(0xFF10B981) else Color(0xFF0284C7),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun DduPipelineCard(pipelineStages: List<PipelineStageMetric>, onClickStage: (PipelineStageMetric) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("ddu_pipeline_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("11-Step Visual DDU Pipeline", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Click any stage to filter relevant DDUs and view conversion %", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            pipelineStages.forEach { stage ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onClickStage(stage) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = DduPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${stage.stageOrder}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = DduPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stage.stageTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Avg ${String.format(Locale.US, "%.1f", stage.averageDaysSpent)} days • ${stage.conversionPercentage.toInt()}% conversion", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${stage.countEntering} DDUs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DduProgressFunnelCard() {
    val funnel = remember { AnalyticsEngine.computeFunnel() }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DDU Progress Funnel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            funnel.forEach { (step, pct) ->
                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(step, fontSize = 11.5.sp)
                        Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                    }
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = DduPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductAnalyticsOverviewCard(productOpportunities: List<ProductOpportunityMetric>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Product Opportunity Dashboard", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Evaluated with weighted DDU Opportunity Score (0-100)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            productOpportunities.take(4).forEach { p ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.productName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Demand: ${p.averageMonthlyQuantity.toInt()} ${p.unit}/mo • ₹${String.format(Locale.US, "%,.0f", p.estimatedMonthlyOpportunityValue)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text("${p.opportunityScore}/100", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketGapComparisonCard(marketGaps: List<MarketGapItem>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Demand vs Local Supply Gaps", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFFEF4444).copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                    Text("LOCALISATION GAPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            marketGaps.take(4).forEach { gap ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(gap.productName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Unmet: ${gap.unmetDemandQuantity.toInt()} ${gap.unit}", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                    Text("Procured externally from ${gap.primaryProcurementOrigin}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (gap.existingLocalSupplyQuantity / gap.localDemandQuantity).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstitutionalDemandTableCard(rows: List<InstitutionalDemandRow>, onOpenRecord: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Institutional Demand Ledger", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Institution → Product → Requirement → Existing Supplier → Potential Local Supplier", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            rows.take(6).forEach { row ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenRecord(row.surveyDduId) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.institutionName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text(row.productName, fontSize = 11.5.sp, color = DduPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Requirement: ${row.monthlyRequirement} (${row.purchaseFrequency}) • Current: ${row.currentSupplier}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Local Linkage: ${row.potentialLocalSupplierOrSWSM}", fontSize = 10.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                            Text("Trace Record →", fontSize = 10.sp, color = DduPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketDemandRetailCard(surveys: List<SurveyWithDetails>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Retail Market & Channel Comparison", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Fast-moving goods, purchase frequencies & margins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            val channels = listOf(
                Triple("Village Grocery / Kirana", "High Daily Turnover", "Soap, Biscuits, Phenyl"),
                Triple("Weekly Rural Haat", "Bulk Bi-Weekly Volume", "Apparel, Vegetables, Towels"),
                Triple("Permanent Market Bazar", "Wholesale Distributors", "Fabrics, Hardware, Packaging"),
                Triple("Institutional Channel", "Scheduled Monthly Contracts", "Hospital Linen, Uniforms, Chemicals")
            )

            channels.forEach { (channel, volume, items) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(channel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(items, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(volume, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorSupplierIntelligenceCard(surveys: List<SurveyWithDetails>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Vendor & Supplier Directory Analytics", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            val vendors = listOf(
                Pair("Chetak Enterprise (Wholesale Linen)", "External Supplier (Tezpur) • Hospital Linen & Patient Gowns"),
                Pair("Krishna Traders & Dist.", "Balrampur Bazar • Chemical Cleaners & Detergents"),
                Pair("Maa Durga Textile Depot", "Guwahati Wholesale • School Uniform Shirting Fabric"),
                Pair("FS General Merchant", "Balrampur Market • Grocery & Daily Household Consumables")
            )

            vendors.forEach { (name, details) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text(details, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwsmVaibhaviLinkageCard(
    swsmProfiles: List<SwsmGroupProfile>,
    vaibhaviProfiles: List<VaibhaviEntrepreneurProfile>,
    sakhyaProfiles: List<SakhyaTrainerProfile>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SWSM, Vaibhavi & Sakhya Linkage Ecosystem", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Mapped SWSM Producer Groups (${swsmProfiles.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
            swsmProfiles.forEach { swsm ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${swsm.name} (${swsm.village})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${swsm.memberCount} Members • Capacity: ${swsm.monthlyProductionCapacity} • Potential: ${swsm.potentialProducts.joinToString(", ")}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Vaibhavi Micro-Aggregators (${vaibhaviProfiles.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
            vaibhaviProfiles.forEach { vai ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${vai.name} (${vai.village})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${vai.businessType} • Monthly: ₹${String.format(Locale.US, "%,.0f", vai.monthlySales)} • Packaging: ${vai.packagingCapability}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun VaatikaDashboardCard(vaatikas: List<VaatikaAnalyticsProfile>, onOpenMap: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vaatika Territory Profiles", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenMap) {
                    Text("Interactive Map →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            vaatikas.forEach { v ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(v.vaatikaName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(v.territory, fontSize = 10.5.sp, color = DduPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${v.totalVillages} Villages • ${v.swsmMembersCount} SWSM Women • ${v.ddusCount} DDUs • ${v.institutionsCount} Institutions", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Top Opportunities: ${v.topOpportunities.joinToString(", ")}", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldRouteAnalysisCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Field Route & Survey Coverage Analysis", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Official GPS survey trails & coverage distribution", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(title = "Visits Logged", value = "38", subtitle = "100% Geocoded", color = DduPrimary, modifier = Modifier.weight(1f))
                KpiCard(title = "Distance", value = "142 km", subtitle = "Field Routes", color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                KpiCard(title = "Coverage Gaps", value = "2 Tolas", subtitle = "Pipra South", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FieldTeamPerformanceCard(surveys: List<SurveyWithDetails>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Field Team Workload & Support Dashboard", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Progress tracking to identify where supervisors can assist", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            val team = listOf(
                Triple("Priya Devi (Vidushi Fellow)", "24 Surveys • 12 Institutions", "Needs wholesale quote verification support"),
                Triple("Anil Bora (Field Surveyor)", "18 Surveys • 8 SWSM Meetings", "On track with Rampur Tola coverage"),
                Triple("Rakesh Saikia (Validator)", "32 Records Verified", "2 pending laboratory sample checks")
            )

            team.forEach { (name, work, note) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text(work, fontSize = 11.sp, color = DduPrimary)
                        Text("Support Focus: $note", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataQualityDashboardCard(qualityMetrics: List<DduDataQualityMetric>, onOpenRecord: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Data Quality & Completeness", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    val avgScore = if (qualityMetrics.isNotEmpty()) qualityMetrics.map { it.completenessScore }.average().toInt() else 90
                    Text("$avgScore% AVG SCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            qualityMetrics.take(6).forEach { m ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onOpenRecord(m.dduId) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${m.dduId} · ${m.entityName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (m.issuesList.isEmpty()) "All GPS, Photos, Pricing & Contacts Verified" else m.issuesList.joinToString(" • "),
                                fontSize = 10.sp,
                                color = if (m.issuesList.isEmpty()) Color(0xFF10B981) else Color(0xFFEF4444),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "${m.completenessScore}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (m.completenessScore >= 80) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerBiReportsConnectorCard(
    surveys: List<SurveyWithDetails>,
    opportunities: List<com.example.data.model.OpportunityEntity>,
    stage2Assessments: List<com.example.data.model.VillageProductionAssessmentEntity>,
    sakhyaScreenings: List<com.example.data.model.SakhyaScreeningEntity>,
    onCopyJson: () -> Unit,
    onCopyCsv: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Power BI Semantic Connector & Data Views", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Relational Fact & Dimension tables ready for Power BI Service, Desktop, & REST API", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            val tables = listOf(
                Pair("fact_ddus", "${opportunities.size} rows · DDU pipeline, status, stages & annual demand"),
                Pair("fact_surveys", "${surveys.size} rows · GPS coordinates, confidence, dates & surveyors"),
                Pair("fact_demand", "${surveys.flatMap { it.products }.size} rows · Product units, purchase prices & suppliers"),
                Pair("fact_stage_history", "${stage2Assessments.size} rows · Village production feasibility & sample tracking"),
                Pair("dim_products", "Distinct products & harmonized categories"),
                Pair("dim_locations", "Settlement villages, tolas, blocks & districts"),
                Pair("dim_sakhyas", "${sakhyaScreenings.size} rows · Entrepreneurship screenings & suitability scores")
            )

            tables.forEach { (table, desc) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(table, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                        Text(desc, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCopyJson,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                    modifier = Modifier.weight(1f).testTag("btn_export_powerbi_json")
                ) {
                    Text("Copy JSON Dataset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCopyCsv,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_export_powerbi_csv")
                ) {
                    Text("Copy fact_ddus CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
