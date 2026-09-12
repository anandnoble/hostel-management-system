package com.hostel.management.presentation.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostel.management.domain.model.*
import com.hostel.management.presentation.auth.AuthViewModel
import com.hostel.management.presentation.hostel.HostelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    announcementViewModel: AnnouncementViewModel,
    authViewModel: AuthViewModel,
    hostelViewModel: HostelViewModel,
    onNavigateBack: () -> Unit
) {
    val announcements by announcementViewModel.announcements.collectAsState()
    val loading by announcementViewModel.loading.collectAsState()
    val userProfile by authViewModel.currentProfile.collectAsState()

    var showCreateAnnouncementDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        announcementViewModel.loadAnnouncements()
        hostelViewModel.loadHostels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcements") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (userProfile?.role == UserRole.SUPER_ADMIN || userProfile?.role == UserRole.HOSTEL_ADMIN) {
                FloatingActionButton(
                    onClick = { showCreateAnnouncementDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Announcement")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (loading && announcements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (announcements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No announcements posted.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(announcements) { announce ->
                        val priorityColor = when (announce.priority) {
                            "High" -> Color(0xFFF44336)
                            "Low" -> Color(0xFF9E9E9E)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = announce.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = announce.priority,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = priorityColor,
                                        modifier = Modifier
                                            .background(priorityColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = announce.message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Audience: ${announce.targetAudience}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Posted: ${announce.publishDate.take(10)}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create Announcement Dialog
        if (showCreateAnnouncementDialog) {
            var title by remember { mutableStateOf("") }
            var message by remember { mutableStateOf("") }
            var targetAudience by remember { mutableStateOf("ALL") }
            var priority by remember { mutableStateOf("Normal") }
            var selectedHostelId by remember { mutableStateOf<String?>(null) }

            val hostels by hostelViewModel.hostels.collectAsState()

            AlertDialog(
                onDismissRequest = { showCreateAnnouncementDialog = false },
                title = { Text("Publish Announcement") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && message.isNotBlank()) {
                                announcementViewModel.createAnnouncement(
                                    title = title,
                                    message = message,
                                    targetAudience = targetAudience,
                                    targetHostelId = selectedHostelId,
                                    targetBuildingId = null,
                                    priority = priority
                                ) {
                                    announcementViewModel.loadAnnouncements()
                                    showCreateAnnouncementDialog = false
                                }
                            }
                        },
                        enabled = title.isNotBlank() && message.isNotBlank()
                    ) {
                        Text("Publish")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateAnnouncementDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message Body") },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        // Audience select
                        var audienceExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = audienceExpanded,
                            onExpandedChange = { audienceExpanded = !audienceExpanded },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = targetAudience,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Audience") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = audienceExpanded,
                                onDismissRequest = { audienceExpanded = false }
                            ) {
                                val audiences = listOf("ALL", "HOSTEL")
                                audiences.forEach { aud ->
                                    DropdownMenuItem(
                                        text = { Text(aud) },
                                        onClick = {
                                            targetAudience = aud
                                            audienceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (targetAudience == "HOSTEL") {
                            // Hostel select dropdown
                            var hostelExpanded by remember { mutableStateOf(false) }
                            val currentHostel = hostels.firstOrNull { it.id == selectedHostelId }
                            ExposedDropdownMenuBox(
                                expanded = hostelExpanded,
                                onExpandedChange = { hostelExpanded = !hostelExpanded },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentHostel?.name ?: "Select Hostel",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Hostel") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hostelExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = hostelExpanded,
                                    onDismissRequest = { hostelExpanded = false }
                                ) {
                                    hostels.forEach { h ->
                                        DropdownMenuItem(
                                            text = { Text(h.name) },
                                            onClick = {
                                                selectedHostelId = h.id
                                                hostelExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Priority select
                        var priorityExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = priorityExpanded,
                            onExpandedChange = { priorityExpanded = !priorityExpanded }
                        ) {
                            OutlinedTextField(
                                value = priority,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Priority") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = priorityExpanded,
                                onDismissRequest = { priorityExpanded = false }
                            ) {
                                val priorities = listOf("Low", "Normal", "High")
                                priorities.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            priority = p
                                            priorityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    announcementViewModel: AnnouncementViewModel,
    studentViewModel: com.hostel.management.presentation.student.StudentViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val notifications by announcementViewModel.notifications.collectAsState()
    val loading by announcementViewModel.loading.collectAsState()
    val pendingRegistrations by (studentViewModel?.pendingRegistrations?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val monthlyPaymentSubmissions by (studentViewModel?.monthlyPaymentSubmissions?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    var selectedTab by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        announcementViewModel.loadNotifications()
        studentViewModel?.loadPendingRegistrations()
        studentViewModel?.loadMonthlyPaymentSubmissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications & Approvals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val tabs = listOf("Registrations", "Monthly Fees", "System")
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Pending Registrations Tab
                    if (loading && pendingRegistrations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (pendingRegistrations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending registrations.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(pendingRegistrations) { reg ->
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = reg.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "Phone: ${reg.phone}", fontSize = 14.sp)
                                        Text(text = "Email: ${reg.email}", fontSize = 14.sp)
                                        Text(text = "Aadhaar: ${reg.aadhaarNumber ?: "N/A"}", fontSize = 14.sp)
                                        Text(text = "Requested Bed: ${reg.bedNumber ?: "Unassigned"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { studentViewModel?.updateSelfRegistrationStatus(reg.id, "Approved") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                            ) {
                                                Text("Approve")
                                            }
                                            OutlinedButton(
                                                onClick = { studentViewModel?.updateSelfRegistrationStatus(reg.id, "Rejected") }
                                            ) {
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Monthly Fees Tab
                    if (loading && monthlyPaymentSubmissions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (monthlyPaymentSubmissions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending monthly fees.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(monthlyPaymentSubmissions) { payment ->
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = payment.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "Room: ${payment.roomNumber} - Bed: ${payment.bedNumber}", fontSize = 14.sp)
                                        Text(text = "Month: ${payment.billingMonth}", fontSize = 14.sp)
                                        Text(text = "Amount: ₹${payment.amount}", fontSize = 14.sp)
                                        Text(text = "UTR: ${payment.utrNumber}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (payment.status == "Pending") {
                                            Button(
                                                onClick = { studentViewModel?.verifyMonthlyPayment(payment.id, "Verified") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                            ) {
                                                Text("Verify Payment")
                                            }
                                        } else {
                                            Text(text = "Status: ${payment.status}", color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // System Notifications Tab
                    if (loading && notifications.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (notifications.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No system notifications.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(notifications) { notif ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { announcementViewModel.markAsRead(notif.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface 
                                                         else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (!notif.isRead) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = notif.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = notif.createdAt.take(16).replace("T", " "), fontSize = 9.sp, color = Color.Gray)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    announcementViewModel: AnnouncementViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by announcementViewModel.auditLogs.collectAsState()
    val loading by announcementViewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        announcementViewModel.loadAuditLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Trail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (loading && logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audit logs found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        val actionColor = when (log.action) {
                            "INSERT" -> Color(0xFF4CAF50)
                            "UPDATE" -> Color(0xFF2196F3)
                            "DELETE" -> Color(0xFFF44336)
                            else -> Color(0xFF607D8B)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.action} : ${log.entityName}",
                                        fontWeight = FontWeight.Bold,
                                        color = actionColor,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = log.createdAt.take(16).replace("T", " "),
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Performed by: ${log.userName ?: "System"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!log.metadata.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.metadata,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 3
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
