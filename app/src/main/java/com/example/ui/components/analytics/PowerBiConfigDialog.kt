package com.example.ui.components.analytics

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.PowerBIService
import com.example.data.analytics.model.PowerBiConfig
import com.example.ui.theme.DduPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerBiConfigDialog(
    powerBIService: PowerBIService,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by powerBIService.configState.collectAsState()

    var workspaceId by remember { mutableStateOf(currentConfig.workspaceId) }
    var reportId by remember { mutableStateOf(currentConfig.reportId) }
    var datasetId by remember { mutableStateOf(currentConfig.datasetId) }
    var tenantId by remember { mutableStateOf(currentConfig.tenantId) }
    var clientId by remember { mutableStateOf(currentConfig.clientId) }
    var clientSecret by remember { mutableStateOf("") }
    var embedUrl by remember { mutableStateOf(currentConfig.embedUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
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
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        tint = DduPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Microsoft Power BI Integration",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tenant & Workspace Configuration",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Form Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentConfig.isConfigured) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (currentConfig.isConfigured) Color(0xFF10B981) else Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Connection Status: ${if (currentConfig.isConfigured) "CONNECTED" else "NOT CONFIGURED"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (currentConfig.isConfigured) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Last Refresh: ${currentConfig.lastRefreshTime}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentConfig.isConfigured) {
                            TextButton(onClick = {
                                powerBIService.disconnect()
                                Toast.makeText(context, "Power BI disconnected", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Disconnect", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = tenantId,
                    onValueChange = { tenantId = it },
                    label = { Text("Microsoft Entra Tenant ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_tenant_id")
                )

                OutlinedTextField(
                    value = workspaceId,
                    onValueChange = { workspaceId = it },
                    label = { Text("Power BI Workspace ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_workspace_id")
                )

                OutlinedTextField(
                    value = reportId,
                    onValueChange = { reportId = it },
                    label = { Text("Report ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_report_id")
                )

                OutlinedTextField(
                    value = datasetId,
                    onValueChange = { datasetId = it },
                    label = { Text("Semantic Model / Dataset ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_dataset_id")
                )

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Application / Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_client_id")
                )

                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    label = { Text("Client Secret (Never displayed after saving)") },
                    placeholder = { Text("••••••••••••••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_client_secret")
                )

                OutlinedTextField(
                    value = embedUrl,
                    onValueChange = { embedUrl = it },
                    label = { Text("Custom Secure Embed URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_powerbi_embed_url")
                )
            }

            // Actions: Test Connection & Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val ok = powerBIService.testConnection()
                        Toast.makeText(
                            context,
                            if (ok) "Connection Successful! Semantic model active." else "Connection check failed. Verify Tenant ID.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_test_powerbi_connection")
                ) {
                    Text("Test Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        powerBIService.saveConfiguration(
                            workspaceId = workspaceId,
                            reportId = reportId,
                            datasetId = datasetId,
                            tenantId = tenantId,
                            clientId = clientId,
                            clientSecret = clientSecret.ifBlank { null },
                            embedUrl = embedUrl.ifBlank { null }
                        )
                        Toast.makeText(context, "Power BI Configuration Saved!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                    modifier = Modifier.weight(1f).testTag("btn_save_powerbi_config")
                ) {
                    Text("Save & Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
