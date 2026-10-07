package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.ProductEntity
import com.example.data.model.SurveyEntity
import com.example.data.model.SurveyWithDetails
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordDialog(
    record: SurveyWithDetails,
    onDismiss: () -> Unit,
    onSave: (SurveyEntity, List<ProductEntity>) -> Unit
) {
    val context = LocalContext.current
    val originalSurvey = record.survey

    // Survey Field States
    var entityName by remember { mutableStateOf(originalSurvey.entityName) }
    var entityType by remember { mutableStateOf(originalSurvey.entityType) }
    var surveyType by remember { mutableStateOf(originalSurvey.surveyType) }
    var contactPerson by remember { mutableStateOf(originalSurvey.contactPerson) }
    var contactNumber by remember { mutableStateOf(originalSurvey.contactNumber) }
    var village by remember { mutableStateOf(originalSurvey.village) }
    var tola by remember { mutableStateOf(originalSurvey.tola) }
    var block by remember { mutableStateOf(originalSurvey.block) }
    var district by remember { mutableStateOf(originalSurvey.district) }
    var gpsLat by remember { mutableStateOf(originalSurvey.gpsLatitude.toString()) }
    var gpsLng by remember { mutableStateOf(originalSurvey.gpsLongitude.toString()) }
    var status by remember { mutableStateOf(originalSurvey.status) }
    var summary by remember { mutableStateOf(originalSurvey.fieldIntelligenceSummary) }

    // Demanded Products list
    var productsList by remember { mutableStateOf(record.products.toMutableList()) }

    // Dropdown states
    var entityTypeExpanded by remember { mutableStateOf(false) }
    var surveyTypeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val entityTypeOptions = listOf(
        "KIRANA_STORE", "RATION_SHOP", "PANCHAYAT_BHAWAN", "SCHOOL",
        "PRIMARY_HEALTH_CENTRE", "ANGANWADI", "FERTILIZER_SHOP", "HARDWARE_STORE",
        "CARPENTRY_WORKSHOP", "FLOUR_MILL", "SELF_HELP_GROUP", "OTHER"
    )

    val surveyTypeOptions = listOf(
        "COMMERCIAL", "INSTITUTION", "ENTERPRISE", "AGRICULTURE", "RESIDENTIAL", "FIELD_OBSERVATION"
    )

    val statusOptions = listOf(
        "DRAFT", "PENDING_SYNC", "SUBMITTED", "UNDER_REVIEW", "APPROVED", "RETURNED"
    )

    // Location fetcher
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        gpsLat = String.format(Locale.US, "%.6f", loc.latitude)
                        gpsLng = String.format(Locale.US, "%.6f", loc.longitude)
                        Toast.makeText(context, "GPS updated: $gpsLat, $gpsLng", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: SecurityException) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("dialog_edit_record")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit Field Record",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record ID: ${originalSurvey.dduId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Form
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "1. GENERAL DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = entityName,
                            onValueChange = { entityName = it },
                            label = { Text("Entity Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        // Entity Type dropdown
                        ExposedDropdownMenuBox(
                            expanded = entityTypeExpanded,
                            onExpandedChange = { entityTypeExpanded = !entityTypeExpanded }
                        ) {
                            OutlinedTextField(
                                value = entityType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Entity Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityTypeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = entityTypeExpanded,
                                onDismissRequest = { entityTypeExpanded = false }
                            ) {
                                entityTypeOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            entityType = opt
                                            entityTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Survey Type dropdown
                        ExposedDropdownMenuBox(
                            expanded = surveyTypeExpanded,
                            onExpandedChange = { surveyTypeExpanded = !surveyTypeExpanded }
                        ) {
                            OutlinedTextField(
                                value = surveyType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Survey Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = surveyTypeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = surveyTypeExpanded,
                                onDismissRequest = { surveyTypeExpanded = false }
                            ) {
                                surveyTypeOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            surveyType = opt
                                            surveyTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = contactPerson,
                                onValueChange = { contactPerson = it },
                                label = { Text("Contact Person") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = contactNumber,
                                onValueChange = { contactNumber = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Text(
                            text = "2. LOCATION & GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = village,
                                onValueChange = { village = it },
                                label = { Text("Village *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tola,
                                onValueChange = { tola = it },
                                label = { Text("Tola / Settlement") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = block,
                                onValueChange = { block = it },
                                label = { Text("Block") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = district,
                                onValueChange = { district = it },
                                label = { Text("District") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = gpsLat,
                                onValueChange = { gpsLat = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = gpsLng,
                                onValueChange = { gpsLng = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fine) {
                                        try {
                                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                                if (loc != null) {
                                                    gpsLat = String.format(Locale.US, "%.6f", loc.latitude)
                                                    gpsLng = String.format(Locale.US, "%.6f", loc.longitude)
                                                    Toast.makeText(context, "GPS updated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (_: SecurityException) {}
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Current Location", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "3. STATUS & OBSERVATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    item {
                        // Status dropdown
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded }
                        ) {
                            OutlinedTextField(
                                value = status,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Validation Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                statusOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            status = opt
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = summary,
                            onValueChange = { summary = it },
                            label = { Text("Field Intelligence Summary") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "4. DEMANDED PRODUCTS (${productsList.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    val newProd = ProductEntity(
                                        surveyDduId = originalSurvey.dduId,
                                        productName = "New Item",
                                        category = "Kirana/Consumer",
                                        brand = "Local",
                                        unit = "Piece",
                                        minQuantity = 10.0,
                                        maxQuantity = 50.0,
                                        buyingPrice = 50.0,
                                        buyingFrequency = "Monthly",
                                        currentSupplier = "Local Wholesaler",
                                        supplierContact = "",
                                        currentSource = "Local"
                                    )
                                    productsList = (productsList + newProd).toMutableList()
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Product", fontSize = 12.sp)
                            }
                        }
                    }

                    itemsIndexed(productsList) { index, prod ->
                        EditableProductItem(
                            product = prod,
                            onUpdate = { updated ->
                                val list = productsList.toMutableList()
                                list[index] = updated
                                productsList = list
                            },
                            onDelete = {
                                val list = productsList.toMutableList()
                                list.removeAt(index)
                                productsList = list
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (entityName.isBlank()) {
                                Toast.makeText(context, "Entity Name is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (village.isBlank()) {
                                Toast.makeText(context, "Village is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val updatedSurvey = originalSurvey.copy(
                                entityName = entityName.trim(),
                                entityType = entityType,
                                surveyType = surveyType,
                                contactPerson = contactPerson.trim(),
                                contactNumber = contactNumber.trim(),
                                village = village.trim(),
                                tola = tola.trim(),
                                block = block.trim(),
                                district = district.trim(),
                                gpsLatitude = gpsLat.toDoubleOrNull() ?: originalSurvey.gpsLatitude,
                                gpsLongitude = gpsLng.toDoubleOrNull() ?: originalSurvey.gpsLongitude,
                                status = status,
                                fieldIntelligenceSummary = summary.trim(),
                                updatedTimestamp = System.currentTimeMillis()
                            )

                            onSave(updatedSurvey, productsList)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_edited_record"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableProductItem(
    product: ProductEntity,
    onUpdate: (ProductEntity) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = product.productName,
                    onValueChange = { onUpdate(product.copy(productName = it)) },
                    label = { Text("Product Name", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Product",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (product.maxQuantity > 0) product.maxQuantity.toInt().toString() else "",
                    onValueChange = { onUpdate(product.copy(maxQuantity = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Monthly Demand", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (product.buyingPrice > 0) product.buyingPrice.toInt().toString() else "",
                    onValueChange = { onUpdate(product.copy(buyingPrice = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Buying Price (₹)", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = product.unit,
                    onValueChange = { onUpdate(product.copy(unit = it)) },
                    label = { Text("Unit", fontSize = 10.sp) },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = product.category,
                    onValueChange = { onUpdate(product.copy(category = it)) },
                    label = { Text("Category", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = product.currentSource,
                    onValueChange = { onUpdate(product.copy(currentSource = it)) },
                    label = { Text("Current Source", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
