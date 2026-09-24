package com.example.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EvidenceEntity
import com.example.ui.components.EvidenceItemCard
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceLibraryScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateToRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val evidenceList by viewModel.allEvidence.collectAsStateWithLifecycle()
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val typeFilters = listOf("ALL" to "All Media", "PHOTO" to "Photos", "VOICE_NOTE" to "Voice Notes")
    val categoryFilters = listOf("ALL", "Institution", "Shopfront", "Product", "Bill / Invoice", "Existing Stock")

    val filtered = remember(evidenceList, selectedTypeFilter, selectedCategoryFilter) {
        evidenceList.filter { ev ->
            val matchesType = selectedTypeFilter == "ALL" || ev.type == selectedTypeFilter
            val matchesCat = selectedCategoryFilter == "ALL" || ev.category.equals(selectedCategoryFilter, ignoreCase = true)
            matchesType && matchesCat
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Evidence Library (${filtered.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Classified photographic, audio & invoice records", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Type filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(typeFilters) { (key, label) ->
                    val isSelected = selectedTypeFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DduPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Category filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categoryFilters) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    SuggestionChip(
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) Color(0xFFE0F2F1) else MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { ev ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (ev.surveyDduId.isNotBlank()) {
                                    onNavigateToRecord(ev.surveyDduId)
                                }
                            }
                    ) {
                        EvidenceItemCard(evidence = ev)
                        if (ev.surveyDduId.isNotBlank()) {
                            Text(
                                text = "Linked to survey: ${ev.surveyDduId} →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DduPrimary,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
