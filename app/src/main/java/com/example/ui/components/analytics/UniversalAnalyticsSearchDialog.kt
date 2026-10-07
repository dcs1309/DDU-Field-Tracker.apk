package com.example.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.AiAnalyticsInsightService
import com.example.data.analytics.AiInsightAnswer
import com.example.data.model.OpportunityEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.ui.theme.DduPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalAnalyticsSearchDialog(
    surveys: List<SurveyWithDetails>,
    opportunities: List<OpportunityEntity>,
    stage2Assessments: List<VillageProductionAssessmentEntity>,
    sakhyaScreenings: List<SakhyaScreeningEntity>,
    onNavigateToRecord: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var activeAnswer by remember { mutableStateOf<AiInsightAnswer?>(null) }

    val presetQueries = listOf(
        "Which products have repeated institutional demand but no local supplier?",
        "Which DDU has been stuck in Stage 1 for the longest?",
        "Which hospital products show local sourcing potential?",
        "Which SWSM groups could potentially supply existing institutional demand?",
        "Show cleaning product opportunities in Balipara.",
        "Which products have samples pending?"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = DduPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Universal Analytics & AI Assistant",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grounded on live DDU database with Explainable Evidence",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) {
                        activeAnswer = AiAnalyticsInsightService.answerQuery(
                            query = it,
                            surveys = surveys,
                            opportunities = opportunities,
                            stage2Assessments = stage2Assessments,
                            sakhyaScreenings = sakhyaScreenings
                        )
                    } else {
                        activeAnswer = null
                    }
                },
                placeholder = { Text("Ask question or search product / village (e.g. 'Phenyl', 'Khaliamari')...", fontSize = 12.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DduPrimary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; activeAnswer = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("universal_analytics_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Prompt Chips
            Text("Sample Operational Queries:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(presetQueries) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            query = prompt
                            activeAnswer = AiAnalyticsInsightService.answerQuery(
                                query = prompt,
                                surveys = surveys,
                                opportunities = opportunities,
                                stage2Assessments = stage2Assessments,
                                sakhyaScreenings = sakhyaScreenings
                            )
                        }
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Results / AI Answer Area
            activeAnswer?.let { ans ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, DduPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ans.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${ans.confidence} CONFIDENCE",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = ans.summary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }

                        item {
                            Text("Supporting Evidence Points:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                        }

                        items(ans.evidencePoints) { pt ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = DduPrimary, fontWeight = FontWeight.Bold)
                                Text(pt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Requirement #49: Explainable Analytics ("WHY THIS INSIGHT APPEARED")
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("WHY THIS INSIGHT APPEARED", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ans.whyThisInsightAppeared, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Recommended Next Action", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        Text(ans.recommendedAction, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        if (ans.relatedDduIds.isNotEmpty()) {
                            item {
                                Text("Traceable Field Records:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            items(ans.relatedDduIds) { dduId ->
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onNavigateToRecord(dduId)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Open $dduId Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Search anything across DDU field database or pick a question above", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
