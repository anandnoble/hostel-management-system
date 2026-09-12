package com.hostel.management.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import java.net.URLEncoder
import com.hostel.management.domain.model.*
import com.hostel.management.presentation.announcement.AnnouncementViewModel
import com.hostel.management.presentation.auth.AuthViewModel
import com.hostel.management.presentation.complaint.ComplaintViewModel
import com.hostel.management.presentation.finance.FinanceViewModel
import kotlinx.coroutines.launch
import com.hostel.management.presentation.hostel.HostelViewModel
import com.hostel.management.presentation.hostel.FloorSetupConfig
import com.hostel.management.presentation.hostel.RoomSetupConfig
import com.hostel.management.presentation.student.StudentViewModel

// -------------------------------------------------------------
// HELPER COMPOSABLES
// -------------------------------------------------------------

@Composable
fun DashboardHeader(
    profile: Profile?,
    onLogout: () -> Unit,
    onNotificationsClick: () -> Unit,
    onBackupSyncClick: (() -> Unit)? = null,
    pendingBadgeCount: Int = 0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome Back,",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = profile?.fullName ?: "User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = profile?.role?.name?.replace("_", " ") ?: "Role",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                if (onBackupSyncClick != null) {
                    IconButton(onClick = onBackupSyncClick) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Backup & Sync",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(onClick = onNotificationsClick) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (pendingBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (pendingBadgeCount > 9) "9+" else pendingBadgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ShortcutItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(68.dp)
        )
    }
}

