package com.example.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.analytics.model.AnalyticsFilterState
import com.example.ui.theme.DduPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerBiFilterBottomSheet(
    filterState: AnalyticsFilterState,
    villages: List<String>,
    blocks: List<String>,
    categories: List<String>,
    onFilterChange: (AnalyticsFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
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
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = DduPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Power BI Filter Panel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cross-filter all 13 analytics modules dynamically",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("btn_reset_analytics_filters")
                ) {
                    Text("Reset All", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Scrollable Filters
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Geography Section
                FilterDropdownSection(
                    title = "Block / Sub-District",
                    selectedValue = filterState.block,
                    options = listOf("ALL") + blocks,
                    onSelected = { onFilterChange(filterState.copy(block = it)) }
                )

                FilterDropdownSection(
                    title = "Village / Settlement",
                    selectedValue = filterState.village,
                    options = listOf("ALL") + villages,
                    onSelected = { onFilterChange(filterState.copy(village = it)) }
                )

                FilterDropdownSection(
                    title = "Vaatika Territory",
                    selectedValue = filterState.vaatika,
                    options = listOf("ALL", "Khaliamari Vaatika", "Balipara Vaatika"),
                    onSelected = { onFilterChange(filterState.copy(vaatika = it)) }
                )

                // 2. Product & Category
                FilterDropdownSection(
                    title = "Product Category",
                    selectedValue = filterState.productCategory,
                    options = listOf("ALL", "Cleaning Chemicals", "Textiles & Linen", "Food Processing", "Agarbatti & Aromatics", "Agri & Livestock", "Handloom & Craft"),
                    onSelected = { onFilterChange(filterState.copy(productCategory = it)) }
                )

                // 3. Institution / Buyer Type
                FilterDropdownSection(
                    title = "Institution / Demand Type",
                    selectedValue = filterState.institutionType,
                    options = listOf("ALL", "School", "Hospital", "Office", "Hotel", "Restaurant", "Other"),
                    onSelected = { onFilterChange(filterState.copy(institutionType = it)) }
                )

                // 4. DDU Stage
                FilterDropdownSection(
                    title = "DDU Pipeline Stage",
                    selectedValue = filterState.dduStage,
                    options = listOf("ALL", "Stage 1 (Discovery & Demand)", "Stage 2 (Village Production)", "Stage 3 (Commercial Opportunity)"),
                    onSelected = { onFilterChange(filterState.copy(dduStage = it)) }
                )

                // 5. Confidence Level
                FilterDropdownSection(
                    title = "Survey Confidence Level",
                    selectedValue = filterState.confidenceLevel,
                    options = listOf("ALL", "HIGH", "MEDIUM", "LOW"),
                    onSelected = { onFilterChange(filterState.copy(confidenceLevel = it)) }
                )

                // 6. Pilot Mode Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pilot Mode Demo Records", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "PILOT",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "Clearly segment simulated test data from verified field records",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = filterState.pilotModeOnly,
                            onCheckedChange = { onFilterChange(filterState.copy(pilotModeOnly = it)) }
                        )
                    }
                }
            }

            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(48.dp)
                    .testTag("btn_apply_analytics_filters"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DduPrimary)
            ) {
                Text("Apply Analytics Filter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FilterDropdownSection(
    title: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (selectedValue != "ALL") DduPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedValue == "ALL") "All ($title)" else selectedValue,
                        fontSize = 13.sp,
                        fontWeight = if (selectedValue != "ALL") FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedValue != "ALL") DduPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                fontWeight = if (opt == selectedValue) FontWeight.Bold else FontWeight.Normal,
                                color = if (opt == selectedValue) DduPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
