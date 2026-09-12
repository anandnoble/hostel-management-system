package com.hostel.management.presentation.saas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostel.management.domain.model.Organization
import com.hostel.management.domain.repository.ClientRegistrationRequest
import com.hostel.management.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaasDashboardScreen(
    authViewModel: AuthViewModel,
    organizationViewModel: OrganizationViewModel,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val organizations by organizationViewModel.organizations.collectAsState()
    val saasPricingConfig by organizationViewModel.saasPricingConfig.collectAsState()
    val isLoading by organizationViewModel.isLoading.collectAsState()
    val error by organizationViewModel.error.collectAsState()
    val successMessage by organizationViewModel.successMessage.collectAsState()
    val registeredCredentials by organizationViewModel.registeredCredentials.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        organizationViewModel.fetchOrganizations()
        organizationViewModel.fetchSaasPricingConfig()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(message = it)
            organizationViewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            organizationViewModel.clearSuccessMessage()
        }
    }

    var showRegisterClientDialog by remember { mutableStateOf(false) }
    var editingOrg by remember { mutableStateOf<Organization?>(null) }
    var deletingOrg by remember { mutableStateOf<Organization?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Central Admin", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "SaaS Platform Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showRegisterClientDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register Client", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegisterClientDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Register Client") },
                text = { Text("New Client Registration") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Calculated SaaS MRR and ARR Telemetry (dynamic based on 2 pricing models & active students)
            val mrr = remember(organizations, saasPricingConfig) {
                organizations.filter { it.status == "Active" }.sumOf { org ->
                    if (org.pricingModel == "Per-Student") {
                        val count = if (org.totalStudents > 0) org.totalStudents else 50
                        count * saasPricingConfig.perStudentRate
                    } else {
                        val count = org.totalStudents
                        when {
                            count <= saasPricingConfig.slab1MaxStudents -> saasPricingConfig.slab1Price
                            count <= saasPricingConfig.slab2MaxStudents -> saasPricingConfig.slab2Price
                            else -> saasPricingConfig.slab3Price
                        }
                    }
                }
            }
            val arr = mrr * 12

            var showPricingConfigCard by remember { mutableStateOf(false) }

            // Dashboard Revenue & Tenant Stats
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Clients",
                        value = organizations.size.toString(),
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Active Plans",
                        value = organizations.count { it.status == "Active" }.toString(),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Est. MRR",
                        value = "₹${mrr.toInt()}/mo",
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Est. ARR",
                        value = "₹${arr.toInt()}/yr",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Global SaaS Pricing Configurator Card (Blanks)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SaaS Global Pricing Configurator (Blanks)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            TextButton(onClick = { showPricingConfigCard = !showPricingConfigCard }) {
                                Icon(if (showPricingConfigCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                Text(if (showPricingConfigCard) "Hide" else "Edit Blanks", fontSize = 12.sp)
                            }
                        }

                        if (showPricingConfigCard) {
                            var perStudentBlank by remember(saasPricingConfig) { mutableStateOf(saasPricingConfig.perStudentRate.toString()) }
                            var slab1PriceBlank by remember(saasPricingConfig) { mutableStateOf(saasPricingConfig.slab1Price.toString()) }
                            var slab2PriceBlank by remember(saasPricingConfig) { mutableStateOf(saasPricingConfig.slab2Price.toString()) }
                            var slab3PriceBlank by remember(saasPricingConfig) { mutableStateOf(saasPricingConfig.slab3Price.toString()) }

                            Text("Plan 1: Per-Student Pricing Model", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = perStudentBlank,
                                onValueChange = { perStudentBlank = it },
                                label = { Text("Rate Per Student / Month (₹/student)") },
                                placeholder = { Text("15.00") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Plan 2: Tiered Fixed Slab Pricing Model", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = slab1PriceBlank,
                                onValueChange = { slab1PriceBlank = it },
                                label = { Text("Slab 1 (Up to 50 students) Rate: ₹") },
                                placeholder = { Text("499.00") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = slab2PriceBlank,
                                onValueChange = { slab2PriceBlank = it },
                                label = { Text("Slab 2 (Up to 150 students) Rate: ₹") },
                                placeholder = { Text("1299.00") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = slab3PriceBlank,
                                onValueChange = { slab3PriceBlank = it },
                                label = { Text("Slab 3 (Up to 300 students) Rate: ₹") },
                                placeholder = { Text("2499.00") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val newConfig = saasPricingConfig.copy(
                                        perStudentRate = perStudentBlank.toDoubleOrNull() ?: 15.0,
                                        slab1Price = slab1PriceBlank.toDoubleOrNull() ?: 499.0,
                                        slab2Price = slab2PriceBlank.toDoubleOrNull() ?: 1299.0,
                                        slab3Price = slab3PriceBlank.toDoubleOrNull() ?: 2499.0
                                    )
                                    organizationViewModel.updateSaasPricingConfig(newConfig)
                                    showPricingConfigCard = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save SaaS Pricing Blanks", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "Current Rates: Per-Student = ₹${saasPricingConfig.perStudentRate}/student/mo | Standard Slab (50 std) = ₹${saasPricingConfig.slab1Price}/mo | Growth (150 std) = ₹${saasPricingConfig.slab2Price}/mo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registered Organizations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { organizationViewModel.fetchOrganizations() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            if (isLoading && organizations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (organizations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BusinessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No clients registered yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showRegisterClientDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register First Client")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(organizations, key = { it.id }) { org ->
                        OrganizationCard(
                            organization = org,
                            onStatusChange = { newStatus ->
                                organizationViewModel.updateStatus(org.id, newStatus)
                            },
                            onEdit = { editingOrg = org },
                            onDelete = { deletingOrg = org }
                        )
                    }
                }
            }
        }
    }

    if (showRegisterClientDialog) {
        RegisterClientDialog(
            onDismiss = { showRegisterClientDialog = false },
            onConfirm = { request ->
                organizationViewModel.registerClient(request)
                showRegisterClientDialog = false
            }
        )
    }

    registeredCredentials?.let { summary ->
        RegistrationCredentialsSummaryDialog(
            summary = summary,
            onDismiss = { organizationViewModel.clearRegisteredCredentials() }
        )
    }

    editingOrg?.let { org ->
        EditOrganizationDialog(
            organization = org,
            onDismiss = { editingOrg = null },
            onConfirm = { name, domain, plan, status, model ->
                organizationViewModel.updateOrganization(org.id, name, domain, plan, status, model)
                editingOrg = null
            }
        )
    }

    deletingOrg?.let { org ->
        AlertDialog(
            onDismissRequest = { deletingOrg = null },
            title = { Text("Delete Organization") },
            text = { Text("Are you sure you want to delete '${org.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        organizationViewModel.deleteOrganization(org.id)
                        deletingOrg = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingOrg = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrganizationCard(
    organization: Organization,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = organization.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = organization.domain?.let { "Domain: $it" } ?: "No domain configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!organization.adminEmail.isNullOrBlank()) {
                        Text(
                            text = "Admin Email: ${organization.adminEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                val (badgeColor, statusText) = when (organization.status) {
                    "Active" -> Color(0xFF4CAF50) to "Active"
                    "Suspended" -> Color(0xFFFF9800) to "Suspended"
                    "Inactive" -> Color(0xFFF44336) to "Inactive"
                    else -> Color(0xFF757575) to organization.status
                }

                Badge(containerColor = badgeColor) {
                    Text(statusText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Plan: ${organization.subscriptionPlan} (${organization.pricingModel})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Organization", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Organization", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Comprehensive Status Control Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (organization.status != "Active") {
                    Button(
                        onClick = { onStatusChange("Active") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Activate")
                    }
                }

                if (organization.status != "Suspended") {
                    OutlinedButton(
                        onClick = { onStatusChange("Suspended") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suspend")
                    }
                }

                if (organization.status != "Inactive") {
                    TextButton(
                        onClick = { onStatusChange("Inactive") },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Deactivate")
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (ClientRegistrationRequest) -> Unit
) {
    var organizationName by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("Basic") }
    var selectedPricingModel by remember { mutableStateOf("Slab") } // "Slab" or "Per-Student"

    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPhone by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var hostelName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val plans = listOf("Free", "Basic", "Premium", "Enterprise")

    val isFormValid = organizationName.isNotBlank() && adminName.isNotBlank() && 
                      adminEmail.isNotBlank() && adminPassword.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register New Client Organization", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Organization Details
                Text("1. Organization Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it },
                    label = { Text("Organization / Hostel Name *") },
                    placeholder = { Text("e.g. Grand Campus Hostels") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Subdomain / Domain") },
                    placeholder = { Text("e.g. grandcampus") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Billing Pricing Model:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPricingModel == "Slab",
                            onClick = { selectedPricingModel = "Slab" }
                        )
                        Text("Fixed Slab Tier (Standard ₹499)", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPricingModel == "Per-Student",
                            onClick = { selectedPricingModel = "Per-Student" }
                        )
                        Text("Per-Student (Rate/student)", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("Subscription Plan Tier:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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

                // Section 2: Hostel Admin Credentials
                Text("2. Client Admin Credentials (Login Account)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Admin Full Name *") },
                    placeholder = { Text("e.g. Jane Doe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = { adminEmail = it },
                    label = { Text("Admin Login Email *") },
                    placeholder = { Text("e.g. admin@grandcampus.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adminPhone,
                    onValueChange = { adminPhone = it },
                    label = { Text("Admin Contact Phone") },
                    placeholder = { Text("+1 555-0192") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    label = { Text("Initial Login Password * (min 6 chars)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()

                // Section 3: Initial Property Setup
                Text("3. Property / Hostel Setup (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = hostelName,
                    onValueChange = { hostelName = it },
                    label = { Text("Primary Hostel Block Name") },
                    placeholder = { Text("e.g. Block A - Boys Hostel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Campus Address") },
                    placeholder = { Text("123 University Ave") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        ClientRegistrationRequest(
                            organizationName = organizationName,
                            domain = domain,
                            subscriptionPlan = selectedPlan,
                            adminName = adminName,
                            adminEmail = adminEmail,
                            adminPhone = adminPhone,
                            adminPassword = adminPassword,
                            hostelName = hostelName,
                            address = address,
                            pricingModel = selectedPricingModel
                        )
                    )
                },
                enabled = isFormValid
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RegistrationCredentialsSummaryDialog(
    summary: com.hostel.management.presentation.saas.ClientRegistrationSummary,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val credentialsText = """
        Client Registered Successfully!
        -----------------------------------
        Organization: ${summary.organization.name}
        Plan: ${summary.organization.subscriptionPlan}
        Domain: ${summary.organization.domain ?: "N/A"}
        
        Admin Login Credentials:
        Name: ${summary.adminName}
        Email: ${summary.adminEmail}
        Password: ${summary.adminPassword}
        
        Primary Hostel: ${summary.hostelName}
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registration Success & Account Credentials", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The client organization has been registered and its admin account created:", style = MaterialTheme.typography.bodyMedium)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Organization: ${summary.organization.name}", fontWeight = FontWeight.Bold)
                        Text("Plan: ${summary.organization.subscriptionPlan}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Admin Name: ${summary.adminName}")
                        Text("Admin Email: ${summary.adminEmail}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Password: ${summary.adminPassword}", fontWeight = FontWeight.Bold)
                    }
                }

                if (copied) {
                    Text("✓ Credentials copied to clipboard!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
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

@Composable
fun EditOrganizationDialog(
    organization: Organization,
    onDismiss: () -> Unit,
    onConfirm: (name: String, domain: String, plan: String, status: String, pricingModel: String) -> Unit
) {
    var name by remember { mutableStateOf(organization.name) }
    var domain by remember { mutableStateOf(organization.domain ?: "") }
    var selectedPlan by remember { mutableStateOf(organization.subscriptionPlan) }
    var selectedStatus by remember { mutableStateOf(organization.status) }
    var selectedPricingModel by remember { mutableStateOf(organization.pricingModel) }
    
    val plans = listOf("Free", "Basic", "Premium", "Enterprise")
    val statuses = listOf("Active", "Suspended", "Inactive")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Organization Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Organization Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Billing Pricing Model:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPricingModel == "Slab",
                            onClick = { selectedPricingModel = "Slab" }
                        )
                        Text("Fixed Slab Tier (Standard ₹499)", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPricingModel == "Per-Student",
                            onClick = { selectedPricingModel = "Per-Student" }
                        )
                        Text("Per-Student (Rate/student)", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("Subscription Plan:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    plans.take(2).forEach { plan ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = plan == selectedPlan,
                                onClick = { selectedPlan = plan }
                            )
                            Text(plan)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    plans.drop(2).forEach { plan ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = plan == selectedPlan,
                                onClick = { selectedPlan = plan }
                            )
                            Text(plan)
                        }
                    }
                }
                
                Text("Status:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    statuses.forEach { status ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = status == selectedStatus,
                                onClick = { selectedStatus = status }
                            )
                            Text(status)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, domain, selectedPlan, selectedStatus, selectedPricingModel) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


