package com.example.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.model.ProductOpportunityMetric
import com.example.data.model.SurveyWithDetails
import com.example.ui.components.opportunity.OpportunityLineageHelper
import com.example.ui.components.opportunity.OpportunitySourceRecordsDialog
import com.example.ui.theme.DduPrimary
import java.util.Locale

@Composable
fun DynamicOpportunityCard(
    opportunity: ProductOpportunityMetric,
    onClickRecord: (String) -> Unit,
    allSurveys: List<SurveyWithDetails> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showLineageDialog by remember { mutableStateOf(false) }

    val supportingSurveys = remember(opportunity, allSurveys) {
        OpportunityLineageHelper.getSupportingDataForProductOpportunity(opportunity, allSurveys)
    }
    val totalPhotos = remember(supportingSurveys) {
        supportingSurveys.flatMap { it.evidenceList }.count { it.type == "PHOTO" || it.type == "DOCUMENT" }
    }
    val totalVoiceNotes = remember(supportingSurveys) {
        supportingSurveys.flatMap { it.evidenceList }.count { it.type == "VOICE_NOTE" }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("opportunity_card_${opportunity.productName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title, Category & Opportunity Score Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opportunity.productName.uppercase(Locale.ROOT),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${opportunity.category} • ${if (opportunity.isExternalSupplier) "External Sourcing Arbitrage" else "Local Production Expansion"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DduPrimary
                    )
                }

                // DDU Opportunity Score Badge (0-100)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (opportunity.scoreCategory) {
                        "High Potential" -> Color(0xFF10B981).copy(alpha = 0.15f)
                        "Moderate Potential" -> Color(0xFF0284C7).copy(alpha = 0.15f)
                        "Needs Validation" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFF64748B).copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (opportunity.scoreCategory) {
                            "High Potential" -> Color(0xFF10B981)
                            "Moderate Potential" -> Color(0xFF0284C7)
                            "Needs Validation" -> Color(0xFFF59E0B)
                            else -> Color(0xFF64748B)
                        }
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${opportunity.opportunityScore}/100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = when (opportunity.scoreCategory) {
                                "High Potential" -> Color(0xFF10B981)
                                "Moderate Potential" -> Color(0xFF0284C7)
                                "Needs Validation" -> Color(0xFFF59E0B)
                                else -> Color(0xFF64748B)
                            }
                        )
                        Text(
                            text = opportunity.scoreCategory,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Grid of traceable field metrics
            Row(modifier = Modifier.fillMaxWidth()) {
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Location",
                    value = "Balrampur Block (${opportunity.villagesCount} villages)"
                )
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Potential Buyers",
                    value = "${opportunity.institutionsDemandingCount} Inst. + ${opportunity.retailersDemandingCount} Retail"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Estimated Demand",
                    value = "${opportunity.averageMonthlyQuantity.toInt()} ${opportunity.unit}/month"
                )
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Current Supplier",
                    value = opportunity.currentSupplierLocation
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Avg. Procurement Distance",
                    value = if (opportunity.isExternalSupplier) "45–60 km (Outside Block)" else "Local (<5 km)"
                )
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Local Producer Available",
                    value = if (opportunity.swsmGroupsCapableCount > 0) "Yes (${opportunity.swsmGroupsCapableCount} SWSM, ${opportunity.vaibhavisCapableCount} Vaibhavis)" else "Requires Training"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Estimated Local Value",
                    value = "₹${String.format(Locale.US, "%,.0f", opportunity.estimatedMonthlyOpportunityValue)}/month"
                )
                OpportunityAttributeColumn(
                    modifier = Modifier.weight(1f),
                    label = "DDU Stage & Confidence",
                    value = "Stage 2 · ${opportunity.confidence} Confidence"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Next Action strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = DduPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Next Action: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DduPrimary
                    )
                    Text(
                        text = "Sample Testing & Buyer Approval (${opportunity.sampleStatus})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary 'View Source Records' button linking directly to supporting evidence
            Button(
                onClick = { showLineageDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_view_source_records_${opportunity.productName}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DduPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Source,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Source Records",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${supportingSurveys.size} Surveys • $totalPhotos Photos • $totalVoiceNotes Audio",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to trace data lineage to surveys, photos, and voice notes
    if (showLineageDialog) {
        OpportunitySourceRecordsDialog(
            opportunityId = opportunity.associatedDduId ?: "DDU-OPP-${opportunity.productName.take(3).uppercase(Locale.ROOT)}",
            opportunityTitle = opportunity.productName,
            category = opportunity.category,
            confidenceLevel = opportunity.confidence,
            matchingSurveys = supportingSurveys,
            onNavigateToRecord = { dduId ->
                showLineageDialog = false
                onClickRecord(dduId)
            },
            onDismiss = { showLineageDialog = false }
        )
    }
}

@Composable
private fun OpportunityAttributeColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
