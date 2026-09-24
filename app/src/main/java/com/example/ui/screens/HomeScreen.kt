package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CollatedExportDialog
import com.example.ui.components.UserProfileDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateToNewSurvey: () -> Unit,
    onNavigateToRecordDetail: (String) -> Unit,
    onNavigateToRecords: (String?) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToQuickObservation: () -> Unit,
    onNavigateToOpportunityGraph: () -> Unit = {},
    onNavigateToPhotoCapture: () -> Unit = {},
    onNavigateToSakhyaList: () -> Unit = {},
    onNavigateToNewSakhyaScreening: (String?) -> Unit = {},
    onNavigateToStage2Assessment: (String?, String?) -> Unit = { _, _ -> },
    onNavigateToStage2DduSelection: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val surveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()
    val filteredSurveys by viewModel.filteredSurveys.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sakhyaScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isStage2Unlocked by viewModel.isStage2Unlocked.collectAsStateWithLifecycle()
    val stage2Assessments by viewModel.allStage2Assessments.collectAsStateWithLifecycle()
    val shortlistedOpps by viewModel.shortlistedOpportunities.collectAsStateWithLifecycle()
    val allOpps by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isSimulatedOffline by viewModel.isSimulatedOffline.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("ALL") }

    if (showSyncDialog) {
        RoomCacheSyncDialog(
            isOnline = isOnline,
            isSimulatedOffline = isSimulatedOffline,
            isSyncing = isSyncing,
            pendingCount = pendingSyncCount,
            lastSyncTime = lastSyncTime,
            syncMessage = syncMessage,
            onToggleSimulatedOffline = { viewModel.toggleOnlineStatus() },
            onSyncNow = { viewModel.triggerSync() },
            onDismiss = { showSyncDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = userProfile,
            onSaveName = { newName -> viewModel.updateUserName(newName) },
            onSaveProfile = { name, email, phone, role, block, district, vatika, designation ->
                viewModel.updateUserProfile(name, email, phone, role, block, district, vatika, designation)
            },
            onSelectPreset = { preset -> viewModel.loginWithPreset(preset) },
            onSignOut = {
                viewModel.signOut()
                onSignOut()
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showExportDialog) {
        CollatedExportDialog(
            userProfile = userProfile,
            surveys = surveys,
            opportunities = allOpps,
            stage2Assessments = stage2Assessments,
            sakhyaScreenings = sakhyaScreenings,
            stats = stats,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showRoleDialog) {
        RoleSwitcherDialog(
            currentRole = currentRole,
            onRoleSelected = { viewModel.setUserRole(it) },
            onDismiss = { showRoleDialog = false }
        )
    }

    Scaffold(
        topBar = {
            val isNightMode by viewModel.isNightMode.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showProfileDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DDU Field Intelligence",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = userProfile.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name / Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${userProfile.role.label} • ${userProfile.block} Cluster",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Collated Export & Share Button
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1B5E20).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF1B5E20).copy(alpha = 0.3f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { showExportDialog = true }
                                .testTag("btn_collated_export_home")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export & Share Collated Report",
                                    tint = Color(0xFF1B5E20),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Night Mode Toggle Button (Light/Dark mode)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { viewModel.toggleNightMode() }
                                .testTag("night_mode_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = if (isNightMode) "Switch to Light Mode" else "Switch to Night Mode",
                                    tint = if (isNightMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Avatar pill / Profile Dialog with colored ring border
                        val initials = userProfile.name.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString("")
                            .uppercase()
                            .ifBlank { "DU" }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showProfileDialog = true }
                                .testTag("role_avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewSurvey,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Survey", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("home_fab_new_survey")
            )
        }
    ) { innerPadding ->
        val isDarkTheme = MaterialTheme.colorScheme.background == DduBackgroundDark
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar for filtering field observations by location, entity, or project name
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { 
                        Text(
                            "Search by location, project, entity, or ID...", 
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = "Search", 
                            tint = MaterialTheme.colorScheme.primary 
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar_input")
                )
            }

            // Prominent Online/Offline Banner
            item {
                OfflineStatusBanner(
                    isOnline = isOnline,
                    isSyncing = isSyncing,
                    syncMessage = syncMessage,
                    pendingCount = pendingSyncCount,
                    onSyncClick = { viewModel.triggerSync() },
                    onToggleOnline = { viewModel.toggleOnlineStatus() },
                    onOpenSyncDialog = { showSyncDialog = true }
                )
            }

            // Hero Opportunity Discovery Banner (Airy, Modern, Lighter Gradient)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToOpportunityGraph)
                        .testTag("hero_opportunity_card")
                ) {
                    val heroGradient = if (isDarkTheme) {
                        listOf(Color(0xFF134E4A), Color(0xFF0F3631))
                    } else {
                        listOf(Color(0xFF0F766E), Color(0xFF0D9488))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(colors = heroGradient))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "HIGH PRIORITY CLUSTER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "★ 94% Viability",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "School Uniforms & Linen Enterprise",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "4 Verified institutions in Balrampur sourcing 1,200+ sets annually from external wholesalers. Ready for local DDU stitching cluster.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Annual Demand Drain",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        text = "₹4.8 Lakhs / yr",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFE0B2)
                                    )
                                }

                                Button(
                                    onClick = onNavigateToOpportunityGraph,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = if (isDarkTheme) Color(0xFF0F3631) else Color(0xFF0F766E)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "View Graph",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Category Filters (Pills with theme tokens)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf(
                        "ALL" to "All Records",
                        "INSTITUTION" to "Institutions",
                        "LOCAL_SHOP" to "Local Shops",
                        "SUPPLIER" to "Suppliers",
                        "CUSTOMER_LEAD" to "Leads"
                    )
                    items(categories) { (key, label) ->
                        val isSelected = selectedFilterCategory == key
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shadowElevation = if (isSelected) 2.dp else 1.dp,
                            modifier = Modifier
                                .clickable {
                                    selectedFilterCategory = key
                                    if (key != "ALL") onNavigateToRecords(key)
                                }
                                .testTag("filter_chip_$key")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4 Metric Cards in 2x2 Grid (Aesthetic Cards with Rounded 20.dp)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            count = "${stats.todaysSurveysCount}",
                            title = "Today's Surveys",
                            icon = Icons.Default.Checklist,
                            iconColor = MaterialTheme.colorScheme.primary,
                            iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { onNavigateToRecords("ALL") },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            count = "${stats.pendingSyncCount}",
                            title = "Pending Sync",
                            icon = Icons.Default.CloudUpload,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { onNavigateToRecords("PENDING_SYNC") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            count = "${stats.submittedCount}",
                            title = "Verified Submissions",
                            icon = Icons.Default.Verified,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { onNavigateToRecords("SUBMITTED") },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            count = "${stats.opportunitiesCount}",
                            title = "Live Opportunities",
                            icon = Icons.Default.Lightbulb,
                            iconColor = if (isDarkTheme) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                            iconBgColor = if (isDarkTheme) Color(0xFF3B1E78) else Color(0xFFF3E8FF),
                            onClick = onNavigateToInsights,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions Section
            item {
                Text(
                    text = "Quick Actions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "New Survey",
                            icon = Icons.Default.AddCircle,
                            isPrimary = true,
                            onClick = onNavigateToNewSurvey,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "Field Camera",
                            icon = Icons.Default.CameraAlt,
                            isPrimary = false,
                            onClick = onNavigateToPhotoCapture,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "Quick Note",
                            icon = Icons.Default.EditNote,
                            onClick = onNavigateToQuickObservation,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "Network Graph",
                            icon = Icons.Default.Hub,
                            onClick = onNavigateToOpportunityGraph,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "Sakhya Form F1",
                            icon = Icons.Default.AssignmentInd,
                            onClick = { onNavigateToNewSakhyaScreening(null) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "Prospects (${sakhyaScreenings.size})",
                            icon = Icons.Default.Groups,
                            onClick = onNavigateToSakhyaList,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // DSDC Sakhya Screening Programme Featured Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F382C) // Forest Dark Green matching Sakhya PDF
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSakhyaList)
                        .testTag("card_sakhya_programme_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF1F5142),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.VolunteerActivism,
                                            contentDescription = null,
                                            tint = Color(0xFFA3E635),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DSDC — SAKHYA SCREENING PROGRAMME",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA3E635),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Sakhya Screening Form (F1)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF1F5142),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${sakhyaScreenings.size} Screened",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Assess business background, profitability, market exposure, operations, compliance & digital readiness for women entrepreneurs and MEG clusters.",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToNewSakhyaScreening(null) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("btn_home_start_sakhya_form"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Form F1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToSakhyaList,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_home_view_sakhya_pipeline"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF22C55E)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("View Register", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Stage 2 Village Production Assessment & DDU Selection Matrix
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStage2Unlocked) Color(0xFF1E3A2F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isStage2Unlocked) Color(0xFF4ADE80) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_stage2_village_production")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isStage2Unlocked) Color(0xFF166534) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PrecisionManufacturing,
                                        contentDescription = null,
                                        tint = if (isStage2Unlocked) Color(0xFF86EFAC) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "STAGE 2 ASSESSMENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStage2Unlocked) Color(0xFF86EFAC) else MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Village Production & DDU Matrix",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStage2Unlocked) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isStage2Unlocked) Color(0xFF22C55E) else Color(0xFF64748B)
                            ) {
                                Text(
                                    text = if (isStage2Unlocked) "UNLOCKED" else "LOCKED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isStage2Unlocked) {
                                "${shortlistedOpps.size} Shortlisted Stage 1 products • ${stage2Assessments.size} Assessments completed. SWSM Criticality & Readiness criteria applied."
                            } else {
                                "Stage 2 unlocks once Stage 1 discovery opportunities are shortlisted. Evaluates village manufacturing readiness, criticality & sampling."
                            },
                            fontSize = 12.sp,
                            color = if (isStage2Unlocked) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isStage2Unlocked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToStage2DduSelection,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("btn_home_stage2_ddu_selection"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22C55E),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DDU Matrix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToStage2Assessment(null, null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_home_stage2_assess_new"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF4ADE80)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("+ Assess", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.syncWithFirestore { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = "Sync Cloud Firestore",
                                        tint = Color(0xFF86EFAC)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = onNavigateToInsights,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_home_unlock_stage2"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shortlist Stage 1 Opportunities to Unlock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Recent Records Section (Styled like Reference Image 1 Leaderboard)
            val isSearching = searchQuery.isNotBlank()
            val displayedSurveys = if (isSearching) filteredSurveys else surveys.take(5)

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSearching) "Matching Observations" else "Recent Discoveries",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${if (isSearching) filteredSurveys.size else surveys.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    TextButton(
                        onClick = { onNavigateToRecords("ALL") },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isSearching) "View all results" else "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isSearching && displayedSurveys.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No observations found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No records matched \"$searchQuery\". Try checking the village or entity name.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.setSearchQuery("") },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Search", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(displayedSurveys) { item ->
                    AestheticRecordCard(
                        record = item,
                        onClick = { onNavigateToRecordDetail(item.survey.dduId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun AestheticRecordCard(
    record: SurveyWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DduBackgroundDark
    val (ringColor, avatarBg, icon) = when (record.survey.surveyType) {
        "INSTITUTION" -> Triple(
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
            Icons.Default.AccountBalance
        )
        "LOCAL_SHOP" -> Triple(
            if (isDark) Color(0xFF34D399) else Color(0xFF059669),
            if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
            Icons.Default.Storefront
        )
        "SUPPLIER" -> Triple(
            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE),
            Icons.Default.LocalShipping
        )
        else -> Triple(
            if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
            if (isDark) Color(0xFF431407) else Color(0xFFFFEDD5),
            Icons.Default.Lightbulb
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("record_item_${record.survey.dduId}")
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular avatar with distinct colored ring
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(2.dp, ringColor, CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.survey.entityName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = record.survey.status)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${record.survey.village} • ${record.survey.tola.ifBlank { "Main Area" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (record.survey.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (record.survey.isSynced) Color(0xFF10B981) else Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (record.survey.isSynced) "Synced" else "Room Cache",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (record.survey.isSynced) Color(0xFF10B981) else Color(0xFFD97706)
                        )
                    }
                }

                if (record.products.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = record.products.first().productName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DduPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val totalEstimated = record.products.sumOf { (it.maxQuantity * it.buyingPrice).toLong() }
                        if (totalEstimated > 0L) {
                            Text(
                                text = "₹$totalEstimated",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DduTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
