package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.analytics.PowerBIService
import com.example.data.export.MasterPortalWebGenerator
import com.example.ui.theme.DduPrimary
import com.example.viewmodel.FieldIntelligenceViewModel
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterWebPortalScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val surveys by viewModel.allSurveysWithDetails.collectAsStateWithLifecycle()
    val opportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val stage2Assessments by viewModel.allStage2Assessments.collectAsStateWithLifecycle()
    val sakhyaScreenings by viewModel.allSakhyaScreenings.collectAsStateWithLifecycle()
    val stats by viewModel.statistics.collectAsStateWithLifecycle()

    val powerBIService = remember { PowerBIService(context) }
    val powerBiConfig by powerBIService.configState.collectAsStateWithLifecycle()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isReloading by remember { mutableStateOf(false) }

    // Generate local portal HTML file
    val portalHtml = remember(surveys, opportunities, stage2Assessments, sakhyaScreenings, userProfile, powerBiConfig) {
        MasterPortalWebGenerator.buildPortalHtmlString(
            userProfile = userProfile,
            surveys = surveys,
            opportunities = opportunities,
            stage2Assessments = stage2Assessments,
            sakhyaScreenings = sakhyaScreenings,
            stats = stats,
            powerBiConfig = powerBiConfig
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0F766E),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MASTER PORTAL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("DDU Field & Power BI Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Unified Executive Web Gateway", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export standalone HTML package to share
                    IconButton(
                        onClick = {
                            try {
                                val file = MasterPortalWebGenerator.generatePortalHtmlFile(
                                    context = context,
                                    userProfile = userProfile,
                                    surveys = surveys,
                                    opportunities = opportunities,
                                    stage2Assessments = stage2Assessments,
                                    sakhyaScreenings = sakhyaScreenings,
                                    stats = stats,
                                    powerBiConfig = powerBiConfig
                                )
                                val uri = MasterPortalWebGenerator.getFileUri(context, file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/html"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "DDU Field Intelligence • Master Web Portal Package")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Master Web Portal HTML"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("action_share_portal_html")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share HTML Portal", tint = DduPrimary)
                    }

                    // Open in Browser
                    IconButton(
                        onClick = {
                            try {
                                val file = MasterPortalWebGenerator.generatePortalHtmlFile(
                                    context = context,
                                    userProfile = userProfile,
                                    surveys = surveys,
                                    opportunities = opportunities,
                                    stage2Assessments = stage2Assessments,
                                    sakhyaScreenings = sakhyaScreenings,
                                    stats = stats,
                                    powerBiConfig = powerBiConfig
                                )
                                val uri = MasterPortalWebGenerator.getFileUri(context, file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "text/html")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open Web Portal in Browser"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Browser launch error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("action_open_external_browser")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Browser", tint = DduPrimary)
                    }

                    // Refresh
                    IconButton(
                        onClick = {
                            isReloading = true
                            webViewInstance?.loadDataWithBaseURL("https://ddu-field-portal.local/", portalHtml, "text/html", "UTF-8", null)
                            isReloading = false
                            Toast.makeText(context, "Web Portal reloaded", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
            )
        },
        bottomBar = {
            // Action status bar for quick actions
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Portal Live (${surveys.size} surveys • ${opportunities.size} opps)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(powerBiConfig.embedUrl))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Power BI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val file = MasterPortalWebGenerator.generatePortalHtmlFile(
                                        context = context,
                                        userProfile = userProfile,
                                        surveys = surveys,
                                        opportunities = opportunities,
                                        stage2Assessments = stage2Assessments,
                                        sakhyaScreenings = sakhyaScreenings,
                                        stats = stats,
                                        powerBiConfig = powerBiConfig
                                    )
                                    Toast.makeText(context, "HTML Portal saved: ${file.name}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("container_master_web_portal")
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewInstance = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try {
                                        ctx.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                        loadDataWithBaseURL("https://ddu-field-portal.local/", portalHtml, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://ddu-field-portal.local/", portalHtml, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
