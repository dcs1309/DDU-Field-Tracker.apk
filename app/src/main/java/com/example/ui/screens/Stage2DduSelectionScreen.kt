package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.ui.components.ExportTargetRecord
import com.example.ui.components.SingleRecordExportDialog
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stage2DduSelectionScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewAssessment: (String?) -> Unit,
    onNavigateToEditAssessment: (String) -> Unit
) {
    val context = LocalContext.current
    val assessments by viewModel.allStage2Assessments.collectAsStateWithLifecycle()
    val shortlistedOpps by viewModel.shortlistedOpportunities.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    // Filter chip: All, Selected, Needs Support, Dropped
    var filterStatus by remember { mutableStateOf("ALL") }
    var exportTarget by remember { mutableStateOf<VillageProductionAssessmentEntity?>(null) }

    val sortedList = remember(assessments, filterStatus) {
        val base = assessments.sortedWith(
            compareBy<VillageProductionAssessmentEntity> { !it.criticalityRatingPass }
                .thenBy { it.priorityLevel }
                .thenByDescending { it.samplingChecked }
                .thenByDescending { it.readinessScore }
        )
        when (filterStatus) {
            "SELECTED" -> base.filter { it.criticalityRatingPass && (it.samplingChecked || it.readinessScore >= 5) }
            "SUPPORT" -> base.filter { it.criticalityRatingPass && !it.samplingChecked && it.readinessScore in 3..4 }
            "DROPPED" -> base.filter { !it.criticalityRatingPass }
            else -> base
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stage 2 · DDU Selection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Final Product List for DDU Selection",
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
                    if (assessments.isNotEmpty()) {
                        IconButton(
                            onClick = { exportTarget = sortedList.firstOrNull() ?: assessments.first() },
                            modifier = Modifier.testTag("action_export_stage2")
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Export DDU 2 Assessment (PDF/Excel/PPTM)",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.syncWithFirestore { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.testTag("action_sync_firestore_stage2")
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Sync Cloud Firestore",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToNewAssessment(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Assess Product", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_stage2_assessment")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Guidelines Card from PDF Page 3
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HOW TO SELECT FOR DDU (STAGE 2 MATRIX)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Order products by Priority Ranking (1 = highest).\n" +
                                    "2. Drop any product where Criticality Rating is Fail — it goes no further.\n" +
                                    "3. For the remaining products, check Sampling Checked. If Yes, mark Final Selection as Selected — Readiness Ranking is not needed.\n" +
                                    "4. If Sampling Checked is No, complete Readiness Ranking and use it to decide Final Selection.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Sync Status Banner
            if (isSyncing) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = syncMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Shortlisted Opportunities Ready for Assessment (Unlocking Queue)
            val unassessedShortlisted = shortlistedOpps.filter { opp ->
                assessments.none { it.stage1OppId == opp.oppId || it.productName.contains(opp.title, ignoreCase = true) }
            }

            if (unassessedShortlisted.isNotEmpty()) {
                item {
                    Text(
                        text = "Shortlisted Stage 1 Queue (${unassessedShortlisted.size} ready for assessment)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        unassessedShortlisted.forEach { opp ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToNewAssessment(opp.oppId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = opp.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${opp.category} • ${opp.demandingEntitiesCount} demanding institutions",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onNavigateToNewAssessment(opp.oppId) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Assess", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == "ALL",
                        onClick = { filterStatus = "ALL" },
                        label = { Text("All (${assessments.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "SELECTED",
                        onClick = { filterStatus = "SELECTED" },
                        label = { Text("Selected", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "SUPPORT",
                        onClick = { filterStatus = "SUPPORT" },
                        label = { Text("Support", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == "DROPPED",
                        onClick = { filterStatus = "DROPPED" },
                        label = { Text("Dropped", fontSize = 11.sp) }
                    )
                }
            }

            if (sortedList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Stage 2 assessments in this filter",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(sortedList) { index, item ->
                    Stage2DecisionItemCard(
                        rankNumber = index + 1,
                        assessment = item,
                        onClick = { onNavigateToEditAssessment(item.assessmentId) },
                        onDelete = { viewModel.deleteStage2Assessment(item) },
                        onExport = { exportTarget = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (exportTarget != null) {
        val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
        SingleRecordExportDialog(
            targetRecord = ExportTargetRecord.Stage2Record(exportTarget!!),
            userProfile = userProfile,
            onDismiss = { exportTarget = null }
        )
    }
}

@Composable
private fun Stage2DecisionItemCard(
    rankNumber: Int,
    assessment: VillageProductionAssessmentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    val isCriticalPass = assessment.criticalityRatingPass
    val isSelected = isCriticalPass && (assessment.samplingChecked || assessment.readinessScore >= 5)

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (!isCriticalPass) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else if (isSelected) Color(0xFF2E7D32).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Rank Badge, Title, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "#$rankNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = assessment.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isCriticalPass) MaterialTheme.colorScheme.error
                    else if (assessment.samplingChecked) Color(0xFF2E7D32)
                    else if (assessment.readinessScore >= 6) Color(0xFF1B5E20)
                    else Color(0xFFF57F17)
                ) {
                    Text(
                        text = if (!isCriticalPass) "FAIL" else if (assessment.samplingChecked) "SELECTED (SAMPLE)" else "READINESS ${assessment.readinessScore}/7",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Decision Table Grid (Matching Page 3 Table columns)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Priority", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("P${assessment.priorityLevel}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Criticality", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isCriticalPass) "Pass" else "Fail", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isCriticalPass) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                }
                Column {
                    Text("Sample", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (assessment.samplingChecked) "Yes" else "No", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Readiness", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${assessment.readinessScore}/7", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Final Selection", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isSelected) "Selected" else if (!isCriticalPass) "Dropped" else "Support Needed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (assessment.swsmDecisionNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SWSM Notes: ${assessment.swsmDecisionNotes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vatika: ${assessment.vatika} • ${assessment.assessmentDate}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onExport, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export Record",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
