package com.example.ui.components.opportunity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EvidenceEntity
import com.example.data.model.SurveyWithDetails
import com.example.ui.theme.DduPrimary
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Data Lineage and Supporting Evidence Modal Sheet / Dialog.
 * Allows tracing from an identified DDU Opportunity directly back to:
 * - Validated Survey Records
 * - Field Photographs & Invoices
 * - Voice Notes & Recorded Respondent Interviews (with Audio Playback & Transcriptions)
 * - Complete Traceability Audit Trail
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitySourceRecordsDialog(
    opportunityId: String,
    opportunityTitle: String,
    category: String,
    confidenceLevel: String,
    matchingSurveys: List<SurveyWithDetails>,
    onNavigateToRecord: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All Lineage", "Surveys (${matchingSurveys.size})", "Photos", "Voice Notes", "Audit Trail")
    var selectedPhotoEvidence by remember { mutableStateOf<EvidenceEntity?>(null) }

    // Aggregate evidence from matching surveys
    val allEvidence = remember(matchingSurveys) {
        matchingSurveys.flatMap { it.evidenceList }
    }
    val photoEvidence = remember(allEvidence) {
        allEvidence.filter { it.type == "PHOTO" || it.type == "DOCUMENT" }
    }
    val voiceNotes = remember(allEvidence) {
        allEvidence.filter { it.type == "VOICE_NOTE" }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dialog_opportunity_source_records")
            ) {
                // Header Bar with Lineage Badge & Close Button
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF7B1FA2),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = opportunityId,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$confidenceLevel CONFIDENCE LINEAGE",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = opportunityTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Data Lineage & Verified Field Evidence • $category",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("btn_close_lineage_dialog")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Summary Statistics Metric Strip
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricCountItem(count = "${matchingSurveys.size}", label = "Surveys", icon = Icons.Default.Description, color = DduPrimary)
                        MetricCountItem(count = "${photoEvidence.size}", label = "Photos & Bills", icon = Icons.Default.PhotoLibrary, color = Color(0xFF0284C7))
                        MetricCountItem(count = "${voiceNotes.size}", label = "Voice Notes", icon = Icons.Default.Mic, color = Color(0xFFEA580C))
                        MetricCountItem(
                            count = "${matchingSurveys.map { it.survey.village }.distinct().size}",
                            label = "Villages",
                            icon = Icons.Default.Place,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }

                // Navigation Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = DduPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Lineage Flow Breadcrumb (Shown on Tab 0 or 4)
                    if (selectedTab == 0 || selectedTab == 4) {
                        item {
                            DataLineageFlowCard(
                                opportunityId = opportunityId,
                                surveysCount = matchingSurveys.size,
                                photosCount = photoEvidence.size,
                                voiceCount = voiceNotes.size,
                                confidenceLevel = confidenceLevel
                            )
                        }
                    }

                    // TAB 0 (All Lineage) or TAB 1 (Surveys)
                    if (selectedTab == 0 || selectedTab == 1) {
                        item {
                            SectionHeader(
                                title = "Validated Survey Records",
                                subtitle = "Primary field records proving institutional and shop demand",
                                count = matchingSurveys.size
                            )
                        }

                        if (matchingSurveys.isEmpty()) {
                            item {
                                EmptyEvidenceCard(
                                    message = "No specific survey matched to this filter yet.",
                                    icon = Icons.Default.SearchOff
                                )
                            }
                        } else {
                            items(matchingSurveys) { item ->
                                LineageSurveyRecordCard(
                                    surveyWithDetails = item,
                                    onOpenRecord = onNavigateToRecord
                                )
                            }
                        }
                    }

                    // TAB 0 (All Lineage) or TAB 2 (Photographs)
                    if (selectedTab == 0 || selectedTab == 2) {
                        item {
                            SectionHeader(
                                title = "Photographic & Documentary Evidence",
                                subtitle = "Geo-tagged field photos, product sizing samples, and vendor bills",
                                count = photoEvidence.size
                            )
                        }

                        if (photoEvidence.isEmpty()) {
                            item {
                                EmptyEvidenceCard(
                                    message = "Photographs currently being uploaded from offline field buffer.",
                                    icon = Icons.Default.CameraAlt
                                )
                            }
                        } else {
                            items(photoEvidence) { ev ->
                                LineagePhotoEvidenceCard(
                                    evidence = ev,
                                    onViewFull = { selectedPhotoEvidence = ev },
                                    onOpenRecord = onNavigateToRecord
                                )
                            }
                        }
                    }

                    // TAB 0 (All Lineage) or TAB 3 (Voice Notes)
                    if (selectedTab == 0 || selectedTab == 3) {
                        item {
                            SectionHeader(
                                title = "Voice Notes & Respondent Interviews",
                                subtitle = "Audio recordings and transcriptions from headmasters, doctors, and store owners",
                                count = voiceNotes.size
                            )
                        }

                        if (voiceNotes.isEmpty()) {
                            item {
                                EmptyEvidenceCard(
                                    message = "No voice notes recorded for this cluster yet.",
                                    icon = Icons.Default.MicOff
                                )
                            }
                        } else {
                            items(voiceNotes) { ev ->
                                LineageVoiceNoteCard(
                                    evidence = ev,
                                    onOpenRecord = onNavigateToRecord
                                )
                            }
                        }
                    }

                    // TAB 4: Detailed Audit Trail
                    if (selectedTab == 4) {
                        item {
                            AuditTrailTimelineCard(
                                opportunityId = opportunityId,
                                matchingSurveys = matchingSurveys
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
    }

    // Photo Preview Modal if clicked
    selectedPhotoEvidence?.let { ev ->
        PhotoPreviewDialog(
            evidence = ev,
            onDismiss = { selectedPhotoEvidence = null },
            onOpenRecord = onNavigateToRecord
        )
    }
}

@Composable
private fun MetricCountItem(
    count: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(count, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            color = DduPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "$count verified",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = DduPrimary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * Visual Flowchart showing the exact journey from field capture to opportunity
 */
@Composable
private fun DataLineageFlowCard(
    opportunityId: String,
    surveysCount: Int,
    photosCount: Int,
    voiceCount: Int,
    confidenceLevel: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = DduPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("END-TO-END DATA LINEAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                }
                Text("Traceability Verified ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Step Visual Journey
            val steps = listOf(
                Triple("1. Field Surveys", "$surveysCount on-site visits with confirmed GPS", Icons.Default.LocationOn),
                Triple("2. Evidence Collected", "$photosCount photos & $voiceCount voice recordings", Icons.Default.CameraAlt),
                Triple("3. Data Cleaning & Validation", "Completeness scored > 85%, verified by supervisor", Icons.Default.Rule),
                Triple("4. DDU Pipeline ($opportunityId)", "Synthesized demand cluster for enterprise formation", Icons.Default.Lightbulb)
            )

            steps.forEachIndexed { index, (stepTitle, stepDesc, icon) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (index == 3) Color(0xFF7B1FA2) else DduPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(DduPrimary.copy(alpha = 0.4f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.padding(bottom = if (index < steps.size - 1) 4.dp else 0.dp)) {
                        Text(stepTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(stepDesc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Validated Survey Record Card with Direct Traceability Link
 */
@Composable
private fun LineageSurveyRecordCard(
    surveyWithDetails: SurveyWithDetails,
    onOpenRecord: (String) -> Unit
) {
    val s = surveyWithDetails.survey
    val products = surveyWithDetails.products
    val evidence = surveyWithDetails.evidenceList

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lineage_survey_${s.dduId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Entity Name + DDU ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (s.surveyType) {
                                    "INSTITUTION" -> Color(0xFF15803D).copy(alpha = 0.15f)
                                    "LOCAL_SHOP" -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                    else -> Color(0xFFEA580C).copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (s.surveyType) {
                                "INSTITUTION" -> "🏛️"
                                "LOCAL_SHOP" -> "🏪"
                                else -> "📦"
                            },
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = s.entityName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${s.entityType} • ${s.village}, ${s.block}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = s.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products Demand Summary
            if (products.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Demand Recorded:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                        products.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${p.productName} (${p.minQuantity.toInt()} ${p.unit})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("₹${p.buyingPrice.toInt()} from ${p.currentSource}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Surveyor & Location Verification Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${s.surveyorName} • ${s.dateString}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format(Locale.US, "GPS %.4f, %.4f", s.gpsLatitude, s.gpsLongitude),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct Link to Record Button
            OutlinedButton(
                onClick = { onOpenRecord(s.dduId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("btn_open_survey_${s.dduId}"),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DduPrimary.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Inspect Full Field Record (${s.dduId})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DduPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = DduPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lineage Card for Photographs & Invoices
 */
@Composable
private fun LineagePhotoEvidenceCard(
    evidence: EvidenceEntity,
    onViewFull: () -> Unit,
    onOpenRecord: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewFull)
            .testTag("lineage_photo_${evidence.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Visual Photo Representation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (evidence.category) {
                            "Bill / Invoice" -> Color(0xFFF1F5F9)
                            "Product" -> Color(0xFFE0F2FE)
                            "Existing Stock" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFE8F5E9)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (evidence.category) {
                            "Bill / Invoice" -> Icons.Default.Receipt
                            "Product" -> Icons.Default.Checkroom
                            "Existing Stock" -> Icons.Default.Inventory
                            else -> Icons.Default.Storefront
                        },
                        contentDescription = null,
                        tint = DduPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FIELD EVIDENCE CAPTURED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = DduPrimary
                    )
                    Text(
                        text = "Tap to enlarge photo 🔍",
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category pill on top right
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = evidence.category,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(
                text = evidence.caption,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Linked survey + Classification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source: ${evidence.surveyDduId}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = DduPrimary,
                    modifier = Modifier.clickable { onOpenRecord(evidence.surveyDduId) }
                )
                Text(
                    text = evidence.classification.replace("_", " "),
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Lineage Card for Voice Notes & Recorded Audio Interviews
 */
@Composable
private fun LineageVoiceNoteCard(
    evidence: EvidenceEntity,
    onOpenRecord: (String) -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0.0f) }

    // Simulated playback loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playbackProgress = 0.0f
            while (playbackProgress < 1.0f) {
                delay(300)
                playbackProgress += 0.05f
            }
            isPlaying = false
            playbackProgress = 0.0f
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lineage_voice_note_${evidence.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA580C).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Audio Interview Recording", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("${evidence.category} • ${evidence.durationSeconds} seconds", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    color = Color(0xFFEA580C).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "RESPONDENT INTERVIEW",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Audio Player Controls Bar
            Surface(
                color = Color(0xFFFFF7ED),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA580C))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPlaying) "Playing audio recording..." else "Tap to play recorded audio",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFC2410C)
                            )
                            Text(
                                text = if (isPlaying) "${(playbackProgress * evidence.durationSeconds).toInt()}s / ${evidence.durationSeconds}s" else "${evidence.durationSeconds}s",
                                fontSize = 10.sp,
                                color = Color(0xFFC2410C)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (isPlaying) playbackProgress else 0.0f },
                            color = Color(0xFFEA580C),
                            trackColor = Color(0xFFFED7AA),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // Caption
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = evidence.caption,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Transcription Box
            if (!evidence.transcription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = DduPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Audio Transcription:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "\"${evidence.transcription}\"",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer link to survey
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source Record: ${evidence.surveyDduId} →",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DduPrimary,
                    modifier = Modifier.clickable { onOpenRecord(evidence.surveyDduId) }
                )
                Text(
                    text = "Verified Voice Note ✓",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

/**
 * Audit Trail Timeline Card
 */
@Composable
private fun AuditTrailTimelineCard(
    opportunityId: String,
    matchingSurveys: List<SurveyWithDetails>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Opportunity Audit Trail & Chain of Custody", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Verifiable history verifying every data touchpoint from field to analytics", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            val auditItems = listOf(
                Quadruple("Initial Field Observation", "Recorded by Vidushi Fellows with on-site GPS verification", "Balrampur Field Team", "Phase 1"),
                Quadruple("Supporting Evidence Attached", "Photographs of uniforms, invoices, and respondent voice notes uploaded", "Field Device Buffer", "Phase 1"),
                Quadruple("Supervisor Field Audit", "Completeness scored > 85%, verified by Supervisor R. K. Baruah", "Block Office", "Phase 2"),
                Quadruple("Pattern Recognition Algorithm", "Cross-village demand matched into Opportunity $opportunityId", "DDU Analytics Engine", "Stage 1 Complete"),
                Quadruple("Stage 2 Production Gate", "Unlocked for SWSM tailoring cluster & raw material assessment", "Opportunity Committee", "Pending Pilot")
            )

            auditItems.forEachIndexed { idx, item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        if (idx < auditItems.size - 1) {
                            Box(modifier = Modifier.width(2.dp).height(30.dp).background(Color(0xFF10B981).copy(alpha = 0.35f)))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.first, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(item.fourth, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                        }
                        Text(item.second, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Touchpoint: ${item.third}", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

/**
 * Empty Evidence State
 */
@Composable
private fun EmptyEvidenceCard(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(message, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Zoomed Photo Preview Dialog
 */
@Composable
private fun PhotoPreviewDialog(
    evidence: EvidenceEntity,
    onDismiss: () -> Unit,
    onOpenRecord: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(evidence.category, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = DduPrimary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PHOTOGRAPH INSPECTION VIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DduPrimary)
                        Text(evidence.mediaUri, fontSize = 9.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(evidence.caption, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Linked Survey: ${evidence.surveyDduId}", fontSize = 11.sp, color = DduPrimary)

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onOpenRecord(evidence.surveyDduId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Linked Survey Record (${evidence.surveyDduId})")
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
