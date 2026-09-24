package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.viewmodel.FieldIntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: FieldIntelligenceViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("Rajesh Kumar") }
    var emailOrPhone by remember { mutableStateOf("+91 98765 43210") }
    var selectedRole by remember { mutableStateOf(UserRole.FIELD_SURVEYOR) }
    var selectedBlock by remember { mutableStateOf("Balrampur") }
    var selectedDistrict by remember { mutableStateOf("Chitrakoot") }

    val blocks = listOf("Balrampur", "Majhgawan", "Manikpur", "Karwi", "Mau")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Institution Brand Emblem
            Surface(
                shape = CircleShape,
                color = Color(0xFF1B5E20),
                modifier = Modifier.size(76.dp),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Eco,
                        contentDescription = "DDU Logo",
                        tint = Color(0xFF86EFAC),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DEENDAYAL RESEARCH INSTITUTE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "DDU Gram Seva Kendra",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Field Intelligence, Village Production & Sakhya Portal",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card Form
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "SURVEYOR & OFFICER SIGN-IN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Full Name") },
                        placeholder = { Text("e.g. Rajesh Kumar") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone / Email Input
                    OutlinedTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        label = { Text("Mobile Phone or Email") },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_contact"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Selection
                    Text(
                        text = "SELECT OPERATIONAL ROLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    UserRole.values().forEach { role ->
                        val isSelected = selectedRole == role
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF1B5E20).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Color(0xFF1B5E20) else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedRole = role }
                                .testTag("role_option_${role.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedRole = role }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = role.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = role.roleDescription,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Block & District
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedBlock,
                            onValueChange = { selectedBlock = it },
                            label = { Text("Block") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = selectedDistrict,
                            onValueChange = { selectedDistrict = it },
                            label = { Text("District") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Sign In Button
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.login(
                                name = name,
                                email = if (emailOrPhone.contains("@")) emailOrPhone else "surveyor@dri-ddu.org",
                                phone = if (emailOrPhone.contains("@")) "+91 98765 43210" else emailOrPhone,
                                role = selectedRole,
                                block = selectedBlock,
                                district = selectedDistrict
                            )
                            Toast.makeText(context, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_enter_portal")
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter DDU Intelligence Portal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick 1-Tap Demo Profiles for rapid testing/evaluating
            Text(
                text = "OR SIGN IN WITH A DEMO PROFILE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.loginWithPreset(UserProfile.PRESET_SURVEYOR)
                        Toast.makeText(context, "Logged in as Rajesh Kumar", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_login_surveyor"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1B5E20)),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rajesh Kumar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                        Text("Surveyor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.loginWithPreset(UserProfile.PRESET_SUPERVISOR)
                        Toast.makeText(context, "Logged in as Priya Sharma", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_login_supervisor"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF0D47A1)),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Priya Sharma", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D47A1))
                        Text("Supervisor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.loginWithPreset(UserProfile.PRESET_ADMIN)
                        Toast.makeText(context, "Logged in as Dr. Verma", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_login_admin"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE65100)),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dr. Verma", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
                        Text("Admin", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security & Privacy notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted Local SQLite • Fully Functional Offline",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
