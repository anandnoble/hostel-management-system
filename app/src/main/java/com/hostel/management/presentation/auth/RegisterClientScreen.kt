package com.hostel.management.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostel.management.domain.repository.ClientRegistrationRequest
import com.hostel.management.presentation.saas.ClientRegistrationSummary
import com.hostel.management.presentation.saas.OrganizationViewModel

enum class RegistrationType {
    SINGLE_HOSTEL,
    ORGANIZATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterClientScreen(
    organizationViewModel: OrganizationViewModel,
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: (email: String) -> Unit
) {
    var registrationType by remember { mutableStateOf(RegistrationType.SINGLE_HOSTEL) }

    var organizationName by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("Basic") }

    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPhone by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var hostelName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val plans = listOf("Free", "Basic", "Premium", "Enterprise")

    val isLoading by organizationViewModel.isLoading.collectAsState()
    val error by organizationViewModel.error.collectAsState()
    val registeredCredentials by organizationViewModel.registeredCredentials.collectAsState()

    val isSingleHostel = registrationType == RegistrationType.SINGLE_HOSTEL

    val isFormValid = if (isSingleHostel) {
        hostelName.isNotBlank() && adminName.isNotBlank() && adminEmail.isNotBlank() && adminPassword.length >= 6 && adminPassword == confirmPassword
    } else {
        organizationName.isNotBlank() && adminName.isNotBlank() && adminEmail.isNotBlank() && adminPassword.length >= 6 && adminPassword == confirmPassword
    }

    // For single hostel, default plan is "Free" and domain is auto-generated
    val effectivePlan = if (isSingleHostel) "Free" else selectedPlan
    val effectiveDomain = if (isSingleHostel) hostelName.trim().lowercase().replace(" ", "") else domain

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isSingleHostel) "Register Hostel" else "Register Organization",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Create your Hostel Management account",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Registration Mode Selector Tabs (Single Hostel vs Organization)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = registrationType == RegistrationType.SINGLE_HOSTEL,
                        onClick = { registrationType = RegistrationType.SINGLE_HOSTEL },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(Icons.Default.HolidayVillage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Single Hostel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    SegmentedButton(
                        selected = registrationType == RegistrationType.ORGANIZATION,
                        onClick = { registrationType = RegistrationType.ORGANIZATION },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Organization / Chain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Registration Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SINGLE HOSTEL MODE: Simple, focused layout
                    if (isSingleHostel) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HolidayVillage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("1. Your Hostel Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                OutlinedTextField(
                                    value = hostelName,
                                    onValueChange = { hostelName = it },
                                    label = { Text("Hostel Name *") },
                                    placeholder = { Text("e.g. Sunrise Boys Hostel") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("Hostel Address") },
                                    placeholder = { Text("123 Main St, City") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Info chip: plan and domain are auto-set
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Free plan · No domain required · You can upgrade later",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ORGANIZATION MODE: Full multi-hostel layout
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("1. Organization Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                OutlinedTextField(
                                    value = organizationName,
                                    onValueChange = { organizationName = it },
                                    label = { Text("Organization Name *") },
                                    placeholder = { Text("e.g. Grand Campus Group") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = domain,
                                    onValueChange = { domain = it },
                                    label = { Text("Domain / Subdomain") },
                                    placeholder = { Text("e.g. grandcampus") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Subscription Plan:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    plans.forEach { plan ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = plan == selectedPlan,
                                                onClick = { selectedPlan = plan }
                                            )
                                            Text(plan, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                HorizontalDivider()

                                OutlinedTextField(
                                    value = hostelName,
                                    onValueChange = { hostelName = it },
                                    label = { Text("Primary Hostel Name (Optional)") },
                                    placeholder = { Text("e.g. Main Campus Block") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("Campus Address (Optional)") },
                                    placeholder = { Text("123 University Ave") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Admin Account Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSingleHostel) "2. Your Admin Login Account" else "2. Hostel Admin Account Setup",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            OutlinedTextField(
                                value = adminName,
                                onValueChange = { adminName = it },
                                label = { Text("Admin Full Name *") },
                                placeholder = { Text("e.g. John Doe") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adminEmail,
                                onValueChange = { adminEmail = it },
                                label = { Text("Admin Login Email *") },
                                placeholder = { Text("e.g. admin@hostel.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adminPhone,
                                onValueChange = { adminPhone = it },
                                label = { Text("Contact Phone Number") },
                                placeholder = { Text("+1 555-0192") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it },
                                label = { Text("Password * (min 6 chars)") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password *") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (confirmPassword.isNotEmpty() && confirmPassword != adminPassword) {
                                Text(
                                    text = "Passwords do not match",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        val finalOrgName = if (isSingleHostel) hostelName else organizationName
                        val finalHostelName = if (isSingleHostel) hostelName else hostelName.ifBlank { organizationName }

                        organizationViewModel.registerClient(
                            ClientRegistrationRequest(
                                organizationName = if (isSingleHostel) hostelName else organizationName,
                                domain = effectiveDomain,
                                subscriptionPlan = effectivePlan,
                                adminName = adminName,
                                adminEmail = adminEmail,
                                adminPhone = adminPhone,
                                adminPassword = adminPassword,
                                hostelName = if (isSingleHostel) hostelName else hostelName.ifBlank { organizationName },
                                address = address
                            )
                        )
                    },
                    enabled = isFormValid && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSingleHostel) "Register Hostel & Create Admin Account" else "Register Organization & Create Admin Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    registeredCredentials?.let { summary ->
        RegistrationSuccessModal(
            summary = summary,
            onProceedToLogin = {
                val email = summary.adminEmail
                organizationViewModel.clearRegisteredCredentials()
                onRegistrationSuccess(email)
            }
        )
    }
}

@Composable
fun RegistrationSuccessModal(
    summary: ClientRegistrationSummary,
    onProceedToLogin: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val credentialsText = """
        Registration Successful!
        -----------------------------------
        Hostel / Organization: ${summary.organization.name}
        Admin Name: ${summary.adminName}
        Login Email: ${summary.adminEmail}
        Password: ${summary.adminPassword}
        Primary Property: ${summary.hostelName}
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onProceedToLogin,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registration Successful!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your hostel account has been created successfully!")

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hostel / Property: ${summary.organization.name}", fontWeight = FontWeight.Bold)
                        Text("Primary Property: ${summary.hostelName}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Admin Name: ${summary.adminName}")
                        Text("Login Email: ${summary.adminEmail}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Password: ${summary.adminPassword}", fontWeight = FontWeight.Bold)
                    }
                }

                if (copied) {
                    Text("✓ Credentials copied to clipboard!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onProceedToLogin) {
                Text("Proceed to Login")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                clipboardManager.setText(AnnotatedString(credentialsText))
                copied = true
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Credentials")
            }
        }
    )
}
