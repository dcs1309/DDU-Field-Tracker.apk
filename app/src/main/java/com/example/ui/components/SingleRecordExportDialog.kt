package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.export.SingleRecordExportGenerator
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserProfile
import com.example.data.model.VillageProductionAssessmentEntity
import java.io.File

sealed class ExportTargetRecord {
    data class SurveyRecord(val record: SurveyWithDetails) : ExportTargetRecord()
    data class Stage2Record(val assessment: VillageProductionAssessmentEntity) : ExportTargetRecord()
    data class SakhyaRecord(val screening: SakhyaScreeningEntity) : ExportTargetRecord()
}

@Composable
fun SingleRecordExportDialog(
    targetRecord: ExportTargetRecord,
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    var isGeneratingExcel by remember { mutableStateOf(false) }
    var generatedExcelFile by remember { mutableStateOf<File?>(null) }

    var isGeneratingPpt by remember { mutableStateOf(false) }
    var generatedPptFile by remember { mutableStateOf<File?>(null) }

    val (title, subtitle, badgeColor, identifier) = when (targetRecord) {
        is ExportTargetRecord.SurveyRecord -> Quad(
            targetRecord.record.survey.entityName,
            "Field Observation Dossier (${targetRecord.record.survey.dduId})",
            Color(0xFF1B5E20),
            targetRecord.record.survey.dduId
        )
        is ExportTargetRecord.Stage2Record -> Quad(
            targetRecord.assessment.productName,
            "Stage 2 DDU Village Production Feasibility",
            Color(0xFFE65100),
            "DDU2-${targetRecord.assessment.priorityLevel}"
        )
        is ExportTargetRecord.SakhyaRecord -> Quad(
            targetRecord.screening.entrepreneurName,
            "Sakhya Women Enterprise Screening (Form F1)",
            Color(0xFF15803D),
            targetRecord.screening.screeningId
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("dialog_single_record_export")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = badgeColor,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Field Record",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PDF • Excel (.xls) • PowerPoint (.pptm)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Record Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = identifier,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Officer: ${userProfile.name} • ${userProfile.block} Cluster",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SELECT EXPORT FORMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ================= FORMAT 1: PDF DOCUMENT =================
                FormatExportCard(
                    title = "PDF Dossier (.pdf)",
                    description = "Printable A4 document with official DRI header, telemetry, tables, and validator sign-off.",
                    badge = "Official",
                    badgeColor = Color(0xFFC62828),
                    icon = Icons.Default.PictureAsPdf,
                    isGenerating = isGeneratingPdf,
                    generatedFile = generatedPdfFile,
                    onGenerate = {
                        isGeneratingPdf = true
                        try {
                            generatedPdfFile = when (targetRecord) {
                                is ExportTargetRecord.SurveyRecord -> SingleRecordExportGenerator.generateSurveyPdf(context, targetRecord.record, userProfile)
                                is ExportTargetRecord.Stage2Record -> SingleRecordExportGenerator.generateStage2Pdf(context, targetRecord.assessment, userProfile)
                                is ExportTargetRecord.SakhyaRecord -> SingleRecordExportGenerator.generateSakhyaPdf(context, targetRecord.screening, userProfile)
                            }
                            Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingPdf = false
                        }
                    },
                    onOpen = { generatedPdfFile?.let { SingleRecordExportGenerator.openFile(context, it) } },
                    onShare = { generatedPdfFile?.let { SingleRecordExportGenerator.shareFile(context, it, "$title Dossier") } },
                    onWhatsApp = { generatedPdfFile?.let { SingleRecordExportGenerator.shareViaWhatsApp(context, it, "Official DDU Field Record: $title ($identifier)") } },
                    onEmail = {
                        generatedPdfFile?.let {
                            SingleRecordExportGenerator.shareViaEmail(
                                context,
                                it,
                                "DDU Field Record: $title ($identifier)",
                                "Respected Officer,\n\nPlease find attached the official field record dossier for $title ($identifier).\n\nRegards,\n${userProfile.name}"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ================= FORMAT 2: EXCEL SPREADSHEET =================
                FormatExportCard(
                    title = "Excel Spreadsheet (.xls)",
                    description = "Structured spreadsheet with tabular records, procurement demand rows, costs, and contact data.",
                    badge = "Data Ready",
                    badgeColor = Color(0xFF1B5E20),
                    icon = Icons.Default.TableChart,
                    isGenerating = isGeneratingExcel,
                    generatedFile = generatedExcelFile,
                    onGenerate = {
                        isGeneratingExcel = true
                        try {
                            generatedExcelFile = when (targetRecord) {
                                is ExportTargetRecord.SurveyRecord -> SingleRecordExportGenerator.generateSurveyExcel(context, targetRecord.record, userProfile)
                                is ExportTargetRecord.Stage2Record -> SingleRecordExportGenerator.generateStage2Excel(context, targetRecord.assessment, userProfile)
                                is ExportTargetRecord.SakhyaRecord -> SingleRecordExportGenerator.generateSakhyaExcel(context, targetRecord.screening, userProfile)
                            }
                            Toast.makeText(context, "Excel file created successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate Excel: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingExcel = false
                        }
                    },
                    onOpen = { generatedExcelFile?.let { SingleRecordExportGenerator.openFile(context, it) } },
                    onShare = { generatedExcelFile?.let { SingleRecordExportGenerator.shareFile(context, it, "$title Spreadsheet") } },
                    onWhatsApp = { generatedExcelFile?.let { SingleRecordExportGenerator.shareViaWhatsApp(context, it, "Field Record Data: $title ($identifier)") } },
                    onEmail = {
                        generatedExcelFile?.let {
                            SingleRecordExportGenerator.shareViaEmail(
                                context,
                                it,
                                "Field Record Spreadsheet: $title",
                                "Respected Officer,\n\nPlease find attached the data spreadsheet for $title ($identifier).\n\nRegards,\n${userProfile.name}"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ================= FORMAT 3: POWERPOINT PRESENTATION (.pptm) =================
                FormatExportCard(
                    title = "PowerPoint Deck (.pptm)",
                    description = "Executive slide presentation formatted for Microsoft PowerPoint, LibreOffice, and Google Slides.",
                    badge = "Presentation",
                    badgeColor = Color(0xFFE65100),
                    icon = Icons.Default.Slideshow,
                    isGenerating = isGeneratingPpt,
                    generatedFile = generatedPptFile,
                    onGenerate = {
                        isGeneratingPpt = true
                        try {
                            generatedPptFile = when (targetRecord) {
                                is ExportTargetRecord.SurveyRecord -> SingleRecordExportGenerator.generateSurveyPpt(context, targetRecord.record, userProfile, "pptm")
                                is ExportTargetRecord.Stage2Record -> SingleRecordExportGenerator.generateStage2Ppt(context, targetRecord.assessment, userProfile, "pptm")
                                is ExportTargetRecord.SakhyaRecord -> SingleRecordExportGenerator.generateSakhyaPpt(context, targetRecord.screening, userProfile, "pptm")
                            }
                            Toast.makeText(context, "PowerPoint deck generated successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate PPT: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingPpt = false
                        }
                    },
                    onOpen = { generatedPptFile?.let { SingleRecordExportGenerator.openFile(context, it) } },
                    onShare = { generatedPptFile?.let { SingleRecordExportGenerator.shareFile(context, it, "$title Presentation") } },
                    onWhatsApp = { generatedPptFile?.let { SingleRecordExportGenerator.shareViaWhatsApp(context, it, "Presentation Deck: $title ($identifier)") } },
                    onEmail = {
                        generatedPptFile?.let {
                            SingleRecordExportGenerator.shareViaEmail(
                                context,
                                it,
                                "Presentation Deck: $title",
                                "Respected Officer,\n\nPlease find attached the PowerPoint executive deck for $title ($identifier).\n\nRegards,\n${userProfile.name}"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FormatExportCard(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    isGenerating: Boolean,
    generatedFile: File?,
    onGenerate: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, if (generatedFile != null) badgeColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (generatedFile == null) {
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate $title", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generated: ${generatedFile.name.takeLast(28)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpen,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onEmail,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
