package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.OpportunityStage
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityDetailScreen(
    oppId: String,
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (String) -> Unit,
    onNavigateToStage2Assessment: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val opportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val allSurveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()

    val opp = opportunities.find { it.oppId == oppId } ?: opportunities.firstOrNull()

    if (opp == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DduPrimary)
        }
        return
    }

    val stages = listOf(
        "IDENTIFIED",
        "EVIDENCE_COLLECTED",
        "PATTERN_CONFIRMED",
        "VALIDATION_REQUIRED",
        "FEASIBILITY_STUDY",
        "PILOT_OPPORTUNITY",
        "ACTIVE_OPPORTUNITY"
    )
    val currentStageIndex = stages.indexOf(opp.status).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF7B1FA2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = opp.oppId,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Opportunity Dossier", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFF3E5F5),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = opp.category,
                                    fontSize = 11.sp,
                                    color = Color(0xFF7B1FA2),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${opp.level} VIABILITY",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = opp.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Aggregated Demand: ${opp.demandingEntitiesCount} entities • ${opp.estimatedAnnualDemand}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DduPrimary
                        )
                        Text(
                            text = "Buying Pattern: ${opp.buyingPattern}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Current Supply: ${opp.currentSource}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pipeline Stage Tracker
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Opportunity Pipeline Stage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        stages.forEachIndexed { index, stageName ->
                            val isCompleted = index <= currentStageIndex
                            val isCurrent = index == currentStageIndex

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isCompleted) Color(0xFF7B1FA2) else Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text("${index + 1}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stageName.replace("_", " "),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Synthesis Narrative
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EE)),
                    border = BorderStroke(1.dp, Color(0xFFB2DFDB)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FIELD INTELLIGENCE SYNTHESIS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DduPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = opp.interpretationNarrative,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Proposed Local Enterprise Model
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Proposed DDU Enterprise Model",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = opp.potentialEnterprise,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Demanding Entities / Confirmed Buyers:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = opp.relatedBuyers,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stage 2 Village Production Assessment Gate
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (opp.isShortlistedForStage2) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (opp.isShortlistedForStage2) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Factory,
                                    contentDescription = null,
                                    tint = if (opp.isShortlistedForStage2) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STAGE 2 VILLAGE ASSESSMENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (opp.isShortlistedForStage2) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (opp.isShortlistedForStage2) Color(0xFF2E7D32) else Color.Gray
                            ) {
                                Text(
                                    text = if (opp.isShortlistedForStage2) "STAGE 2 UNLOCKED" else "STAGE 1 ONLY",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = if (opp.isShortlistedForStage2) {
                                "This opportunity has been shortlisted! Stage 2 Village Production Assessment is unlocked to evaluate raw material, priority, criticality, and readiness."
                            } else {
                                "Shortlist this product to unlock Stage 2 Village Production Assessment and verify village manufacturing viability."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.toggleShortlistOpportunity(opp.oppId, opp.isShortlistedForStage2)
                                    val msg = if (!opp.isShortlistedForStage2) "Shortlisted! Stage 2 Unlocked." else "Removed from Stage 2 shortlist"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
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
                                    text = if (opp.isShortlistedForStage2) "Shortlisted" else "Shortlist",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    if (!opp.isShortlistedForStage2) {
                                        viewModel.shortlistOpportunity(opp.oppId)
                                    }
                                    onNavigateToStage2Assessment(opp.oppId)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (opp.isShortlistedForStage2) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Assess Stage 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (currentStageIndex < stages.size - 1) {
                                val next = stages[currentStageIndex + 1]
                                viewModel.updateOpportunityStage(opp.oppId, next)
                                Toast.makeText(context, "Advanced to ${next.replace("_", " ")}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Advance Pipeline Stage", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
