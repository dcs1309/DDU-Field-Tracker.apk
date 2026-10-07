package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SakhyaScreeningEntity
import com.example.ui.components.ExportTargetRecord
import com.example.ui.components.SingleRecordExportDialog
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakhyaScreeningListScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewScreening: (String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var exportTarget by remember { mutableStateOf<SakhyaScreeningEntity?>(null) }

    val filteredList = remember(allScreenings, searchQuery, selectedFilter) {
        allScreenings.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.entrepreneurName.contains(searchQuery, ignoreCase = true) ||
                item.productAndValueChain.contains(searchQuery, ignoreCase = true) ||
                item.screeningId.contains(searchQuery, ignoreCase = true) ||
                item.fieldFellowName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UDYAMI_READY" -> item.readinessScore >= 75
                "DEVELOPING" -> item.readinessScore in 50..74
                "EARLY" -> item.readinessScore < 50
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val udyamiReadyCount = allScreenings.count { it.readinessScore >= 75 }
    val developingCount = allScreenings.count { it.readinessScore in 50..74 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sakhya Prospects",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "DSDC — Sakhya Screening Pipeline",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_sakhya_list_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (allScreenings.isNotEmpty()) {
                        IconButton(
                            onClick = { exportTarget = filteredList.firstOrNull() ?: allScreenings.first() },
                            modifier = Modifier.testTag("btn_export_sakhya_top")
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Export Sakhya Prospect (PDF/Excel/PPTM)",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { onNavigateToNewScreening(null) },
                        modifier = Modifier.testTag("btn_add_sakhya_screening_top")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Sakhya Screening")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToNewScreening(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Screen Prospect (F1)", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF1B4D3E),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_sakhya_screening")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F382C)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "SAKHYA PROSPECT SCREENING REGISTER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA3E635),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Vaibhavi & Micro-Enterprise Readiness Assessment",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SakhyaStatPill("Total Screened", "${allScreenings.size}", Modifier.weight(1f))
                        SakhyaStatPill("Udyami Ready", "$udyamiReadyCount", Modifier.weight(1f), Color(0xFF22C55E))
                        SakhyaStatPill("Developing", "$developingCount", Modifier.weight(1f), Color(0xFFF59E0B))
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search entrepreneur, value chain, fellow...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("input_search_sakhya"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${allScreenings.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "UDYAMI_READY",
                    onClick = { selectedFilter = "UDYAMI_READY" },
                    label = { Text("Udyami Ready (${udyamiReadyCount})", fontSize = 12.sp) },
                    leadingIcon = {
                        Surface(shape = CircleShape, color = Color(0xFF22C55E), modifier = Modifier.size(8.dp)) {}
                    }
                )
                FilterChip(
                    selected = selectedFilter == "DEVELOPING",
                    onClick = { selectedFilter = "DEVELOPING" },
                    label = { Text("Developing (${developingCount})", fontSize = 12.sp) },
                    leadingIcon = {
                        Surface(shape = CircleShape, color = Color(0xFFF59E0B), modifier = Modifier.size(8.dp)) {}
                    }
                )
                FilterChip(
                    selected = selectedFilter == "EARLY",
                    onClick = { selectedFilter = "EARLY" },
                    label = { Text("Early Stage", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Screening List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AssignmentLate,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Sakhya screenings found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a new screening using the button below or tap Fill Sample.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToNewScreening(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4D3E))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Screening Form (F1)")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.screeningId }) { screening ->
                        SakhyaProspectCard(
                            screening = screening,
                            onClick = { onNavigateToDetail(screening.screeningId) },
                            onEdit = { onNavigateToNewScreening(screening.screeningId) },
                            onDelete = { viewModel.deleteSakhyaScreening(screening) },
                            onExport = { exportTarget = screening }
                        )
                    }
                }
            }
        }
    }

    if (exportTarget != null) {
        val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
        SingleRecordExportDialog(
            targetRecord = ExportTargetRecord.SakhyaRecord(exportTarget!!),
            userProfile = userProfile,
            onDismiss = { exportTarget = null }
        )
    }
}

@Composable
private fun SakhyaStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightColor: Color = Color.White
) {
    Surface(
        color = Color(0xFF1F5142),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = highlightColor)
            Text(text = label, fontSize = 9.sp, color = Color(0xFFE2E8F0))
        }
    }
}

@Composable
private fun SakhyaProspectCard(
    screening: SakhyaScreeningEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Screening?") },
            text = { Text("Are you sure you want to remove ${screening.entrepreneurName} from the Sakhya Screening register?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("card_sakhya_${screening.screeningId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name & Readiness Score badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = screening.entrepreneurName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = screening.productAndValueChain,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Readiness Badge
                val badgeColor = when {
                    screening.readinessScore >= 75 -> Color(0xFF15803D)
                    screening.readinessScore >= 50 -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${screening.readinessScore}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (screening.readinessScore >= 75) "Udyami Ready" else if (screening.readinessScore >= 50) "Developing" else "Early",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar showing readiness score
            LinearProgressIndicator(
                progress = { (screening.readinessScore / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    screening.readinessScore >= 75 -> Color(0xFF15803D)
                    screening.readinessScore >= 50 -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: Profit, Exp, Market
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${screening.currentMonthlyProfitability.toInt()}/mo profit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${screening.yearsInBusiness} yrs in business",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = screening.dedicatedMarketingMember,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Footer with Fellow Name, Date, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = screening.fieldFellowName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${screening.screeningDate}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export Prospect Dossier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
