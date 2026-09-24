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
import com.example.data.export.CollatedReportGenerator
import com.example.data.model.OpportunityEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserProfile
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.viewmodel.UiStatistics
import java.io.File

@Composable
fun CollatedExportDialog(
    userProfile: UserProfile,
    surveys: List<SurveyWithDetails>,
    opportunities: List<OpportunityEntity>,
    stage2Assessments: List<VillageProductionAssessmentEntity>,
    sakhyaScreenings: List<SakhyaScreeningEntity>,
    stats: UiStatistics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    var isGeneratingExcel by remember { mutableStateOf(false) }
    var generatedExcelFile by remember { mutableStateOf<File?>(null) }

    var isGeneratingPpt by remember { mutableStateOf(false) }
    var generatedPptFile by remember { mutableStateOf<File?>(null) }

    val whatsappCaption = """
*DDU Field Intelligence & Village Production Report*
Lead Officer: ${userProfile.name} (${userProfile.designation})
Jurisdiction: ${userProfile.block} Block, ${userProfile.district} Dist
Vatika: ${userProfile.vatika}

*Key Collated Statistics:*
• Surveys Completed: ${surveys.size}
• Stage 1 Product Opportunities: ${opportunities.size}
• Stage 2 Village Production Assessed: ${stage2Assessments.size}
• Sakhya Women Enterprises Screened: ${sakhyaScreenings.size}

Attached is the full collated field report from Deendayal Research Institute.
    """.trimIndent()

    val emailSubject = "DDU Field Intelligence & Village Production Report - ${userProfile.name} (${userProfile.block})"
    val emailBody = """
Respected Officer / Colleague,

Please find attached the comprehensive field intelligence report collated from on-site village surveys, Stage 1 demand synthesis, Stage 2 village production assessments, and Sakhya women enterprise screenings (Form F1).

Report Summary:
- Surveyor: ${userProfile.name} (${userProfile.designation})
- Role: ${userProfile.role.label}
- Cluster: ${userProfile.block} Block, ${userProfile.district} District
- Gram Vatika: ${userProfile.vatika}
- Surveys Recorded: ${surveys.size}
- Stage 1 High-Demand Opportunities: ${opportunities.size}
- Stage 2 Feasibility Assessments: ${stage2Assessments.size}
- Sakhya Micro-Enterprises: ${sakhyaScreenings.size}

Generated via DDU Field Intelligence System, Deendayal Research Institute.
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("dialog_collated_export")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Collated Report & Export",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generate PDF, PPT, Excel & Share",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Summary Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B5E20).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF1B5E20).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COLLATED REPORT DATA SOURCES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Surveys: ${surveys.size} • Stage 1 Opps: ${opportunities.size} • Stage 2 Assessed: ${stage2Assessments.size} • Sakhya: ${sakhyaScreenings.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Author: ${userProfile.name} • ${userProfile.block} Block",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= OPTION 1: COLLATED PDF =================
                ExportFormatCard(
                    title = "Collated PDF Report",
                    formatBadge = "PDF",
                    badgeColor = Color(0xFFDC2626),
                    icon = Icons.Default.PictureAsPdf,
                    description = "Executive briefing with 4 full pages: Executive KPI Dashboard, Stage 1 Demand Matrix, Stage 2 Village Viability & SWSM status, Sakhya MEG Register, and Surveyor certification.",
                    file = generatedPdfFile,
                    isGenerating = isGeneratingPdf,
                    onGenerate = {
                        isGeneratingPdf = true
                        try {
                            val f = CollatedReportGenerator.generateCollatedPdf(
                                context = context,
                                userProfile = userProfile,
                                surveys = surveys,
                                opportunities = opportunities,
                                stage2Assessments = stage2Assessments,
                                sakhyaScreenings = sakhyaScreenings,
                                stats = stats
                            )
                            generatedPdfFile = f
                            Toast.makeText(context, "PDF Generated (${f.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingPdf = false
                        }
                    },
                    onWhatsApp = {
                        val f = generatedPdfFile ?: CollatedReportGenerator.generateCollatedPdf(
                            context, userProfile, surveys, opportunities, stage2Assessments, sakhyaScreenings, stats
                        ).also { generatedPdfFile = it }
                        CollatedReportGenerator.shareToWhatsApp(context, f, whatsappCaption)
                    },
                    onEmail = {
                        val f = generatedPdfFile ?: CollatedReportGenerator.generateCollatedPdf(
                            context, userProfile, surveys, opportunities, stage2Assessments, sakhyaScreenings, stats
                        ).also { generatedPdfFile = it }
                        CollatedReportGenerator.shareToEmail(context, f, emailSubject, emailBody)
                    },
                    onOpen = {
                        generatedPdfFile?.let { CollatedReportGenerator.openFile(context, it) }
                    },
                    onGeneralShare = {
                        generatedPdfFile?.let { CollatedReportGenerator.shareGeneral(context, it, "DDU Field Intelligence PDF") }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ================= OPTION 2: COLLATED EXCEL WORKBOOK =================
                ExportFormatCard(
                    title = "Collated Excel Workbook (.xls)",
                    formatBadge = "EXCEL",
                    badgeColor = Color(0xFF16A34A),
                    icon = Icons.Default.TableChart,
                    description = "Comprehensive 5-sheet dataset: Executive Summary, Master Field Surveys, Stage 1 Opportunities, Stage 2 Village Assessments, and Sakhya Micro-Enterprises.",
                    file = generatedExcelFile,
                    isGenerating = isGeneratingExcel,
                    onGenerate = {
                        isGeneratingExcel = true
                        try {
                            val f = CollatedReportGenerator.generateCollatedExcel(
                                context = context,
                                userProfile = userProfile,
                                surveys = surveys,
                                opportunities = opportunities,
                                stage2Assessments = stage2Assessments,
                                sakhyaScreenings = sakhyaScreenings
                            )
                            generatedExcelFile = f
                            Toast.makeText(context, "Excel Generated (${f.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Excel Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingExcel = false
                        }
                    },
                    onWhatsApp = {
                        val f = generatedExcelFile ?: CollatedReportGenerator.generateCollatedExcel(
                            context, userProfile, surveys, opportunities, stage2Assessments, sakhyaScreenings
                        ).also { generatedExcelFile = it }
                        CollatedReportGenerator.shareToWhatsApp(context, f, whatsappCaption)
                    },
                    onEmail = {
                        val f = generatedExcelFile ?: CollatedReportGenerator.generateCollatedExcel(
                            context, userProfile, surveys, opportunities, stage2Assessments, sakhyaScreenings
                        ).also { generatedExcelFile = it }
                        CollatedReportGenerator.shareToEmail(context, f, emailSubject, emailBody)
                    },
                    onOpen = {
                        generatedExcelFile?.let { CollatedReportGenerator.openFile(context, it) }
                    },
                    onGeneralShare = {
                        generatedExcelFile?.let { CollatedReportGenerator.shareGeneral(context, it, "DDU Field Intelligence Excel") }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ================= OPTION 3: COLLATED POWERPOINT =================
                ExportFormatCard(
                    title = "Collated PPT Presentation (.ppt)",
                    formatBadge = "PPT",
                    badgeColor = Color(0xFFEA580C),
                    icon = Icons.Default.Slideshow,
                    description = "Executive 6-slide presentation deck: Project Identity, Field Coverage & KPIs, Stage 1 Product Gaps, Stage 2 Village Viability Matrix, Sakhya MEG Profile, and Strategic Roadmap.",
                    file = generatedPptFile,
                    isGenerating = isGeneratingPpt,
                    onGenerate = {
                        isGeneratingPpt = true
                        try {
                            val f = CollatedReportGenerator.generateCollatedPpt(
                                context = context,
                                userProfile = userProfile,
                                surveys = surveys,
                                opportunities = opportunities,
                                stage2Assessments = stage2Assessments,
                                sakhyaScreenings = sakhyaScreenings,
                                stats = stats
                            )
                            generatedPptFile = f
                            Toast.makeText(context, "PPT Generated (${f.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "PPT Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingPpt = false
                        }
                    },
                    onWhatsApp = {
                        val f = generatedPptFile ?: CollatedReportGenerator.generateCollatedPpt(
                            context, userProfile, surveys, opportunities, stage2Assessments, sakhyaScreenings, stats
                        ).also { generatedPptFile = it }
                        CollatedReportGenerator.shareToWhatsApp(context, f, whatsappCaption)
                    },
                    onEmail = {
                        val f = generatedPptFile ?: CollatedReportGenerator.generateCollatedPpt(
                            context, userProfile, surveys, opportunities, stage2Assessments, sakhyaScreenings, stats
                        ).also { generatedPptFile = it }
                        CollatedReportGenerator.shareToEmail(context, f, emailSubject, emailBody)
                    },
                    onOpen = {
                        generatedPptFile?.let { CollatedReportGenerator.openFile(context, it) }
                    },
                    onGeneralShare = {
                        generatedPptFile?.let { CollatedReportGenerator.shareGeneral(context, it, "DDU Field Intelligence Presentation") }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    title: String,
    formatBadge: String,
    badgeColor: Color,
    icon: ImageVector,
    description: String,
    file: File?,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit,
    onOpen: () -> Unit,
    onGeneralShare: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = formatBadge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            if (file != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1B5E20).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ready: ${file.name} (${file.length() / 1024} KB)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Generate / Re-generate button
                Button(
                    onClick = onGenerate,
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (file == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (file == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(if (file == null) Icons.Default.Download else Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (file == null) "Create $formatBadge" else "Regenerate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Button (Green)
                Button(
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Email Button (Blue)
                Button(
                    onClick = onEmail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Open / More Options
                if (file != null) {
                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Open file", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