// -------------------------------------------------------------
// 1. ADMIN DASHBOARD
// -------------------------------------------------------------
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    hostelViewModel: HostelViewModel,
    financeViewModel: FinanceViewModel,
    studentViewModel: StudentViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val stats by hostelViewModel.dashboardStats.collectAsState()
    val hostels by hostelViewModel.hostels.collectAsState()
    val loading by hostelViewModel.loading.collectAsState()
    val pendingRegistrations by studentViewModel.pendingRegistrations.collectAsState()

    var activeTab by remember { mutableStateOf(if (hostels.isEmpty()) "Edit" else "View") }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hostelViewModel.loadHostels()
        hostelViewModel.loadDashboardStats()
        studentViewModel.loadPendingRegistrations()
    }

    if (selectedStudentId != null) {
        StudentProfileBottomSheet(
            studentId = selectedStudentId!!,
            studentViewModel = studentViewModel,
            financeViewModel = financeViewModel,
            onDismiss = { selectedStudentId = null }
        )
    }

    Scaffold(
        bottomBar = {
            // Can add bottom navigation if needed
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") },
                onBackupSyncClick = { onNavigate("backup_sync") },
                pendingBadgeCount = pendingRegistrations.size
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tab Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val tabIndex = when(activeTab) {
                        "Edit" -> 0
                        "View" -> 1
                        "QR" -> 2
                        else -> 0
                    }
                    ScrollableTabRow(
                        selectedTabIndex = tabIndex,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = activeTab == "Edit",
                            onClick = { activeTab = "Edit" },
                            text = { Text("Edit", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == "View",
                            onClick = { activeTab = "View" },
                            text = { Text("View", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == "QR",
                            onClick = { 
                                activeTab = "QR"
                                showQrDialog = true 
                            },
                            text = { Text("Student QR", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (loading && hostels.isEmpty() && stats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (activeTab == "Edit") {
                    // Show Setup Wizard / Edit Mode
                    if (hostels.isEmpty()) {
                        HostelSetupWizard(hostelViewModel)
                    } else {
                        HostelHierarchyEditor(hostelViewModel, hostels.first().id)
                    }
                } else if (activeTab == "View") {
                    // 2D View
                    if (hostels.isNotEmpty()) {
                        Hostel2DView(
                            hostelViewModel = hostelViewModel,
                            financeViewModel = financeViewModel,
                            hostelId = hostels.first().id,
                            onBedClick = { studentId ->
                                selectedStudentId = studentId
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hostel created yet. Go to Edit Hostel.", color = Color.Gray)
                        }
                    }
                } else if (activeTab == "QR") {
                    StudentRegistrationQrCard(
                        hostelId = hostels.firstOrNull()?.id ?: "",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (showQrDialog) {
        val hostelId = hostels.firstOrNull()?.id ?: ""
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showQrDialog = false },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close")
                }
            },
            text = {
                StudentRegistrationQrCard(
                    hostelId = hostelId,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}


// -------------------------------------------------------------
// STUDENT REGISTRATION QR & LINK COMPONENT
// -------------------------------------------------------------
@Composable
fun StudentRegistrationQrCard(
    hostelId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    var baseUrl by remember { mutableStateOf("https://hostel-student-portal.pages.dev") }
    var isEditingUrl by remember { mutableStateOf(false) }

    // 3 Configurable Fee & Payment Blanks (Encrypted Backend)
    var upiNumber by remember { mutableStateOf("") }
    var amountPerMonth by remember { mutableStateOf("") }
    var advanceAmount by remember { mutableStateOf("") }
    var isSavingConfig by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hostelRepo = remember { com.hostel.management.di.ServiceLocator.hostelRepository }

    var activeHostelId by remember(hostelId) { mutableStateOf(hostelId) }

    LaunchedEffect(hostelId) {
        scope.launch {
            val list = hostelRepo.getHostels().getOrNull() ?: emptyList()
            val target = list.find { it.id == hostelId } ?: list.firstOrNull()
            if (target != null) {
                activeHostelId = target.id
                if (!target.upiId.isNullOrBlank()) upiNumber = target.upiId
                if (target.monthlyFee != null && target.monthlyFee > 0) amountPerMonth = target.monthlyFee.toInt().toString()
                if (target.advanceDeposit != null && target.advanceDeposit > 0) advanceAmount = target.advanceDeposit.toInt().toString()
            }
        }
    }

    val fullUrl = remember(baseUrl, activeHostelId) {
        val cleanBase = baseUrl.trim()
        if (activeHostelId.isNotBlank()) {
            if (cleanBase.contains("?")) "$cleanBase&hostel=$activeHostelId" else "$cleanBase?hostel=$activeHostelId"
        } else {
            cleanBase
        }
    }

    val encodedData = remember(fullUrl) {
        try {
            URLEncoder.encode(fullUrl, "UTF-8")
        } catch (e: Exception) {
            fullUrl
        }
    }

    val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$encodedData"

    var showQrGenerated by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Student Registration & Payment Setup",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure your payment details and share the official hostel QR code with students.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECTION 1: HOSTEL FEE & PAYMENT SETTINGS BLANKS ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Section 1: Hostel Fee & Payment Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Blank 1: UPI Number / ID
                    OutlinedTextField(
                        value = upiNumber,
                        onValueChange = { upiNumber = it },
                        label = { Text("1. UPI Number / ID (e.g. 9876543210@upi)") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    // Blank 2: Amount Per Month
                    OutlinedTextField(
                        value = amountPerMonth,
                        onValueChange = { amountPerMonth = it },
                        label = { Text("2. Amount Per Month (e.g. 5000)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    // Blank 3: Advance
                    OutlinedTextField(
                        value = advanceAmount,
                        onValueChange = { advanceAmount = it },
                        label = { Text("3. Advance Deposit (e.g. 10000)") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    Button(
                        onClick = {
                            val targetId = activeHostelId.ifBlank { hostelId }
                            if (targetId.isBlank()) {
                                Toast.makeText(context, "No hostel selected", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                isSavingConfig = true
                                val monthlyFeeVal = amountPerMonth.toDoubleOrNull() ?: 0.0
                                val advanceVal = advanceAmount.toDoubleOrNull() ?: 0.0
                                val result = hostelRepo.updateHostelPaymentConfig(
                                    hostelId = targetId,
                                    upiId = upiNumber,
                                    monthlyFee = monthlyFeeVal,
                                    advanceDeposit = advanceVal
                                )
                                isSavingConfig = false
                                if (result.isSuccess) {
                                    val updatedList = hostelRepo.getHostels().getOrNull() ?: emptyList()
                                    val refreshed = updatedList.find { it.upiId == upiNumber || it.id == targetId } ?: updatedList.firstOrNull()
                                    if (refreshed != null) {
                                        activeHostelId = refreshed.id
                                    }
                                    Toast.makeText(context, "Payment settings saved & encrypted in Supabase!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error saving settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSavingConfig,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        if (isSavingConfig) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Payment Settings to Supabase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECTION 2: HOSTEL REGISTRATION QR CODE & LINK ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Section 2: Official Hostel QR Code & Link",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // QR Code Container (high-contrast white frame for camera scannability)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = qrCodeUrl,
                            contentDescription = "Student Registration QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingUrl) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Portal Web Address / URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = fullUrl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(fullUrl))
                                Toast.makeText(context, "Registration link copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Link", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(fullUrl)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Link", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// HOSTEL ACCORDION VIEW
// -------------------------------------------------------------
@Composable
fun Hostel2DView(
    hostelViewModel: HostelViewModel,
    financeViewModel: FinanceViewModel,
    hostelId: String,
    onBedClick: (String) -> Unit
) {
    val mapData by hostelViewModel.hostelMapData.collectAsState()
    val allocations by hostelViewModel.allocations.collectAsState()
    val invoices by financeViewModel.invoices.collectAsState()
    val loading by hostelViewModel.loading.collectAsState()

    var currentLevel by remember { mutableStateOf("FLOORS") }
    var selectedFloorId by remember { mutableStateOf<String?>(null) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(hostelId) {
        hostelViewModel.loadFullHostelMap(hostelId)
        hostelViewModel.loadAllocations()
        financeViewModel.loadInvoices()
    }

    if (loading && mapData.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (mapData.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("No building data found.", color = Color.Gray)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Live Student Name & Bed Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search student name, roll number, or bed...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (searchQuery.isNotBlank()) {
            val matchingAllocations = allocations.filter { alloc ->
                alloc.status == "Active" && (
                    alloc.studentName?.contains(searchQuery, ignoreCase = true) == true ||
                    alloc.studentRollNumber?.contains(searchQuery, ignoreCase = true) == true ||
                    alloc.bedNumber?.contains(searchQuery, ignoreCase = true) == true ||
                    alloc.roomNumber?.contains(searchQuery, ignoreCase = true) == true
                )
            }

            Text(
                text = "Search Results (${matchingAllocations.size} found):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (matchingAllocations.isEmpty()) {
                Text("No occupied bed found matching '$searchQuery'", color = Color.Gray, fontSize = 13.sp)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchingAllocations) { alloc ->
                        val studentInvoices = invoices.filter { it.studentId == alloc.studentId }
                        val hasPending = studentInvoices.any { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
                        val statusColor = if (hasPending) Color(0xFFF44336) else Color(0xFF4CAF50)
                        val statusText = if (hasPending) "! NOT PAID" else "✓ PAID"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBedClick(alloc.studentId) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alloc.studentName ?: "Student",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Room ${alloc.roomNumber ?: ""} • ${alloc.bedNumber ?: ""} (${alloc.studentRollNumber ?: ""})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Badge(containerColor = statusColor) {
                                    Text(statusText, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Navigation Breadcrumbs
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            if (currentLevel != "FLOORS") {
                IconButton(onClick = { 
                    if (currentLevel == "BEDS") {
                        currentLevel = "ROOMS"
                        selectedRoomId = null
                    } else if (currentLevel == "ROOMS") {
                        currentLevel = "FLOORS"
                        selectedFloorId = null
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            }
            Text(
                text = when(currentLevel) {
                    "FLOORS" -> "Select Floor"
                    "ROOMS" -> "Floor Corridor"
                    "BEDS" -> "Inside Room"
                    else -> ""
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        when(currentLevel) {
            "FLOORS" -> {
                val buildingData = mapData.firstOrNull()
                val floors = buildingData?.floors ?: emptyList()
                
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 600.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(floors.size) { index ->
                        val floorData = floors[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                                selectedFloorId = floorData.floor.id
                                currentLevel = "ROOMS"
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Floor", fontSize = 16.sp, color = Color.Gray)
                                    Text(floorData.floor.floorNumber.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${floorData.rooms.size} Rooms", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            "ROOMS" -> {
                val buildingData = mapData.firstOrNull()
                val floorData = buildingData?.floors?.find { it.floor.id == selectedFloorId }
                val rooms = floorData?.rooms ?: emptyList()
                
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 600.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(rooms.size) { index ->
                        val room = rooms[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedRoomId = room.id
                                currentLevel = "BEDS"
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.6f)
                                    .background(Color(0xFF8D6E63), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            ) {
                                // Doorknob
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp)
                                        .size(12.dp)
                                        .background(Color(0xFFFFD700), androidx.compose.foundation.shape.CircleShape)
                                )
                                // Room Name Plate
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(room.roomNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🚪 ${room.roomNumber}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            "BEDS" -> {
                val buildingData = mapData.firstOrNull()
                val floorData = buildingData?.floors?.find { it.floor.id == selectedFloorId }
                val room = floorData?.rooms?.find { it.id == selectedRoomId }
                val beds = room?.beds ?: emptyList()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5DC), RoundedCornerShape(12.dp))
                        .border(4.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 600.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(beds.size) { index ->
                            val bed = beds[index]
                            
                            val allocation = allocations.find { it.bedId == bed.id && it.status == "Active" }
                            val isAvailable = allocation == null
                            
                            var bedColor = Color(0xFF2196F3) // BLUE
                            var statusText = "AVAILABLE"
                            var assignedStudentId = allocation?.studentId
                            
                            if (!isAvailable && assignedStudentId != null) {
                                val studentInvoices = invoices.filter { it.studentId == assignedStudentId }
                                val hasPending = studentInvoices.any { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
                                if (hasPending) {
                                    bedColor = Color(0xFFF44336) // RED
                                    statusText = "! NOT PAID"
                                } else {
                                    bedColor = Color(0xFF4CAF50) // GREEN
                                    statusText = "✓ PAID"
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    if (!isAvailable && assignedStudentId != null) {
                                        onBedClick(assignedStudentId)
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.8f)
                                        .background(bedColor, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    // Pillow
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth(0.8f)
                                            .fillMaxHeight(0.3f)
                                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(bed.bedNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(statusText, color = bedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// HOSTEL SETUP WIZARD
// -------------------------------------------------------------
@Composable
fun HostelSetupWizard(
    hostelViewModel: HostelViewModel
) {
    var step by remember { mutableStateOf(1) }
    
    // Step 1 State
    var floorsCountStr by remember { mutableStateOf("") }
    
    // Config State
    var floorsConfig by remember { mutableStateOf(emptyList<FloorSetupConfig>()) }
    var roomPrefix by remember { mutableStateOf("Rm-") }

    val loading by hostelViewModel.loading.collectAsState()
    val error by hostelViewModel.error.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Error: $error\n\nDid you run the SQL script in Supabase?",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.Domain,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to Hostel Management",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Let's set up your hostel structure dynamically.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                // Stepper Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Step 1: Floors", fontWeight = if (step == 1) FontWeight.Bold else FontWeight.Normal, color = if (step == 1) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 12.sp)
                    Text(">", color = Color.Gray, fontSize = 12.sp)
                    Text("Step 2: Rooms", fontWeight = if (step == 2) FontWeight.Bold else FontWeight.Normal, color = if (step == 2) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 12.sp)
                    Text(">", color = Color.Gray, fontSize = 12.sp)
                    Text("Step 3: Beds", fontWeight = if (step == 3) FontWeight.Bold else FontWeight.Normal, color = if (step == 3) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (step == 1) {
                    Text("How many floors does your hostel have?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = floorsCountStr,
                        onValueChange = { floorsCountStr = it },
                        label = { Text("Number of Floors") },
                        placeholder = { Text("e.g., 3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            val count = floorsCountStr.toIntOrNull() ?: 1
                            floorsConfig = (1..count).map { FloorSetupConfig(floorNumber = it) }
                            step = 2
                        },
                        enabled = floorsCountStr.toIntOrNull() != null && floorsCountStr.toInt() > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next: Configure Rooms")
                    }
                } else if (step == 2) {
                    Text("How many rooms for each floor?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    floorsConfig.forEachIndexed { index, floor ->
                        OutlinedTextField(
                            value = if (floor.roomsCount == 0) "" else floor.roomsCount.toString(),
                            onValueChange = { 
                                val newConfig = floorsConfig.toMutableList()
                                newConfig[index] = floor.copy(roomsCount = it.toIntOrNull() ?: 0)
                                floorsConfig = newConfig
                            },
                            label = { Text("Rooms on Floor ${floor.floorNumber}") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = roomPrefix,
                        onValueChange = { roomPrefix = it },
                        label = { Text("Room Name Prefix") },
                        placeholder = { Text("e.g., Rm-") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(
                            onClick = { 
                                val newConfig = floorsConfig.toMutableList()
                                newConfig.forEach { floor ->
                                    val rCount = if(floor.roomsCount > 0) floor.roomsCount else 1
                                    floor.rooms.clear()
                                    for(i in 1..rCount){
                                        floor.rooms.add(RoomSetupConfig(roomIndex = i, bedsCount = 0))
                                    }
                                }
                                floorsConfig = newConfig
                                step = 3 
                            },
                            enabled = floorsConfig.all { it.roomsCount > 0 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next: Beds")
                        }
                    }
                } else if (step == 3) {
                    Text("What is the bed capacity for each room?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    floorsConfig.forEachIndexed { fIndex, floor ->
                        Text("Floor ${floor.floorNumber}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
                        floor.rooms.forEachIndexed { rIndex, room ->
                            OutlinedTextField(
                                value = if (room.bedsCount == 0) "" else room.bedsCount.toString(),
                                onValueChange = { 
                                    val newConfig = floorsConfig.toMutableList()
                                    val newRooms = floor.rooms.map { it.copy() }.toMutableList()
                                    newRooms[rIndex].bedsCount = it.toIntOrNull() ?: 0
                                    newConfig[fIndex] = floor.copy(rooms = newRooms)
                                    floorsConfig = newConfig
                                },
                                label = { Text("Beds in ${roomPrefix}${floor.floorNumber}0${room.roomIndex}") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                hostelViewModel.runInitialSetupWizard(floorsConfig, roomPrefix) {
                                    // Done!
                                }
                            },
                            enabled = floorsConfig.all { floor -> floor.rooms.all { room -> room.bedsCount > 0 } } && !loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Complete Setup")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HostelHierarchyEditor(
    hostelViewModel: HostelViewModel,
    hostelId: String
) {
    val mapData by hostelViewModel.hostelMapData.collectAsState()
    val loading by hostelViewModel.loading.collectAsState()
    
    LaunchedEffect(hostelId) {
        hostelViewModel.loadFullHostelMap(hostelId)
    }
    
    if (loading && mapData.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val error by hostelViewModel.error.collectAsState()
    if (error != null) {
        Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
    }
    
    if (mapData.isEmpty()) {
        Text("No structural data found.", color = Color.Gray, modifier = Modifier.padding(16.dp))
        return
    }
    
    val building = mapData.first().building
    val floors = mapData.first().floors
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Hostel Structure Editor", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🏢 ${building.name}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { 
                val nextFloorNum = (floors.maxOfOrNull { it.floor.floorNumber } ?: 0) + 1
                hostelViewModel.addFloor(building.id, nextFloorNum, hostelId) 
            }) {
                Text("+ Floor")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        floors.forEach { floorData ->
            var expanded by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Floor ${floorData.floor.floorNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row {
                            TextButton(onClick = { expanded = !expanded }) {
                                Text(if (expanded) "Hide" else "Select")
                            }
                            TextButton(onClick = { hostelViewModel.deleteFloor(floorData.floor.id, hostelId) }, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Delete")
                            }
                        }
                    }
                    
                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { 
                                val nextRoomIdx = (floorData.rooms.maxOfOrNull { 
                                    it.roomNumber.substringAfterLast("-").drop(1).toIntOrNull() ?: 0 
                                } ?: 0) + 1
                                val prefix = if (nextRoomIdx < 10) "0" else ""
                                hostelViewModel.addRoom(floorData.floor.id, "Rm-${floorData.floor.floorNumber}$prefix$nextRoomIdx", 1, hostelId) 
                            }) {
                                Text("+ Room", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        floorData.rooms.forEach { room ->
                            var roomExpanded by remember { mutableStateOf(false) }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 16.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${room.roomNumber} (${room.bedsCount} beds)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Row {
                                            TextButton(onClick = { roomExpanded = !roomExpanded }) {
                                                Text(if (roomExpanded) "Hide" else "Select", fontSize = 12.sp)
                                            }
                                            TextButton(onClick = { hostelViewModel.deleteRoom(room.id, hostelId) }, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                                Text("Delete", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    
                                    if (roomExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            OutlinedButton(onClick = { 
                                                val maxChar = room.beds.mapNotNull { it.bedNumber.lastOrNull() }.maxOrNull() ?: '@'
                                                val nextBed = if (maxChar in 'A'..'Y') "Bed ${maxChar + 1}" else "Bed ${room.beds.size + 1}"
                                                hostelViewModel.addBed(room.id, nextBed, hostelId) 
                                            }) {
                                                Text("+ Bed", fontSize = 12.sp)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        room.beds.forEach { bed ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(bed.bedNumber, fontSize = 13.sp)
                                                IconButton(onClick = { hostelViewModel.deleteBed(bed.id, hostelId) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// 2. STUDENT DASHBOARD
// -------------------------------------------------------------
@Composable
fun StudentDashboardScreen(
    authViewModel: AuthViewModel,
    hostelViewModel: HostelViewModel,
    financeViewModel: FinanceViewModel,
    complaintViewModel: ComplaintViewModel,
    announcementViewModel: AnnouncementViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val allocations by hostelViewModel.allocations.collectAsState()
    val invoices by financeViewModel.invoices.collectAsState()
    val complaints by complaintViewModel.complaints.collectAsState()
    val announcements by announcementViewModel.announcements.collectAsState()

    LaunchedEffect(profile) {
        profile?.id?.let { studentId ->
            hostelViewModel.loadAllocations()
            financeViewModel.loadInvoices(studentId = studentId)
            complaintViewModel.loadComplaints(studentId = studentId)
            announcementViewModel.loadAnnouncements()
        }
    }

    val activeAllocation = allocations.firstOrNull { it.studentId == profile?.id && it.status == "Active" }
    val pendingInvoice = invoices.firstOrNull { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
    val openComplaint = complaints.firstOrNull { it.status != "Resolved" && it.status != "Closed" }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Room Allocation details card
                Text("My Accommodation", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (activeAllocation != null) {
                            Text(
                                text = "Your Current Assignment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Visual 5-row list of Hostel, Block, Floor, Room, Bed
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🏢 My Hostel", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.hostelName ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🧱 My Block/Building", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.buildingName ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🪜 My Floor", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                val flText = if (activeAllocation.floorNumber != null) {
                                    if (activeAllocation.floorNumber == 0) "Ground Floor" else "Floor ${activeAllocation.floorNumber}"
                                } else {
                                    "Not Assigned"
                                }
                                Text(text = flText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🚪 My Room", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.roomNumber ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🛏 My Bed", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.bedNumber ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Allocated on: ${activeAllocation.allocatedAt.take(10)}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("No Room Allocated Yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                    Text("Please contact the warden for room/bed selection.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Finance & Complaints summaries
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Billing Dues
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .clickable { onNavigate("fees_list") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Mess Dues", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (pendingInvoice != null) "₹${pendingInvoice.balance}" else "No Dues",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingInvoice != null) Color(0xFFF44336) else Color(0xFF4CAF50)
                            )
                        }
                    }

                    // Complaints
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                            .clickable { onNavigate("complaints_list") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFF9800).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFF9800))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Complaints", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = openComplaint?.status ?: "All Clear",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (openComplaint != null) Color(0xFFFF9800) else Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Announcements section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Announcements", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onNavigate("announcements") }) {
                        Text("View All")
                    }
                }

                if (announcements.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No announcements today.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    announcements.take(2).forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val pillColor = if (item.priority == "High") Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                                    Text(
                                        text = item.priority,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor,
                                        modifier = Modifier
                                            .background(pillColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = item.message, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ACCOUNTANT DASHBOARD
// -------------------------------------------------------------
@Composable
fun AccountantDashboardScreen(
    authViewModel: AuthViewModel,
    financeViewModel: FinanceViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val stats by financeViewModel.financialStats.collectAsState()

    LaunchedEffect(Unit) {
        financeViewModel.loadFinancialStats()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Finance Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Total Collected",
                        value = "₹${stats["totalCollected"] ?: 0}",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Today's Payment",
                        value = "₹${stats["todayCollected"] ?: 0}",
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF3F51B5),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Pending Fees",
                        value = "₹${stats["totalPending"] ?: 0}",
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Overdue Amount",
                        value = "₹${stats["totalOverdue"] ?: 0}",
                        icon = Icons.Default.AssignmentLate,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ShortcutItem("Fee List", Icons.Default.ReceiptLong, Color(0xFF4CAF50)) { onNavigate("fees_list") }
                        ShortcutItem("Generate Bills", Icons.Default.PostAdd, Color(0xFF009688)) {
                            // Can trigger bottom sheet / dialog to input billing month
                            onNavigate("fees_list")
                        }
                        ShortcutItem("Reports", Icons.Default.Assessment, Color(0xFF9C27B0)) { onNavigate("reports") }
                        ShortcutItem("Students", Icons.Default.People, Color(0xFF3F51B5)) { onNavigate("student_list") }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 4. MAINTENANCE DASHBOARD
// -------------------------------------------------------------
@Composable
fun MaintenanceDashboardScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val assignedComplaints by complaintViewModel.assignedComplaints.collectAsState()
    val loading by complaintViewModel.loading.collectAsState()

    LaunchedEffect(profile) {
        profile?.id?.let { staffId ->
            complaintViewModel.loadAssignedComplaints(staffId)
        }
    }

    val highPriorityCount = assignedComplaints.count { it.priority == "High" || it.priority == "Emergency" }
    val pendingCount = assignedComplaints.count { it.status == "Assigned" || it.status == "In Progress" }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Task Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "My Total Tasks",
                        value = assignedComplaints.size.toString(),
                        icon = Icons.Default.Build,
                        color = Color(0xFF607D8B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Urgent Tasks",
                        value = highPriorityCount.toString(),
                        icon = Icons.Default.ReportProblem,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Assigned Complaints", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { profile?.id?.let { complaintViewModel.loadAssignedComplaints(it) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (assignedComplaints.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No complaints assigned to you!", fontWeight = FontWeight.Bold)
                                Text("Sit back and relax, or check in later.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(assignedComplaints) { complaint ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate("complaint_details/${complaint.id}") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val priorityColor = when (complaint.priority) {
                                                "Emergency" -> Color(0xFFD32F2F)
                                                "High" -> Color(0xFFF44336)
                                                "Medium" -> Color(0xFFFF9800)
                                                else -> Color(0xFF4CAF50)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(priorityColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = complaint.category, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = priorityColor)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = complaint.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = complaint.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    
                                    // Status Badge
                                    Text(
                                        text = complaint.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileBottomSheet(
    studentId: String,
    studentViewModel: StudentViewModel,
    financeViewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val student by studentViewModel.selectedStudent.collectAsState()
    val invoices by financeViewModel.invoices.collectAsState()
    val loading by studentViewModel.loading.collectAsState()
    val financeLoading by financeViewModel.loading.collectAsState()

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State for Custom Amount Dialog
    var showCustomAmountDialog by remember { mutableStateOf<com.hostel.management.domain.model.FeeInvoice?>(null) }
    var customAmountText by remember { mutableStateOf("") }
    
    // State for New Advance Payment
    var showNewPaymentDialog by remember { mutableStateOf(false) }
    var newPaymentMonth by remember { mutableStateOf("") }
    var newPaymentAmount by remember { mutableStateOf("") }
    var newPaymentMethod by remember { mutableStateOf("Cash") }
    var isMonthDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        studentViewModel.loadStudentDetails(studentId)
        // Invoices are already loaded globally, but we can filter them
    }

    val studentInvoices = invoices.filter { it.studentId == studentId }.sortedByDescending { it.billingMonth }
    val pendingInvoices = studentInvoices.filter { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
    val paymentHistory = studentInvoices.filter { it.paymentStatus == "Paid" || it.amountPaid > 0 }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (loading || student == null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(student?.profile?.fullName ?: "Unknown", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(student?.studentIdNumber ?: "", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Details
                Text("Details", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        com.hostel.management.presentation.student.DetailRow("Address", student?.address ?: "N/A")
                        com.hostel.management.presentation.student.DetailRow("Admission Date", student?.admissionDate ?: "N/A")
                        com.hostel.management.presentation.student.DetailRow("Course", "${student?.course ?: ""} - ${student?.department ?: ""}")
                        
                        val nextPayDate = pendingInvoices.minByOrNull { it.dueDate }?.dueDate ?: "No Pending Dues"
                        com.hostel.management.presentation.student.DetailRow("Next Pay Date", nextPayDate)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Header for Payments
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pending Dues", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { showNewPaymentDialog = true }) {
                        Text("Record New Payment")
                    }
                }

                if (pendingInvoices.isNotEmpty()) {
                    pendingInvoices.forEach { invoice ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Month: ${invoice.billingMonth}", fontWeight = FontWeight.Bold)
                                    Text("₹${invoice.balance}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Due: ${invoice.dueDate}", fontSize = 12.sp, color = Color.Gray)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Button(
                                        onClick = { 
                                            financeViewModel.recordPayment(studentId, invoice.id, invoice.balance, "Cash", null, null) {
                                                financeViewModel.loadInvoices()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        enabled = !financeLoading
                                    ) {
                                        Text("Pay Full", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedButton(
                                        onClick = { showCustomAmountDialog = invoice },
                                        modifier = Modifier.weight(1f),
                                        enabled = !financeLoading
                                    ) {
                                        Text("Custom", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            financeViewModel.recordPayment(studentId, invoice.id, invoice.balance, "Waived", null, "No Dues") {
                                                financeViewModel.loadInvoices()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !financeLoading,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Waive", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Payment History
                if (paymentHistory.isNotEmpty()) {
                    Text("Payment History", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    paymentHistory.forEach { invoice ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(invoice.billingMonth, fontWeight = FontWeight.Medium)
                                Text("Paid: ₹${invoice.amountPaid}", color = Color(0xFF4CAF50))
                            }
                            Text("Status: ${invoice.paymentStatus}", fontSize = 12.sp, color = Color.Gray)
                            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Custom Amount Dialog
    if (showCustomAmountDialog != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomAmountDialog = null },
            title = { Text("Custom Payment Amount") },
            text = {
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it },
                    label = { Text("Amount (Max: ₹${showCustomAmountDialog?.balance})") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customAmountText.toDoubleOrNull()
                        if (amount != null && amount > 0 && amount <= (showCustomAmountDialog?.balance ?: 0.0)) {
                            financeViewModel.recordPayment(studentId, showCustomAmountDialog!!.id, amount, "Cash", null, null) {
                                financeViewModel.loadInvoices()
                                showCustomAmountDialog = null
                                customAmountText = ""
                            }
                        }
                    },
                    enabled = !financeLoading
                ) {
                    Text("Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAmountDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Payment Dialog
    if (showNewPaymentDialog) {
        val monthsList = listOf("January 2026", "February 2026", "March 2026", "April 2026", "May 2026", "June 2026", "July 2026", "August 2026", "September 2026", "October 2026", "November 2026", "December 2026")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNewPaymentDialog = false },
            title = { Text("Record New Payment") },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newPaymentMonth,
                            onValueChange = { },
                            label = { Text("Select Month") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { isMonthDropdownExpanded = true },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = isMonthDropdownExpanded,
                            onDismissRequest = { isMonthDropdownExpanded = false }
                        ) {
                            monthsList.forEach { m ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = { newPaymentMonth = m; isMonthDropdownExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPaymentAmount,
                        onValueChange = { newPaymentAmount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newPaymentAmount.toDoubleOrNull()
                        if (amount != null && amount > 0 && newPaymentMonth.isNotBlank()) {
                            financeViewModel.recordPaymentForMonth(studentId, newPaymentMonth, amount, newPaymentMethod, null) {
                                financeViewModel.loadInvoices()
                                showNewPaymentDialog = false
                                newPaymentMonth = ""
                                newPaymentAmount = ""
                            }
                        }
                    },
                    enabled = !financeLoading
                ) {
                    Text("Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
