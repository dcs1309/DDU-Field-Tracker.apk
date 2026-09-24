package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OpportunityEntity
import com.example.data.model.OpportunityStage
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.ui.components.CollatedExportDialog
import com.example.ui.components.ConfidenceBadge
import com.example.ui.theme.DduPrimary
import com.example.ui.theme.DduPrimaryContainer
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateToRecordDetail: (String) -> Unit,
    onNavigateToOpportunityGraph: () -> Unit = {},
    onNavigateToStage2Assessment: (String?, String?) -> Unit = { _, _ -> },
    onNavigateToStage2DduSelection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val opportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val allSurveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()
    val aliases by viewModel.allAliases.collectAsStateWithLifecycle()
    val isStage2Unlocked by viewModel.isStage2Unlocked.collectAsStateWithLifecycle()
    val stage2Assessments by viewModel.allStage2Assessments.collectAsStateWithLifecycle()
    val shortlistedOpps by viewModel.shortlistedOpportunities.collectAsStateWithLifecycle()
    val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val sakhyaScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()
    val stats by viewModel.statistics.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Stage 1 Opps", "Stage 2 Village", "Village Demand", "Smart Matching", "Export Reports")

    var selectedOppDetail by remember { mutableStateOf<OpportunityEntity?>(null) }
    var showCollatedExportDialog by remember { mutableStateOf(false) }

    if (showCollatedExportDialog) {
        CollatedExportDialog(
            userProfile = userProfile,
            surveys = allSurveys,
            opportunities = opportunities,
            stage2Assessments = stage2Assessments,
            sakhyaScreenings = sakhyaScreenings,
            stats = stats,
            onDismiss = { showCollatedExportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Collective Synthesis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "From Field Data to Verified Opportunities",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToOpportunityGraph,
                        modifier = Modifier.testTag("action_open_network_graph")
                    ) {
                        Icon(Icons.Default.Hub, contentDescription = "Opportunity Network Graph", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showCollatedExportDialog = true },
                        modifier = Modifier.testTag("export_reports_action")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Collated Reports & Share", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Synthesis Philosophy Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rule: Connect information across institutions & shops — Do not merely store data.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> OpportunitiesTabContent(
                    opportunities = opportunities,
                    onSelectOpportunity = { selectedOppDetail = it },
                    onAdvanceStage = { opp, nextStage ->
                        viewModel.updateOpportunityStage(opp.oppId, nextStage)
                        Toast.makeText(context, "Pipeline updated to $nextStage", Toast.LENGTH_SHORT).show()
                    },
                    onToggleShortlist = { opp ->
                        viewModel.toggleShortlistOpportunity(opp.oppId, opp.isShortlistedForStage2)
                        val msg = if (!opp.isShortlistedForStage2) "Shortlisted! Stage 2 Unlocked." else "Removed from shortlist"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onAssessStage2 = { opp ->
                        if (!opp.isShortlistedForStage2) {
                            viewModel.shortlistOpportunity(opp.oppId)
                        }
                        onNavigateToStage2Assessment(null, opp.oppId)
                    },
                    onDrillDownToSource = { opp ->
                        val matching = allSurveys.firstOrNull { it.products.any { p -> opp.title.contains(p.productName, ignoreCase = true) } }
                        if (matching != null) {
                            onNavigateToRecordDetail(matching.survey.dduId)
                        } else {
                            onNavigateToRecordDetail("DDU-BAL-2026-000124")
                        }
                    }
                )

                1 -> Stage2VillageTabContent(
                    isStage2Unlocked = isStage2Unlocked,
                    shortlistedOpps = shortlistedOpps,
                    stage2Assessments = stage2Assessments,
                    onUnlockStage2 = {
                        opportunities.take(2).forEach { opp ->
                            viewModel.shortlistOpportunity(opp.oppId)
                        }
                        Toast.makeText(context, "Shortlisted top opportunities! Stage 2 Unlocked.", Toast.LENGTH_SHORT).show()
                    },
                    onStartAssessment = { oppId ->
                        onNavigateToStage2Assessment(null, oppId)
                    },
                    onEditAssessment = { assessmentId ->
                        onNavigateToStage2Assessment(assessmentId, null)
                    },
                    onOpenDduSelection = onNavigateToStage2DduSelection,
                    onSyncFirestore = {
                        viewModel.syncWithFirestore { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )

                2 -> VillageDemandTabContent(surveys = allSurveys)

                3 -> SmartMatchingTabContent(
                    viewModel = viewModel,
                    aliasesCount = aliases.size
                )

                4 -> ExportReportsTabContent(
                    onExportClick = { showCollatedExportDialog = true }
                )
            }
        }
    }
}

@Composable
fun OpportunitiesTabContent(
    opportunities: List<OpportunityEntity>,
    onSelectOpportunity: (OpportunityEntity) -> Unit,
    onAdvanceStage: (OpportunityEntity, String) -> Unit,
    onToggleShortlist: (OpportunityEntity) -> Unit,
    onAssessStage2: (OpportunityEntity) -> Unit,
    onDrillDownToSource: (OpportunityEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Identified DDU Opportunities (${opportunities.size})",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aggregated demand exceeding local supply thresholds • Shortlist to unlock Stage 2 Village Assessment",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Visual Network Graph representation
        item {
            EnterpriseRelationshipGraphCard()
        }

        items(opportunities) { opp ->
            OpportunityDetailCard(
                opp = opp,
                onAdvanceStage = { nextStage -> onAdvanceStage(opp, nextStage) },
                onToggleShortlist = { onToggleShortlist(opp) },
                onAssessStage2 = { onAssessStage2(opp) },
                onDrillDown = { onDrillDownToSource(opp) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun EnterpriseRelationshipGraphCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balrampur Supply & Demand Network",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFFE0F2F1),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "LIVE GRAPH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DduPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas drawing network nodes
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val width = size.width
                val height = size.height

                val institutionPoint = Offset(width * 0.15f, height * 0.35f)
                val shopPoint = Offset(width * 0.15f, height * 0.75f)
                val productPoint = Offset(width * 0.5f, height * 0.5f)
                val supplierPoint = Offset(width * 0.85f, height * 0.25f)
                val opportunityPoint = Offset(width * 0.85f, height * 0.75f)

                // Draw connecting lines
                drawLine(
                    color = Color(0xFF80CBC4),
                    start = institutionPoint,
                    end = productPoint,
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF80CBC4),
                    start = shopPoint,
                    end = productPoint,
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFFFFCC80),
                    start = productPoint,
                    end = supplierPoint,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawLine(
                    color = Color(0xFFBA68C8),
                    start = productPoint,
                    end = opportunityPoint,
                    strokeWidth = 4f
                )

                // Draw Nodes
                drawCircle(color = Color(0xFF00796B), radius = 14f, center = institutionPoint)
                drawCircle(color = Color(0xFF00796B), radius = 14f, center = shopPoint)
                drawCircle(color = Color(0xFF004D40), radius = 18f, center = productPoint)
                drawCircle(color = Color(0xFFE65100), radius = 14f, center = supplierPoint)
                drawCircle(color = Color(0xFF7B1FA2), radius = 16f, center = opportunityPoint)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Buyers (Institutions)", fontSize = 10.sp, color = DduPrimary, fontWeight = FontWeight.Bold)
                Text("Product Demand", fontSize = 10.sp, color = Color(0xFF004D40), fontWeight = FontWeight.Bold)
                Text("External Source → DDU Opp", fontSize = 10.sp, color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OpportunityDetailCard(
    opp: OpportunityEntity,
    onAdvanceStage: (String) -> Unit,
    onToggleShortlist: () -> Unit,
    onAssessStage2: () -> Unit,
    onDrillDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Opp ID & Stage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF7B1FA2),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = opp.oppId,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = opp.level + " VIABILITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (opp.level == "HIGH") Color(0xFF2E7D32) else Color(0xFFF57C00)
                    )
                }

                Surface(
                    color = Color(0xFFEDE7F6),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = opp.status.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A148C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = opp.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Demand: ${opp.demandingEntitiesCount} entities • Est: ${opp.estimatedAnnualDemand}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DduPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Current sourcing vs Local opportunity
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Current Source: ${opp.currentSource}",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Price Range: ${opp.averagePriceRange}",
                        fontSize = 11.sp,
                        color = Color(0xFFB25E00)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable full narrative
            Text(
                text = opp.interpretationNarrative,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (expanded) "Show less ⌃" else "Read full synthesis narrative ⌄",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DduPrimary,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Potential DDU Enterprise: ${opp.potentialEnterprise}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00796B)
                    )
                    Text(
                        text = "Actionable Next Steps:\n${opp.nextSteps}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Responsible: ${opp.responsiblePerson} • Follow-up: ${opp.followUpDate}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDrillDown,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Source Records", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val stages = OpportunityStage.values()
                        val currentIndex = stages.indexOfFirst { it.name == opp.status }
                        val nextStage = if (currentIndex in 0 until stages.size - 2) stages[currentIndex + 1].name else OpportunityStage.ACTIVE_OPPORTUNITY.name
                        onAdvanceStage(nextStage)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Advance Pipeline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleShortlist,
                    shape = RoundedCornerShape(8.dp),
                    colors = if (opp.isShortlistedForStage2) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE8F5E9)) else ButtonDefaults.outlinedButtonColors(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (opp.isShortlistedForStage2) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (opp.isShortlistedForStage2) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (opp.isShortlistedForStage2) "Shortlisted (Stage 2)" else "Shortlist for Stage 2",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (opp.isShortlistedForStage2) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primary
                    )
                }

                if (opp.isShortlistedForStage2) {
                    Button(
                        onClick = onAssessStage2,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Factory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Assess Stage 2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun Stage2VillageTabContent(
    isStage2Unlocked: Boolean,
    shortlistedOpps: List<OpportunityEntity>,
    stage2Assessments: List<VillageProductionAssessmentEntity>,
    onUnlockStage2: () -> Unit,
    onStartAssessment: (String?) -> Unit,
    onEditAssessment: (String) -> Unit,
    onOpenDduSelection: () -> Unit,
    onSyncFirestore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isStage2Unlocked) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Stage 2 · Village Production Assessment",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Stage 2 answers the core question: \"Can our village produce this, competitively?\" evaluating raw materials, priority level, mandatory criticality, sampling, and village readiness.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onUnlockStage2,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Stage 2 (Shortlist Top Products)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Stage 2 Unlocked Header & Dashboard
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STAGE 2 UNLOCKED & ACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            FilledTonalButton(
                                onClick = onSyncFirestore,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Cloud", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stage 1 opportunities have been shortlisted into the village production pipeline.",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenDduSelection,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DDU Matrix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onStartAssessment(null) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assess New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Summary Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val passCount = stage2Assessments.count { it.criticalityRatingPass }
                    val directSampleCount = stage2Assessments.count { it.samplingChecked }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Assessed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stage2Assessments.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Direct Sample", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            Text("$directSampleCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Pass Critical", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text("$passCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Shortlisted Queue Ready for Assessment
            val unassessedShortlisted = shortlistedOpps.filter { opp ->
                stage2Assessments.none { it.stage1OppId == opp.oppId || it.productName.contains(opp.title, ignoreCase = true) }
            }

            if (unassessedShortlisted.isNotEmpty()) {
                item {
                    Text(
                        text = "Shortlisted for Stage 2 Queue (${unassessedShortlisted.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(unassessedShortlisted) { opp ->
                    OutlinedCard(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opp.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${opp.category} • Annual Demand: ${opp.estimatedAnnualDemand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { onStartAssessment(opp.oppId) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Assess Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Completed Assessments List
            item {
                Text(
                    text = "Completed Village Production Assessments (${stage2Assessments.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(stage2Assessments) { item ->
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditAssessment(item.assessmentId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Vatika: ${item.vatika} • Assessed: ${item.assessmentDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!item.criticalityRatingPass) MaterialTheme.colorScheme.error
                                else if (item.samplingChecked) Color(0xFF2E7D32)
                                else Color(0xFF1976D2)
                            ) {
                                Text(
                                    text = if (!item.criticalityRatingPass) "FAIL"
                                    else if (item.samplingChecked) "SAMPLE READY"
                                    else "READINESS ${item.readinessScore}/7",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Priority ${item.priorityLevel}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Criticality: ${if (item.criticalityRatingPass) "Pass" else "Fail"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (item.criticalityRatingPass) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                            Text("Selection: ${item.finalSelectionStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (item.swsmDecisionNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notes: ${item.swsmDecisionNotes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun VillageDemandTabContent(surveys: List<com.example.data.model.SurveyWithDetails>) {
    val villages = listOf("Rampur Tola", "Juri", "Balrampur Bazar", "Grazi", "Harijan Basti")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Village-Level Demand Profiles",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aggregation of institutional and retail gaps by geographic cluster",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(villages) { villageName ->
            val matching = surveys.filter { it.survey.village.contains(villageName, ignoreCase = true) }
            val productsCount = matching.flatMap { it.products }.size

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = villageName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFFE0F2F1),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${matching.size} Entities Surveyed",
                                fontSize = 11.sp,
                                color = DduPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Identified Unmet Demands ($productsCount items):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    matching.flatMap { it.products }.take(3).forEach { prod ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(DduPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${prod.productName} (${prod.minQuantity.toInt()} - ${prod.maxQuantity.toInt()} ${prod.unit}, ${prod.buyingFrequency})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            // Seasonal Demand Peak Calendar
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Seasonal Demand Surge Calendar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• April - June: School Uniforms & Hostel linen surge (Admissions peak)", fontSize = 12.sp)
                    Text("• November - January: Paper Plates & Napkins surge (Weddings & fairs)", fontSize = 12.sp)
                    Text("• July - August: White cotton hospital linens surge (Monsoon hospital admissions)", fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun SmartMatchingTabContent(
    viewModel: FieldIntelligenceViewModel,
    aliasesCount: Int
) {
    val context = LocalContext.current
    var aliasStandard by remember { mutableStateOf("School Uniform") }
    var aliasInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Smart Matching & Normalisation",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Standardize spelling variations to prevent fragmented demand aggregation",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Detected Potential Product Matches",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val variations = listOf(
                        Triple("School Uniform", "School dress", "Kasturba Hostel survey"),
                        Triple("Liquid Phynyl", "White phenyl 5L", "Times Clinic survey"),
                        Triple("Hospital Bedsheet", "White cotton sheet", "Dorika Hospital survey")
                    )

                    variations.forEach { (standard, variation, source) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "\"$variation\" → $standard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Reported in $source", fontSize = 11.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    viewModel.addProductAlias(standard, variation)
                                    Toast.makeText(context, "Merged into standard '$standard'", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Merge", fontSize = 11.sp)
                            }
                        }
                        Divider()
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Add Custom Product Alias Rule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = aliasStandard,
                        onValueChange = { aliasStandard = it },
                        label = { Text("Standard Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        label = { Text("Alias / Local Spelling") },
                        placeholder = { Text("e.g. Uniform set") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (aliasInput.isNotBlank()) {
                                viewModel.addProductAlias(aliasStandard, aliasInput)
                                aliasInput = ""
                                Toast.makeText(context, "Alias Rule Added", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Alias Rule")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun ExportReportsTabContent(onExportClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Collated Field Intelligence Export",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Export and share verified field data across PDF, PPT, and Excel via WhatsApp or Email",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // WhatsApp & Email Quick Share Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF86EFAC)
                        ) {
                            Text(
                                text = "WHATSAPP & EMAIL READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instant 1-Tap Collated Export",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generates a comprehensive compilation uniting Surveys, Stage 1 Opportunities, Stage 2 Village Feasibility, and Sakhya Form F1 records ready to dispatch to district authorities.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Export & Share Hub", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Format 1: Multi-Page PDF
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.15f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.padding(6.dp).size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Executive Briefing PDF (4 Pages)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Multi-page visual document with cover, KPI grid, and signatures", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Includes: Page 1 Executive KPI Dashboard, Page 2 Stage 1 Demand Matrix, Page 3 Stage 2 Village Production Viability & SWSM status, Page 4 Sakhya MEG Register & Official Endorsements.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Share PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Format 2: Structured Excel (.xls)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.15f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.padding(6.dp).size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Master Collated Excel Workbook (.xls)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Multi-sheet spreadsheet format for Microsoft Excel & Google Sheets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Structured across 5 distinct tabs: Executive_Summary, Field_Surveys, Stage1_Opportunities, Stage2_Village_Assessment, and Sakhya_Enterprises.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Share Excel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Format 3: Presentation Deck (.ppt)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEA580C).copy(alpha = 0.15f)
                        ) {
                            Icon(Icons.Default.Slideshow, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.padding(6.dp).size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Executive PowerPoint Presentation (.ppt)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "6-Slide presentation deck ready for stakeholder briefings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Formatted slides: Title Deck, Executive Summary KPIs, Stage 1 Opportunities, Stage 2 Feasibility, Sakhya MEG Register, and Strategic Roadmap.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Share Presentation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
