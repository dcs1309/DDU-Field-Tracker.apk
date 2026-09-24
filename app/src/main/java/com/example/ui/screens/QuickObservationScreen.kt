package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickObservationScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Stitching & Garments") }
    var productService by remember { mutableStateOf("") }
    var observationText by remember { mutableStateOf("") }
    var potentialDemand by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("Rampur Tola") }

    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val isTranscribingVoice by viewModel.isTranscribingVoice.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.startVoiceRecording()
            Toast.makeText(context, "Recording started... Speak clearly about your observation", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required for AI Voice transcription", Toast.LENGTH_SHORT).show()
        }
    }

    val categories = listOf(
        "Stitching & Garments", "Bakery & Food", "Cleaning & Chemicals",
        "Healthcare Supplies", "Packaging / Paper", "Agri & Poultry", "General Gap"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quick Field Observation", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Fast 1-minute opportunity lead capture with AI Voice", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
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
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record quick leads on the go. Tap the AI Voice button to speak your notes—Gemini will transcribe and extract key insights.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // AI Voice-to-Note Assistant Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecordingVoice) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRecordingVoice) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ai_voice_note_card")
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
                                        .background(if (isRecordingVoice) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingVoice) Icons.Default.Mic else Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isRecordingVoice) "Recording Voice Note..." else "AI Voice-to-Note Transcription",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isRecordingVoice) Color(0xFFB91C1C) else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isRecordingVoice) "Listening... tap stop when done" else "Powered by Gemini 3.5 Flash",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isTranscribingVoice) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (isRecordingVoice) {
                                Button(
                                    onClick = {
                                        viewModel.stopAndTranscribeVoiceNote("Quick market observation note for $selectedCategory in $village") { res ->
                                            if (res.success) {
                                                observationText = if (observationText.isBlank()) res.transcript else "$observationText\n\n${res.transcript}"
                                                if (title.isBlank() && res.summary.isNotBlank()) {
                                                    title = res.summary.take(45)
                                                }
                                                Toast.makeText(context, "Transcribed via Gemini AI!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, res.error ?: "Transcription failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_stop_transcribe_voice")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        if (hasAudioPermission) {
                                            viewModel.startVoiceRecording()
                                            Toast.makeText(context, "Recording started... Speak your observation", Toast.LENGTH_SHORT).show()
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_start_voice_to_note")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Speak Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Observation Title *") },
                    placeholder = { Text("e.g. Village haat tailoring cluster gap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_obs_title")
                )
            }

            item {
                Text("Category *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village / Locality *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = productService,
                    onValueChange = { productService = it },
                    label = { Text("Product / Service Observed") },
                    placeholder = { Text("e.g. School uniform stitching, packaging paper") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = observationText,
                    onValueChange = { observationText = it },
                    label = { Text("What did you observe? *") },
                    placeholder = { Text("Describe the field condition, current suppliers, prices or respondent complaints...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = potentialDemand,
                    onValueChange = { potentialDemand = it },
                    label = { Text("Estimated Demand / Local Opportunity") },
                    placeholder = { Text("e.g. Approx 150 uniforms/season or 40 cans of phenyl monthly") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Room Offline Cache: Observations are saved locally first and will sync to Firestore automatically when online.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (title.isBlank() || observationText.isBlank()) {
                            Toast.makeText(context, "Please enter title and observation", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.createQuickObservation(
                            title = title,
                            category = selectedCategory,
                            observation = observationText,
                            productService = productService,
                            potentialDemand = potentialDemand,
                            village = village
                        )
                        Toast.makeText(context, "Observation saved to Room Offline Cache", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_quick_observation_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Quick Observation", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
